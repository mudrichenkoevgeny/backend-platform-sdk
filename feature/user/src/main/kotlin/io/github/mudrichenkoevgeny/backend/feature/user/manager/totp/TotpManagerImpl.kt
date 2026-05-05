package io.github.mudrichenkoevgeny.backend.feature.user.manager.totp

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.combine
import io.github.mudrichenkoevgeny.backend.core.common.result.flatMapSuccess
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.core.common.result.mapToResult
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.backend.core.security.aescryptor.AesCryptor
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.totpcryptoprocessor.TotpCryptoProcessor
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usertotpsettings.UserTotpSettingsRepository
import io.github.mudrichenkoevgeny.backend.feature.user.model.totp.UserTotpSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.DecryptedString
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant

/**
 * Default [TotpManager] implementation coordinating TOTP lifecycle and security.
 *
 * Utilizes [TotpCryptoProcessor] for time-based code verification and [AesCryptor] for
 * securing recovery codes. Orchestrates state changes between [UserTotpSettingsRepository]
 * and [UserRepository] within [dbQuery] blocks to ensure atomic activation and deactivation.
 */
@Singleton
class TotpManagerImpl @Inject constructor(
    private val userTotpSettingsRepository: UserTotpSettingsRepository,
    private val userRepository: UserRepository,
    private val aesCryptor: AesCryptor,
    private val totpCryptoProcessor: TotpCryptoProcessor
) : TotpManager {

    override suspend fun initiateTotpSetup(
        userId: UserId,
        encryptedSecret: EncryptedString
    ): AppResult<UserTotpSettings> = dbQuery {
        userTotpSettingsRepository.upsertUnconfirmedSettings(userId, encryptedSecret)
    }

    override suspend fun confirmTotp(
        userId: UserId,
        decryptedRecoveryCodes: List<DecryptedString>
    ): AppResult<Unit> = dbQuery {
        decryptedRecoveryCodes
            .mapToResult { aesCryptor.encrypt(it) }
            .flatMapSuccess { encryptedCodes ->
                userTotpSettingsRepository.confirmTotp(
                    userId = userId,
                    encryptedRecoveryCodes = encryptedCodes
                )
            }
            .flatMapSuccess {
                userRepository.updateUser(
                    userId = userId,
                    isTotpEnabled = UpdateField.Set(true)
                )
            }
            .mapSuccess { }
    }

    override suspend fun disableTotp(userId: UserId): AppResult<Unit> = dbQuery {
        val deleteResult = userTotpSettingsRepository.deleteSettings(userId)
        if (deleteResult is AppResult.Error) {
            return@dbQuery deleteResult
        }

        val userUpdateResult = userRepository.updateUser(
            userId = userId,
            isTotpEnabled = UpdateField.Set(false)
        )

        when (userUpdateResult) {
            is AppResult.Error -> AppResult.Error(userUpdateResult.error)
            is AppResult.Success -> AppResult.Success(Unit)
        }
    }

    override suspend fun markAsUsed(
        userId: UserId,
        timestamp: Instant
    ): AppResult<Unit> = dbQuery {
        userTotpSettingsRepository.updateSettings(
            userId = userId,
            lastUsedAt = UpdateField.Set(timestamp)
        ).mapSuccess { }
    }

    override suspend fun verifyTotp(userId: UserId, code: String): AppResult<Unit> = dbQuery {
        userTotpSettingsRepository.getSettingsByUserId(userId)
            .mapNotNullOrError(SecurityError.TotpNotEnabled())
            .flatMapSuccess { settings ->
                if (settings.isConfirmed) {
                    AppResult.Success(settings)
                } else {
                    AppResult.Error(SecurityError.TotpNotEnabled())
                }
            }
            .flatMapSuccess { totpSettings ->
                totpCryptoProcessor.isCodeValid(
                    code = code,
                    encryptedSecret = totpSettings.encryptedSecret
                )
            }
            .flatMapSuccess { isValid ->
                if (isValid) {
                    AppResult.Success(Unit)
                } else {
                    AppResult.Error(SecurityError.InvalidTotpCode())
                }
            }
    }

    override suspend fun verifyTotpRecoveryCode(userId: UserId, code: String): AppResult<Unit> = dbQuery {
        userTotpSettingsRepository.getSettingsByUserId(userId)
            .mapNotNullOrError(SecurityError.TotpNotEnabled())
            .flatMapSuccess { settings ->
                if (settings.isConfirmed) {
                    AppResult.Success(settings)
                } else {
                    AppResult.Error(SecurityError.TotpNotEnabled())
                }
            }
            .flatMapSuccess { settings ->
                val recoveryCodes = settings.encryptedRecoveryCodes
                    ?: return@flatMapSuccess AppResult.Error(SecurityError.InvalidTotpCode())

                var matchedEncryptedCode: EncryptedString? = null

                for (encryptedCode in recoveryCodes) {
                    val decryptResult = aesCryptor.decrypt(encryptedCode)
                    if (decryptResult is AppResult.Success && decryptResult.data.value == code) {
                        matchedEncryptedCode = encryptedCode
                        break
                    }
                }

                if (matchedEncryptedCode != null) {
                    val updatedCodes = recoveryCodes - matchedEncryptedCode
                    userTotpSettingsRepository.updateSettings(
                        userId = userId,
                        encryptedRecoveryCodes = UpdateField.Set(updatedCodes)
                    ).mapSuccess { Unit }
                } else {
                    AppResult.Error(SecurityError.InvalidTotpCode())
                }
            }
    }

    override suspend fun getSettings(
        userId: UserId
    ): AppResult<UserTotpSettings?> = dbQuery {
        userTotpSettingsRepository.getSettingsByUserId(userId)
    }

    override suspend fun getDecryptedRecoveryCodes(userId: UserId): AppResult<List<DecryptedString>> = dbQuery {
        userTotpSettingsRepository.getSettingsByUserId(userId)
            .mapNotNullOrError(SecurityError.TotpNotEnabled())
            .flatMapSuccess { settings ->
                if (!settings.isConfirmed || settings.encryptedRecoveryCodes == null) {
                    return@flatMapSuccess AppResult.Error(SecurityError.TotpNotEnabled())
                }

                settings.encryptedRecoveryCodes
                    .map { encryptedRecoveryCode ->
                        aesCryptor.decrypt(encryptedRecoveryCode)
                    }
                    .combine()
            }
    }

    override suspend fun updateRecoveryCodes(
        userId: UserId,
        decryptedRecoveryCodes: List<DecryptedString>
    ): AppResult<List<DecryptedString>> = dbQuery {
        decryptedRecoveryCodes
            .mapToResult { decryptedRecoveryCode ->
                aesCryptor.encrypt(decryptedRecoveryCode)
            }
            .flatMapSuccess { encryptedCodes ->
                userTotpSettingsRepository.updateSettings(
                    userId = userId,
                    encryptedRecoveryCodes = UpdateField.Set(encryptedCodes)
                )
            }
            .flatMapSuccess { settings ->
                settings.encryptedRecoveryCodes?.mapToResult { encryptedCode ->
                    aesCryptor.decrypt(encryptedCode)
                } ?: AppResult.Success(emptyList())
            }
    }
}