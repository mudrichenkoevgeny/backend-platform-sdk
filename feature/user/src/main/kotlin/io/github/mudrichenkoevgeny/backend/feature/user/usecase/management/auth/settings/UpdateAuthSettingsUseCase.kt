package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auth.settings

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.settings.toAuthSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.auth.settings.PublicAuthSettingsPayload
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid

@Singleton
class UpdateAuthSettingsUseCase @Inject constructor(
    private val authSettingsProvider: AuthSettingsProvider,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val webSocketManager: WebSocketManager
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
     * 3. Retrieves the updated public portion of the settings.
     * 4. Broadcasts a [UserWebSocketEventTypes.AUTH_SETTINGS_UPDATED] frame to all connected clients
     *    via [webSocketManager] to ensure real-time synchronization of system auth policies.
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

        val publicAuthSettingPayload = authSettingsProvider.getPublicAuthSettings().toAuthSettingsPayload()
        webSocketManager.sendMessageToAll(
            SocketFrame(
                id = Uuid.random().toHexDashString(),
                type = UserWebSocketEventTypes.AUTH_SETTINGS_UPDATED,
                timestamp = System.currentTimeMillis(),
                payload = FoundationJson.encodeToJsonElement(
                    PublicAuthSettingsPayload.serializer(),
                    publicAuthSettingPayload
                )
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