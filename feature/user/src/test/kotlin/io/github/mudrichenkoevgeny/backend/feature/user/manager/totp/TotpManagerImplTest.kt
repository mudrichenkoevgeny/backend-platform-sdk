package io.github.mudrichenkoevgeny.backend.feature.user.manager.totp

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.aescryptor.AesCryptor
import io.github.mudrichenkoevgeny.backend.core.security.totpcryptoprocessor.TotpCryptoProcessor
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usertotpsettings.UserTotpSettingsRepository
import io.github.mudrichenkoevgeny.backend.feature.user.model.totp.UserTotpSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.DecryptedString
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class TotpManagerImplTest {

    private val userTotpSettingsRepository = mockk<UserTotpSettingsRepository>()
    private val userRepository = mockk<UserRepository>()
    private val aesCryptor = mockk<AesCryptor>()
    private val totpCryptoProcessor = mockk<TotpCryptoProcessor>()

    private val manager = TotpManagerImpl(
        userTotpSettingsRepository,
        userRepository,
        aesCryptor,
        totpCryptoProcessor
    )

    private val userId = UserId.generate()
    private val encryptedSecret = EncryptedString("encrypted-secret")

    @Test
    fun `initiateTotpSetup returns settings`() = runTest {
        val settings = createSampleSettings(userId, isConfirmed = false)
        coEvery {
            userTotpSettingsRepository.upsertUnconfirmedSettings(userId, encryptedSecret)
        } returns AppResult.Success(settings)

        val result = manager.initiateTotpSetup(userId, encryptedSecret)

        assertTrue(result is AppResult.Success)
        assertEquals(settings, (result as AppResult.Success).data)
    }

    @Test
    fun `confirmTotp success flow`() = runTest {
        val decryptedCodes = listOf(DecryptedString("code1"))
        val encryptedCodes = listOf(EncryptedString("enc1"))
        val settings = createSampleSettings(userId, isConfirmed = true)
        val userDetails = createSampleUserDetails(userId)

        coEvery { aesCryptor.encrypt(any()) } returns AppResult.Success(encryptedCodes.first())
        coEvery {
            userTotpSettingsRepository.confirmTotp(userId, any())
        } returns AppResult.Success(settings)
        coEvery {
            userRepository.updateUser(userId, isTotpEnabled = UpdateField.Set(true))
        } returns AppResult.Success(userDetails)

        val result = manager.confirmTotp(userId, decryptedCodes)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `disableTotp success flow`() = runTest {
        val userDetails = createSampleUserDetails(userId)

        coEvery { userTotpSettingsRepository.deleteSettings(userId) } returns AppResult.Success(Unit)
        coEvery {
            userRepository.updateUser(userId, isTotpEnabled = UpdateField.Set(false))
        } returns AppResult.Success(userDetails)

        val result = manager.disableTotp(userId)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `verifyTotp code valid`() = runTest {
        val settings = createSampleSettings(userId, isConfirmed = true)
        coEvery { userTotpSettingsRepository.getSettingsByUserId(userId) } returns AppResult.Success(settings)
        coEvery {
            totpCryptoProcessor.isCodeValid("123456", settings.encryptedSecret)
        } returns AppResult.Success(true)

        val result = manager.verifyTotp(userId, "123456")

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `verifyTotpRecoveryCode valid code deletes it`() = runTest {
        val recoveryCode = "REC-1"
        val encCode = EncryptedString("enc-rec-1")
        val settings = createSampleSettings(userId, isConfirmed = true).copy(
            encryptedRecoveryCodes = listOf(encCode)
        )

        coEvery { userTotpSettingsRepository.getSettingsByUserId(userId) } returns AppResult.Success(settings)
        coEvery { aesCryptor.decrypt(encCode) } returns AppResult.Success(DecryptedString(recoveryCode))
        coEvery {
            userTotpSettingsRepository.updateSettings(userId, encryptedRecoveryCodes = any())
        } returns AppResult.Success(settings.copy(encryptedRecoveryCodes = emptyList()))

        val result = manager.verifyTotpRecoveryCode(userId, recoveryCode)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `getDecryptedRecoveryCodes success`() = runTest {
        val encCodes = listOf(EncryptedString("e1"))
        val decCodes = listOf(DecryptedString("d1"))
        val settings = createSampleSettings(userId, isConfirmed = true).copy(
            encryptedRecoveryCodes = encCodes
        )

        coEvery { userTotpSettingsRepository.getSettingsByUserId(userId) } returns AppResult.Success(settings)
        coEvery { aesCryptor.decrypt(encCodes[0]) } returns AppResult.Success(decCodes[0])

        val result = manager.getDecryptedRecoveryCodes(userId)

        assertTrue(result is AppResult.Success)
        assertEquals(decCodes, (result as AppResult.Success).data)
    }

    @Test
    fun `updateRecoveryCodes flow`() = runTest {
        val newDec = listOf(DecryptedString("n1"))
        val newEnc = listOf(EncryptedString("ne1"))
        val settings = createSampleSettings(userId, isConfirmed = true).copy(
            encryptedRecoveryCodes = newEnc
        )

        coEvery { aesCryptor.encrypt(any()) } returns AppResult.Success(newEnc.first())
        coEvery {
            userTotpSettingsRepository.updateSettings(userId, encryptedRecoveryCodes = any())
        } returns AppResult.Success(settings)
        coEvery { aesCryptor.decrypt(newEnc.first()) } returns AppResult.Success(newDec.first())

        val result = manager.updateRecoveryCodes(userId, newDec)

        assertTrue(result is AppResult.Success)
        assertEquals(newDec, (result as AppResult.Success).data)
    }

    @Test
    fun `markAsUsed success`() = runTest {
        val now = Clock.System.now()
        val settings = createSampleSettings(userId, isConfirmed = true)

        coEvery {
            userTotpSettingsRepository.updateSettings(userId, lastUsedAt = any())
        } returns AppResult.Success(settings)

        val result = manager.markAsUsed(userId, now)

        assertTrue(result is AppResult.Success)
    }

    private fun createSampleSettings(uId: UserId, isConfirmed: Boolean) = UserTotpSettings(
        userId = uId,
        encryptedSecret = EncryptedString("secret"),
        isConfirmed = isConfirmed,
        encryptedRecoveryCodes = null,
        lastUsedAt = null
    )

    private fun createSampleUserDetails(uId: UserId) = UserDetails(
        id = uId,
        role = UserRole.USER,
        accountStatus = UserAccountStatus.ACTIVE,
        accountStatusBeforeDeletion = null,
        authorityLevel = 1,
        permissionCodes = emptySet(),
        isTotpEnabled = true,
        createdAt = Clock.System.now()
    )
}