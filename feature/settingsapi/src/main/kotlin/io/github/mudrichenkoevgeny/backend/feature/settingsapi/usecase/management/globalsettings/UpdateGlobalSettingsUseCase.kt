package io.github.mudrichenkoevgeny.backend.feature.settingsapi.usecase.management.globalsettings

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.GlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.mapper.globalsettings.toGlobalSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.contract.SettingsWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.model.globalsettings.GlobalSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.settingsapi.domain.audit.action.SettingsAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.settingsapi.domain.audit.resource.SettingsAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid

/**
 * Use case for updating global settings from a management context.
 *
 * On successful persistence, notifies all connected WebSocket clients (including public sessions)
 * with [SettingsWebSocketEventTypes.GLOBAL_SETTINGS_UPDATED] and the updated [GlobalSettingsPayload].
 */
@Singleton
class UpdateGlobalSettingsUseCase @Inject constructor(
    private val globalSettingsProvider: GlobalSettingsProvider,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val webSocketManager: WebSocketManager
) {
    /**
     * Updates platform-wide global settings and synchronizes all connected clients.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE] for the management caller.
     *
     * **Security:**
     * - Requires an active management session (STAFF or ADMIN) with specific update permissions.
     * - Restricts modifications to fully active accounts to prevent unauthorized system-wide changes.
     *
     * **Workflow:**
     * 1. Persists the updated [GlobalSettings] via [globalSettingsProvider].
     * 2. Logs the administrative action via [AuditLogger] with [SettingsAuditActionType.MANAGEMENT_UPDATE_GLOBAL_SETTINGS].
     * 3. Broadcasts a [SettingsWebSocketEventTypes.GLOBAL_SETTINGS_UPDATED] frame with the new payload
     *    to all connected clients via [webSocketManager] for real-time synchronization.
     *
     * @param globalSettings The new platform-wide configuration settings.
     * @param authenticatedRequestContext The context of the authenticated management request.
     * @return [AppResult] indicating success or the specific [AppError].
     */
    suspend operator fun invoke(
        globalSettings: GlobalSettings,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata()

        val updateGlobalSettingsResult = globalSettingsProvider.updateGlobalSettings(globalSettings)

        if (updateGlobalSettingsResult is AppResult.Error) {
            return handleError(
                error = updateGlobalSettingsResult.error,
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

        val globalSettingsPayload = globalSettings.toGlobalSettingsPayload()
        webSocketManager.sendMessageToAll(
            SocketFrame(
                id = Uuid.random().toHexDashString(),
                type = SettingsWebSocketEventTypes.GLOBAL_SETTINGS_UPDATED,
                timestamp = System.currentTimeMillis(),
                payload = FoundationJson.encodeToJsonElement(
                    GlobalSettingsPayload.serializer(),
                    globalSettingsPayload
                )
            )
        )

        return updateGlobalSettingsResult
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
            action = SettingsAuditActionType.MANAGEMENT_UPDATE_GLOBAL_SETTINGS,
            resource = SettingsAuditResourceType.GLOBAL_SETTINGS,
            status = status,
            metadata = metadata
        )
    }
}