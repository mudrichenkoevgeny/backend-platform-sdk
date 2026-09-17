package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.settings

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.ManagementSecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.securitysettings.toManagementSecuritySettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.securitysettings.toOpenSecuritySettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.contract.SecurityWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.securitysettings.ManagementSecuritySettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.securitysettings.OpenSecuritySettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.audit.action.SecurityAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.audit.resource.SecurityAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid

/**
 * Use case for updating security settings from a management context.
 *
 * On successful persistence, notifies connected WebSocket clients in open and management scopes
 * with [SecurityWebSocketEventTypes.OPEN_SECURITY_SETTINGS_UPDATED] and [SecurityWebSocketEventTypes.MANAGEMENT_SECURITY_SETTINGS_UPDATED] frames.
 */
@Singleton
class UpdateSecuritySettingsUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val webSocketManager: WebSocketManager
) {
    /**
     * Updates global security settings and synchronizes security policies across all clients.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE] for the management caller.
     *
     * **Security:**
     * - Requires an active management session (STAFF or ADMIN) with explicit security update permissions.
     * - Restricts modifications to fully active accounts to ensure integrity of security policies.
     *
     * **Workflow:**
     * 1. Persists the updated [ManagementSecuritySettings] via [securitySettingsProvider].
     * 2. Logs the administrative change via [AuditLogger] with [SecurityAuditActionType.MANAGEMENT_UPDATE_SECURITY_SETTINGS].
     * 3. Retrieves the updated open and management settings snapshots.
     * 4. Broadcasts a [SecurityWebSocketEventTypes.OPEN_SECURITY_SETTINGS_UPDATED] frame to open-scope clients
     *    and a [SecurityWebSocketEventTypes.MANAGEMENT_SECURITY_SETTINGS_UPDATED] frame to management-scope clients
     *    via [webSocketManager] for immediate policy enforcement.
     *
     * @param managementSecuritySettings The new security configuration and policies to be applied globally.
     * @param authenticatedRequestContext The context of the authenticated management request.
     * @return [AppResult] indicating success or the specific [AppError].
     */
    suspend operator fun invoke(
        managementSecuritySettings: ManagementSecuritySettings,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata()

        val updateSecuritySettingsResult = securitySettingsProvider.updateManagementSecuritySettings(managementSecuritySettings)

        if (updateSecuritySettingsResult is AppResult.Error) {
            return handleError(
                error = updateSecuritySettingsResult.error,
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

        val openSecuritySettingsPayload = securitySettingsProvider.getOpenSecuritySettings().toOpenSecuritySettingsPayload()
        webSocketManager.sendMessageToScope(
            scope = ApiScope.OPEN,
            frame = SocketFrame(
                id = Uuid.random().toHexDashString(),
                type = SecurityWebSocketEventTypes.OPEN_SECURITY_SETTINGS_UPDATED,
                timestamp = timestamp,
                payload = FoundationJson.encodeToJsonElement(
                    OpenSecuritySettingsPayload.serializer(),
                    openSecuritySettingsPayload
                ),
                metadata = emptyMap()
            )
        )

        val managementSecuritySettingsPayload = securitySettingsProvider.getManagementSecuritySettings().toManagementSecuritySettingsPayload()
        webSocketManager.sendMessageToScope(
            scope = ApiScope.MANAGEMENT,
            frame = SocketFrame(
                id = Uuid.random().toHexDashString(),
                type = SecurityWebSocketEventTypes.MANAGEMENT_SECURITY_SETTINGS_UPDATED,
                timestamp = timestamp,
                payload = FoundationJson.encodeToJsonElement(
                    ManagementSecuritySettingsPayload.serializer(),
                    managementSecuritySettingsPayload
                ),
                metadata = emptyMap()
            )
        )

        return updateSecuritySettingsResult
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
            action = SecurityAuditActionType.MANAGEMENT_UPDATE_SECURITY_SETTINGS,
            resource = SecurityAuditResourceType.SECURITY_SETTINGS,
            status = status,
            metadata = metadata
        )
    }
}
