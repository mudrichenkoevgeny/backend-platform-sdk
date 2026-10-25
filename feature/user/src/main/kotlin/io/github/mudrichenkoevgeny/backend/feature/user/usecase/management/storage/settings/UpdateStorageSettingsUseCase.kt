package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage.settings

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.audit.action.StorageAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.audit.resource.StorageAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.storagesettings.ManagementStorageSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.mapper.storagesettings.toManagementStorageSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.mapper.storagesettings.toOpenStorageSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.contract.StorageWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.model.storagesettings.ManagementStorageSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.model.storagesettings.OpenStorageSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid

/**
 * Use case for updating storage settings from a management context.
 *
 * On successful persistence, notifies connected WebSocket clients in open and management scopes
 * with [StorageWebSocketEventTypes.OPEN_STORAGE_SETTINGS_UPDATED] and [StorageWebSocketEventTypes.MANAGEMENT_STORAGE_SETTINGS_UPDATED] frames.
 */
@Singleton
class UpdateStorageSettingsUseCase @Inject constructor(
    private val storageSettingsProvider: StorageSettingsProvider,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val webSocketManager: WebSocketManager,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val authenticationChallengeService: AuthenticationChallengeService
) {
    /**
     * Updates global storage settings and synchronizes storage policies across all clients.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE] for the management caller.
     *
     * **Security:**
     * - Requires an active management session (STAFF or ADMIN) with explicit storage update permissions.
     * - Restricts modifications to fully active accounts to ensure integrity of storage policies.
     *
     * **Workflow:**
     * 1. Persists the updated [ManagementStorageSettings] via [storageSettingsProvider].
     * 2. Logs the administrative change via [AuditLogger] with [StorageAuditActionType.MANAGEMENT_UPDATE_STORAGE_SETTINGS].
     * 3. Retrieves the updated open and management settings snapshots.
     * 4. Broadcasts a [StorageWebSocketEventTypes.OPEN_STORAGE_SETTINGS_UPDATED] frame to open-scope clients
     *    and a [StorageWebSocketEventTypes.MANAGEMENT_STORAGE_SETTINGS_UPDATED] frame to management-scope clients
     *    via [webSocketManager] for immediate policy enforcement.
     *
     * @param managementStorageSettings The new storage configuration and policies to be applied globally.
     * @param authenticatedRequestContext The context of the authenticated management request.
     * @return [AppResult] indicating success or the specific [AppError].
     */
    suspend operator fun invoke(
        managementStorageSettings: ManagementStorageSettings,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<ManagementStorageSettings> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata()

        val userResult = userManager.getUserInternalById(authenticatedRequestContext.userId)
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

        val userSessionResult = sessionManager.getUserSessionInternal(
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
            userInternal = currentUser,
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

        val updateStorageSettingsResult = storageSettingsProvider.updateManagementStorageSettings(managementStorageSettings)

        if (updateStorageSettingsResult is AppResult.Error) {
            return handleError(
                error = updateStorageSettingsResult.error,
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

        val openSettingsPayload = storageSettingsProvider.getOpenStorageSettings().toOpenStorageSettingsPayload()
        webSocketManager.sendMessageToScope(
            scope = ApiScope.OPEN,
            frame = SocketFrame(
                id = Uuid.random().toHexDashString(),
                type = StorageWebSocketEventTypes.OPEN_STORAGE_SETTINGS_UPDATED,
                timestamp = timestamp,
                payload = FoundationJson.encodeToJsonElement(
                    OpenStorageSettingsPayload.serializer(),
                    openSettingsPayload
                ),
                metadata = emptyMap()
            )
        )

        val managementSettingsPayload = storageSettingsProvider.getManagementStorageSettings().toManagementStorageSettingsPayload()
        webSocketManager.sendMessageToScope(
            scope = ApiScope.MANAGEMENT,
            frame = SocketFrame(
                id = Uuid.random().toHexDashString(),
                type = StorageWebSocketEventTypes.MANAGEMENT_STORAGE_SETTINGS_UPDATED,
                timestamp = timestamp,
                payload = FoundationJson.encodeToJsonElement(
                    ManagementStorageSettingsPayload.serializer(),
                    managementSettingsPayload
                ),
                metadata = emptyMap()
            )
        )

        return updateStorageSettingsResult.mapSuccess {
            storageSettingsProvider.getManagementStorageSettings()
        }
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
            actorUserRole = actorUserRole?.value,
            action = StorageAuditActionType.MANAGEMENT_UPDATE_STORAGE_SETTINGS,
            resource = StorageAuditResourceType.STORAGE_SETTINGS,
            status = status,
            metadata = metadata
        )
    }
}