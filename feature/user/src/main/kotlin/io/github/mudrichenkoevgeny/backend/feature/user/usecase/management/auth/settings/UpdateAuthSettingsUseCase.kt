package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auth.settings

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.settings.toManagementAuthSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.settings.toOpenAuthSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.auth.settings.ManagementAuthSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.auth.settings.OpenAuthSettingsPayload
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid

/**
 * Use case for updating authentication settings from a management context.
 *
 * On successful persistence, notifies connected WebSocket clients in open and management scopes
 * with [UserWebSocketEventTypes.OPEN_AUTH_SETTINGS_UPDATED] and [UserWebSocketEventTypes.MANAGEMENT_AUTH_SETTINGS_UPDATED] frames.
 */
@Singleton
class UpdateAuthSettingsUseCase @Inject constructor(
    private val authSettingsProvider: AuthSettingsProvider,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val webSocketManager: WebSocketManager,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val authenticationChallengeService: AuthenticationChallengeService
) {
    /**
     * Updates global authentication settings and notifies all clients of the change.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE] for the management caller.
     *
     * **Security:**
     * - Requires an active management session (STAFF or ADMIN) with explicit update permissions.
     * - Validates the caller's account status to ensure they are fully active.
     *
     * **Workflow:**
     * 1. Persists the new authentication settings via [authSettingsProvider].
     * 2. Logs the modification via [AuditLogger] with [UserAuditActionType.MANAGEMENT_UPDATE_AUTH_SETTINGS].
     * 3. Retrieves the updated open and management settings snapshots.
     * 4. Broadcasts a [UserWebSocketEventTypes.OPEN_AUTH_SETTINGS_UPDATED] frame to open-scope clients
     *    and a [UserWebSocketEventTypes.MANAGEMENT_AUTH_SETTINGS_UPDATED] frame to management-scope clients
     *    via [webSocketManager] for real-time policy synchronization.
     *
     * @param managementAuthSettings The new configuration for global authentication settings.
     * @param authenticatedRequestContext The context of the authenticated management request.
     * @return [AppResult] indicating success or the specific [AppError].
     */
    suspend operator fun invoke(
        managementAuthSettings: ManagementAuthSettings,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata()

        val userResult = userManager.getUserByIdForSelf(authenticatedRequestContext.userId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (userResult) {
            is AppResult.Error -> return handleError(
                error = userResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> userResult.data
        }

        val userSessionResult = sessionManager.getUserSessionForSystem(
            userSessionId = authenticatedRequestContext.sessionId
        ).mapNotNullOrError(UserError.InvalidSession())

        val currentSession = when (userSessionResult) {
            is AppResult.Success -> userSessionResult.data
            is AppResult.Error -> return handleError(
                error = userSessionResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val ensureSessionConfirmedResult = authenticationChallengeService.ensureSessionConfirmed(
            userDetails = currentUser,
            userSession = currentSession,
            requireTotp = true
        )
        if (ensureSessionConfirmedResult is AppResult.Error) {
            return handleError(
                error = ensureSessionConfirmedResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val updateResult = authSettingsProvider.updateManagementAuthSettings(managementAuthSettings)

        if (updateResult is AppResult.Error) {
            return handleError(
                error = updateResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        logAudit(
            actorId = auditActorId,
            actorUserRole = auditActorUserRole,
            status = AuditStatus.SUCCESS,
            metadata = auditMetadata
        )

        val timestamp = System.currentTimeMillis()

        val publicAuthSettingPayload = authSettingsProvider.getOpenAuthSettings().toOpenAuthSettingsPayload()
        webSocketManager.sendMessageToScope(
            scope = ApiScope.OPEN,
            frame = SocketFrame(
                id = Uuid.random().toHexDashString(),
                type = UserWebSocketEventTypes.OPEN_AUTH_SETTINGS_UPDATED,
                timestamp = timestamp,
                payload = FoundationJson.encodeToJsonElement(
                    OpenAuthSettingsPayload.serializer(),
                    publicAuthSettingPayload
                ),
                metadata = emptyMap()
            )
        )

        val managementAuthSettingPayload = authSettingsProvider.getManagementAuthSettings().toManagementAuthSettingsPayload()
        webSocketManager.sendMessageToScope(
            scope = ApiScope.MANAGEMENT,
            frame = SocketFrame(
                id = Uuid.random().toHexDashString(),
                type = UserWebSocketEventTypes.MANAGEMENT_AUTH_SETTINGS_UPDATED,
                timestamp = timestamp,
                payload = FoundationJson.encodeToJsonElement(
                    ManagementAuthSettingsPayload.serializer(),
                    managementAuthSettingPayload
                ),
                metadata = emptyMap()
            )
        )

        return updateResult
    }

    private fun <T> handleError(
        error: AppError,
        actorId: String? = null,
        actorUserRole: UserRole? = null,
        baseMetadata: Set<AuditEventMetadata>
    ): AppResult<T> {
        val auditErrorLogData = auditErrorConverter.convert(error)
        logAudit(
            actorId = actorId,
            actorUserRole = actorUserRole,
            status = auditErrorLogData.status,
            metadata = baseMetadata + auditErrorLogData.metadata
        )
        return AppResult.Error(error)
    }

    private fun logAudit(
        actorId: String? = null,
        actorUserRole: UserRole? = null,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            actorUserRole = actorUserRole?.serialName,
            action = UserAuditActionType.MANAGEMENT_UPDATE_AUTH_SETTINGS,
            resource = UserAuditResourceType.AUTH_SETTINGS,
            status = status,
            metadata = metadata
        )
    }
}
