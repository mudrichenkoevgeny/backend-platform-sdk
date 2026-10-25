package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storagesettings

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.error.CommonAuditErrorParser
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage.settings.ResetStorageSettingsUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.audit.action.StorageAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.audit.resource.StorageAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.contract.StorageWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.storage.domain.model.storagesettings.createTestManagementStorageSettings
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.storage.domain.model.storagesettings.createTestOpenStorageSettings
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.session.createTestUserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ResetStorageSettingsUseCaseTest {

    private val storageSettingsProvider = mockk<StorageSettingsProvider>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val commonAuditErrorParser = CommonAuditErrorParser()
    private val auditErrorConverter = AuditErrorConverter(emptySet(), commonAuditErrorParser)
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()

    private lateinit var useCase: ResetStorageSettingsUseCase

    private val defaultSettings = createTestManagementStorageSettings()

    @BeforeEach
    fun setup() {
        useCase = ResetStorageSettingsUseCase(
            storageSettingsProvider,
            auditLogger,
            auditErrorConverter,
            webSocketManager,
            userManager,
            sessionManager,
            authenticationChallengeService
        )
    }

    private fun mockMfaCheckSuccess(userId: UserId) {
        val userInternal = createTestUserInternal(id = userId)
        val userSession = createTestUserSessionInternal(userId = userId)
        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(userInternal)
        coEvery { sessionManager.getUserSessionInternal(any()) } returns AppResult.Success(userSession)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(userInternal, userSession, true) } returns AppResult.Success(Unit)
    }

    @Test
    fun `invoke resets settings and logs successful audit`() = runBlocking {
        val userId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = userId)
        mockMfaCheckSuccess(userId)

        coEvery { storageSettingsProvider.resetManagementStorageSettings() } returns AppResult.Success(defaultSettings)
        every { storageSettingsProvider.getOpenStorageSettings() } returns createTestOpenStorageSettings()

        val result = useCase(context)

        assertTrue(result is AppResult.Success)
        assertEquals(defaultSettings, (result as AppResult.Success).data)
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.value,
                action = StorageAuditActionType.MANAGEMENT_RESET_STORAGE_SETTINGS,
                resource = StorageAuditResourceType.STORAGE_SETTINGS,
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
            webSocketManager.sendMessageToScope(
                scope = ApiScope.OPEN,
                frame = match { it.type == StorageWebSocketEventTypes.OPEN_STORAGE_SETTINGS_UPDATED }
            )
            webSocketManager.sendMessageToScope(
                scope = ApiScope.MANAGEMENT,
                frame = match { it.type == StorageWebSocketEventTypes.MANAGEMENT_STORAGE_SETTINGS_UPDATED }
            )
        }
    }

    @Test
    fun `invoke logs failed audit when reset fails`() = runBlocking {
        val userId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = userId)
        mockMfaCheckSuccess(userId)

        val expectedError = AppResult.Error(CommonError.Internal(RuntimeException("Reset error")))
        coEvery { storageSettingsProvider.resetManagementStorageSettings() } returns expectedError

        val result = useCase(context)

        assertEquals(expectedError, result)
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.value,
                action = StorageAuditActionType.MANAGEMENT_RESET_STORAGE_SETTINGS,
                resource = StorageAuditResourceType.STORAGE_SETTINGS,
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }
}