package io.github.mudrichenkoevgeny.backend.feature.securityapi.api.usecase.management.settings

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.contract.SecurityWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.feature.securityapi.domain.audit.action.SecurityAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.securityapi.domain.audit.resource.SecurityAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateSecuritySettingsUseCaseTest {

    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)

    private val useCase = UpdateSecuritySettingsUseCase(
        securitySettingsProvider,
        auditLogger,
        auditErrorConverter,
        webSocketManager
    )

    private fun sampleSettings() = SecuritySettings(
        recentAuthenticationValiditySeconds = 300,
        recentAuthenticationValiditySecondsForManagement = 60,
        passwordPolicy = PasswordPolicy(
            minLength = 10,
            requireLetter = true,
            requireUpperCase = true,
            requireLowerCase = true,
            requireDigit = true,
            requireSpecialChar = true,
            commonPasswords = emptySet()
        ),
        otpConfirmation = OtpConfirmation(
            retryAfterSeconds = 60,
            numberOfSymbols = 6,
            expirationSeconds = 300
        ),
        mfaTokenExpirationSeconds = 600
    )

    private fun authContext(userId: UserId) = AuthenticatedRequestContext(
        traceId = null,
        userId = userId,
        userRole = UserRole.ADMIN,
        sessionId = UserSessionId.generate(),
        clientInfo = ClientInfo(
            deviceInfo = ClientDeviceInfo(null, null, null, null, null, null),
            userAgent = "test-agent",
            ipAddress = "127.0.0.1",
            host = null,
            origin = null,
            apiVersion = null
        )
    )

    @Test
    fun `successfully updates settings, logs audit and broadcasts via websocket`() = runTest {
        val settings = sampleSettings()
        val userId = UserId.generate()
        val context = authContext(userId)

        coEvery { securitySettingsProvider.updateSecuritySettings(settings) } returns AppResult.Success(Unit)

        val result = useCase(settings, context)

        assertEquals(AppResult.Success(Unit), result)

        coVerify(exactly = 1) {
            securitySettingsProvider.updateSecuritySettings(settings)
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
                action = SecurityAuditActionType.MANAGEMENT_UPDATE_SECURITY_SETTINGS,
                resource = SecurityAuditResourceType.SECURITY_SETTINGS,
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
            webSocketManager.sendMessageToAll(match {
                it.type == SecurityWebSocketEventTypes.SECURITY_SETTINGS_UPDATED
            })
        }
    }

    @Test
    fun `handles error, logs failure audit and does not broadcast`() = runTest {
        val settings = sampleSettings()
        val userId = UserId.generate()
        val context = authContext(userId)
        val exception = RuntimeException("Update failed")
        val error = CommonError.Internal(exception)
        val errorLogData = AuditErrorLogData(AuditStatus.FAILED, emptySet())

        coEvery { securitySettingsProvider.updateSecuritySettings(settings) } returns AppResult.Error(error)
        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(settings, context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
                action = SecurityAuditActionType.MANAGEMENT_UPDATE_SECURITY_SETTINGS,
                resource = SecurityAuditResourceType.SECURITY_SETTINGS,
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
        coVerify(exactly = 0) {
            webSocketManager.sendMessageToAll(any())
        }
    }
}