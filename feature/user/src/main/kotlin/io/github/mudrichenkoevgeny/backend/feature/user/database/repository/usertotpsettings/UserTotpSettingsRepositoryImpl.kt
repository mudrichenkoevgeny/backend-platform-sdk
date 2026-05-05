package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usertotpsettings

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.model.onSet
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.dataOrNull
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.common.util.toJavaInstant
import io.github.mudrichenkoevgeny.backend.core.common.util.toKotlinInstant
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserTotpSettingsTable
import io.github.mudrichenkoevgeny.backend.feature.user.model.totp.UserTotpSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant as JavaInstant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant as KotlinInstant
import kotlin.uuid.Uuid

/**
 * Default [UserTotpSettingsRepository] implementation backed by Exposed and [UserTotpSettingsTable].
 *
 * Handles persistent storage of encrypted TOTP secrets and recovery codes. Returns [CommonError.Database]
 * when database constraints are violated or expected row updates fail.
 */
@Singleton
class UserTotpSettingsRepositoryImpl @Inject constructor() : UserTotpSettingsRepository {

    override suspend fun upsertUnconfirmedSettings(
        userId: UserId,
        encryptedSecret: EncryptedString
    ): AppResult<UserTotpSettings> {
        val existing = getSettingsByUserId(userId).dataOrNull()

        if (existing == null) {
            UserTotpSettingsTable.insert { row ->
                row[id] = Uuid.random()
                row[UserTotpSettingsTable.userId] = userId.value
                row[UserTotpSettingsTable.encryptedSecret] = encryptedSecret.value
                row[isConfirmed] = false
                row[encryptedRecoveryCodes] = null
                row[lastUsedAt] = null
            }
        } else {
            UserTotpSettingsTable.update({ UserTotpSettingsTable.userId eq userId.value }) { row ->
                row[UserTotpSettingsTable.encryptedSecret] = encryptedSecret.value
                row[isConfirmed] = false
                row[encryptedRecoveryCodes] = null
                row[lastUsedAt] = null
                row[updatedAt] = JavaInstant.now()
            }
        }

        return getSettingsByUserId(userId).mapNotNullOrError(
            CommonError.Database("Upsert failed for user id=${userId.value}")
        )
    }

    override suspend fun confirmTotp(
        userId: UserId,
        encryptedRecoveryCodes: List<EncryptedString>
    ): AppResult<UserTotpSettings> {
        val updatedRows = UserTotpSettingsTable.update(
            { UserTotpSettingsTable.userId eq userId.value }
        ) { updateStatement ->
            updateStatement[isConfirmed] = true
            updateStatement[UserTotpSettingsTable.encryptedRecoveryCodes] = encryptedRecoveryCodes.map { it.value }
            updateStatement[updatedAt] = JavaInstant.now()
        }

        if (updatedRows == 0) {
            return AppResult.Error(
                CommonError.Database("Failed to confirm TOTP for user id=${userId.value}")
            )
        }

        return getSettingsByUserId(userId).mapNotNullOrError(
            CommonError.Database("Failed to retrieve settings after confirmation for user id=${userId.value}")
        )
    }

    override suspend fun updateSettings(
        userId: UserId,
        encryptedSecret: UpdateField<EncryptedString>,
        isConfirmed: UpdateField<Boolean>,
        encryptedRecoveryCodes: UpdateField<List<EncryptedString>?>,
        lastUsedAt: UpdateField<KotlinInstant?>
    ): AppResult<UserTotpSettings> {
        val secretToSet = if (encryptedSecret is UpdateField.Set) {
            encryptedSecret.value
                ?: return AppResult.Error(CommonError.Database("Secret cannot be null"))
        } else null

        val confirmationToSet = if (isConfirmed is UpdateField.Set) {
            isConfirmed.value
                ?: return AppResult.Error(CommonError.Database("isConfirmed cannot be null"))
        } else null

        val updatedRows = UserTotpSettingsTable.update(
            { UserTotpSettingsTable.userId eq userId.value }
        ) { updateStatement ->
            secretToSet?.let { encryptedSecret ->
                updateStatement[UserTotpSettingsTable.encryptedSecret] = encryptedSecret.value
            }
            confirmationToSet?.let { isConfirmed ->
                updateStatement[UserTotpSettingsTable.isConfirmed] = isConfirmed
            }

            encryptedRecoveryCodes.onSet { value ->
                updateStatement[UserTotpSettingsTable.encryptedRecoveryCodes] = value?.map { hashedRecoveryCode ->
                    hashedRecoveryCode.value
                }
            }

            lastUsedAt.onSet { lastUsedAt ->
                updateStatement[UserTotpSettingsTable.lastUsedAt] = lastUsedAt?.toJavaInstant()
            }

            updateStatement[updatedAt] = JavaInstant.now()
        }

        if (updatedRows == 0) {
            return AppResult.Error(
                CommonError.Database("Failed to update TOTP settings for user id=${userId.value}")
            )
        }

        return getSettingsByUserId(userId).mapNotNullOrError(
            CommonError.Database("Failed to retrieve settings after update for user id=${userId.value}")
        )
    }

    override suspend fun getSettingsByUserId(userId: UserId): AppResult<UserTotpSettings?> {
        val row = UserTotpSettingsTable
            .selectAll()
            .where { UserTotpSettingsTable.userId eq userId.value }
            .singleOrNull()

        return AppResult.Success(row?.toUserTotpSettings())
    }

    override suspend fun deleteSettings(userId: UserId): AppResult<Unit> {
        UserTotpSettingsTable.deleteWhere { UserTotpSettingsTable.userId eq userId.value }
        return AppResult.Success(Unit)
    }

    private fun ResultRow.toUserTotpSettings(): UserTotpSettings {
        return UserTotpSettings(
            userId = UserId(this[UserTotpSettingsTable.userId].value),
            encryptedSecret = EncryptedString(this[UserTotpSettingsTable.encryptedSecret]),
            isConfirmed = this[UserTotpSettingsTable.isConfirmed],
            encryptedRecoveryCodes = this[UserTotpSettingsTable.encryptedRecoveryCodes]?.map { encryptedString ->
                EncryptedString(encryptedString)
            },
            lastUsedAt = this[UserTotpSettingsTable.lastUsedAt]?.toKotlinInstant()
        )
    }
}