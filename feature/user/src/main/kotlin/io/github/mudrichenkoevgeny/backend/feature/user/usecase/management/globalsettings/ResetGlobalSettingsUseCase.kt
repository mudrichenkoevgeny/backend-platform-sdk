package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.globalsettings

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.audit.action.SettingsAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.audit.resource.SettingsAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.mapper.globalsettings.toManagementGlobalSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.mapper.globalsettings.toOpenGlobalSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.model.globalsettings.ManagementGlobalSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.model.globalsettings.OpenGlobalSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.contract.SettingsWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid

/**
 * Use case for resetting global settings to default values from a management context.
 */
@Singleton
class ResetGlobalSettingsUseCase @Inject constructor(
    private val globalSettingsProvider: GlobalSettingsProvider,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val webSocketManager: WebSocketManager
) {
    /**
     * Resets platform-wide global settings to defaults and notifies connected clients.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE] for the management caller.
     *
     * **Security:**
     * - Requires an active management session (STAFF or ADMIN) with explicit update permissions.
     * - Validates the caller's account status.
     *
     * **Workflow:**
     * 1. Resets global settings to defaults via [globalSettingsProvider].
     * 2. Logs the reset via [AuditLogger] with [SettingsAuditActionType.MANAGEMENT_RESET_GLOBAL_SETTINGS].
     * 3. Broadcasts [SettingsWebSocketEventTypes.OPEN_GLOBAL_SETTINGS_UPDATED] and [SettingsWebSocketEventTypes.MANAGEMENT_GLOBAL_SETTINGS_UPDATED] frames.
     *
     * @param authenticatedRequestContext The context of the authenticated management request.
     * @return [AppResult] containing the reset [ManagementGlobalSettings] or an [AppError].
     */
    suspend operator fun invoke(
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<ManagementGlobalSettings> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata()

        val resetResult = globalSettingsProvider.resetManagementGlobalSettings()

        val resetSettings = when (resetResult) {
            is AppResult.Success -> resetResult.data
            is AppResult.Error -> return handleError(
                error = resetResult.error,
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

        val publicGlobalSettingsPayload = globalSettingsProvider.getOpenGlobalSettings().toOpenGlobalSettingsPayload()
        webSocketManager.sendMessageToScope(
            scope = ApiScope.OPEN,
            frame = SocketFrame(
                id = Uuid.random().toHexDashString(),
                type = SettingsWebSocketEventTypes.OPEN_GLOBAL_SETTINGS_UPDATED,
                timestamp = timestamp,
                payload = FoundationJson.encodeToJsonElement(
                    OpenGlobalSettingsPayload.serializer(),
                    publicGlobalSettingsPayload
                ),
                metadata = emptyMap()
            )
        )

        val managementGlobalSettingsPayload = resetSettings.toManagementGlobalSettingsPayload()
        webSocketManager.sendMessageToScope(
            scope = ApiScope.MANAGEMENT,
            frame = SocketFrame(
                id = Uuid.random().toHexDashString(),
                type = SettingsWebSocketEventTypes.MANAGEMENT_GLOBAL_SETTINGS_UPDATED,
                timestamp = timestamp,
                payload = FoundationJson.encodeToJsonElement(
                    ManagementGlobalSettingsPayload.serializer(),
                    managementGlobalSettingsPayload
                ),
                metadata = emptyMap()
            )
        )

        return AppResult.Success(resetSettings)
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
            action = SettingsAuditActionType.MANAGEMENT_RESET_GLOBAL_SETTINGS,
            resource = SettingsAuditResourceType.GLOBAL_SETTINGS,
            status = status,
            metadata = metadata
        )
    }
}
