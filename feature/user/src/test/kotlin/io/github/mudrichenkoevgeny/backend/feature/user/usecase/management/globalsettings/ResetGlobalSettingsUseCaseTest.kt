package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.globalsettings

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.backend.core.settings.domain.model.globalsettings.createTestManagementGlobalSettings
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.session.createTestUserSessionInternal
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.createTestUserDetails
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.OpenGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.contract.SettingsWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.audit.action.SettingsAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.audit.resource.SettingsAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResetGlobalSettingsUseCaseTest {

    private val globalSettingsProvider = mockk<GlobalSettingsProvider>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()

    private val useCase = ResetGlobalSettingsUseCase(
        globalSettingsProvider,
        auditLogger,
        auditErrorConverter,
        webSocketManager,
        userManager,
        sessionManager,
        authenticationChallengeService
    )

    private fun mockMfaCheckSuccess(userId: UserId) {
        val userDetails = createTestUserDetails(id = userId)
        val userSession = createTestUserSessionInternal(userId = userId)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(any()) } returns AppResult.Success(userSession)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(userDetails, userSession, true) } returns AppResult.Success(Unit)
    }

    private fun sampleSettings() = createTestManagementGlobalSettings(
        privacyPolicyUrl = "https://example.com/privacy",
        termsOfServiceUrl = "https://example.com/terms",
        contactSupportEmail = "support@example.com"
    )

    @Test
    fun `successfully resets settings, logs audit and broadcasts via websocket`() = runTest {
        val settings = sampleSettings()
        val userId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = userId)
        val openGlobalSettings = mockk<OpenGlobalSettings>(relaxed = true)

        mockMfaCheckSuccess(userId)

        coEvery { globalSettingsProvider.resetManagementGlobalSettings() } returns AppResult.Success(settings)
        every { globalSettingsProvider.getOpenGlobalSettings() } returns openGlobalSettings

        val result = useCase(context)

        assertEquals(AppResult.Success(settings), result)

        coVerify(exactly = 1) {
            globalSettingsProvider.resetManagementGlobalSettings()
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
                action = SettingsAuditActionType.MANAGEMENT_RESET_GLOBAL_SETTINGS,
                resource = SettingsAuditResourceType.GLOBAL_SETTINGS,
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
            webSocketManager.sendMessageToScope(
                scope = ApiScope.OPEN,
                frame = match {
                    it.type == SettingsWebSocketEventTypes.OPEN_GLOBAL_SETTINGS_UPDATED
                }
            )
            webSocketManager.sendMessageToScope(
                scope = ApiScope.MANAGEMENT,
                frame = match {
                    it.type == SettingsWebSocketEventTypes.MANAGEMENT_GLOBAL_SETTINGS_UPDATED
                }
            )
        }
    }

    @Test
    fun `handles error, logs failure audit and does not broadcast`() = runTest {
        val userId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = userId)
        val error = CommonError.Internal(RuntimeException("Database error"))
        val errorLogData = AuditErrorLogData(AuditStatus.FAILED, emptySet())

        mockMfaCheckSuccess(userId)

        coEvery { globalSettingsProvider.resetManagementGlobalSettings() } returns AppResult.Error(error)
        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
                action = SettingsAuditActionType.MANAGEMENT_RESET_GLOBAL_SETTINGS,
                resource = SettingsAuditResourceType.GLOBAL_SETTINGS,
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
        coVerify(exactly = 0) {
            globalSettingsProvider.getOpenGlobalSettings()
            webSocketManager.sendMessageToScope(any(), any())
        }
    }
}
