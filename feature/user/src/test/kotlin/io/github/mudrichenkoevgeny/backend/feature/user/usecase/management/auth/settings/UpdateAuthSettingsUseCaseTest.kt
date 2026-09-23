package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auth.settings

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.auth.settings.createTestManagementAuthSettings
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.session.createTestUserSessionInternal
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.createTestUserDetails
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.OpenAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateAuthSettingsUseCaseTest {

    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()

    private val useCase = UpdateAuthSettingsUseCase(
        authSettingsProvider = authSettingsProvider,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        webSocketManager = webSocketManager,
        userManager = userManager,
        sessionManager = sessionManager,
        authenticationChallengeService = authenticationChallengeService
    )

    private fun mockMfaCheckSuccess(userId: UserId) {
        val userDetails = createTestUserDetails(id = userId)
        val userSession = createTestUserSessionInternal(userId = userId)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(any()) } returns AppResult.Success(userSession)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(userDetails, userSession, true) } returns AppResult.Success(Unit)
    }

    @Test
    fun `successfully updates settings, logs audit and broadcasts via websocket`() = runTest {
        val settings = createTestManagementAuthSettings()
        val userId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = userId)
        val openAuthSettings = mockk<OpenAuthSettings>(relaxed = true)

        mockMfaCheckSuccess(userId)

        coEvery {
            authSettingsProvider.updateManagementAuthSettings(settings)
        } returns AppResult.Success(Unit)

        every { authSettingsProvider.getOpenAuthSettings() } returns openAuthSettings
        every { authSettingsProvider.getManagementAuthSettings() } returns settings

        val result = useCase(settings, context)

        assertEquals(AppResult.Success(Unit), result)

        coVerify(exactly = 1) {
            authSettingsProvider.updateManagementAuthSettings(settings)
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
                action = UserAuditActionType.MANAGEMENT_UPDATE_AUTH_SETTINGS,
                resource = UserAuditResourceType.AUTH_SETTINGS,
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
            webSocketManager.sendMessageToScope(
                scope = ApiScope.OPEN,
                frame = match {
                    it.type == UserWebSocketEventTypes.OPEN_AUTH_SETTINGS_UPDATED
                }
            )
            webSocketManager.sendMessageToScope(
                scope = ApiScope.MANAGEMENT,
                frame = match {
                    it.type == UserWebSocketEventTypes.MANAGEMENT_AUTH_SETTINGS_UPDATED
                }
            )
        }
    }

    @Test
    fun `handles error, logs failure audit and does not broadcast`() = runTest {
        val settings = createTestManagementAuthSettings()
        val userId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = userId)
        val error = UserError.UserForbidden()
        val errorLogData = AuditErrorLogData(
            status = AuditStatus.FAILED,
            metadata = emptySet()
        )

        mockMfaCheckSuccess(userId)

        coEvery {
            authSettingsProvider.updateManagementAuthSettings(settings)
        } returns AppResult.Error(error)

        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(settings, context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.serialName,
                action = UserAuditActionType.MANAGEMENT_UPDATE_AUTH_SETTINGS,
                resource = UserAuditResourceType.AUTH_SETTINGS,
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }

        coVerify(exactly = 0) {
            authSettingsProvider.getOpenAuthSettings()
            authSettingsProvider.getManagementAuthSettings()
            webSocketManager.sendMessageToScope(any(), any())
        }
    }
}
