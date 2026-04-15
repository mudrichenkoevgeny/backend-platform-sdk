package io.github.mudrichenkoevgeny.backend.feature.settings.api.usecase.management.globalsettings

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.audit.toErrorUserAuditEventMetadata
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
import io.github.mudrichenkoevgeny.shared.foundation.feature.settings.api.domain.audit.action.SettingsAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.settings.api.domain.audit.resource.SettingsAuditResourceType
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
    private val webSocketManager: WebSocketManager
) {
    /**
     * Persists [globalSettings] via [GlobalSettingsProvider] and records an audit event reflecting
     * success or failure of the update.
     */
    suspend operator fun invoke(
        globalSettings: GlobalSettings,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        val updateGlobalSettingsResult = globalSettingsProvider.updateGlobalSettings(globalSettings)

        val metadata: Set<AuditEventMetadata> = authenticatedRequestContext.clientInfo.toAuditMetadata()
        if (updateGlobalSettingsResult is AppResult.Error) {
            metadata + updateGlobalSettingsResult.error.toErrorUserAuditEventMetadata()
        }

        auditLogger.log(
            actorId = authenticatedRequestContext.userId.asHexDashString(),
            actorType = AuditActorType.USER,
            actorUserRole = null,
            action = SettingsAuditActionType.MANAGEMENT_UPDATE_GLOBAL_SETTINGS,
            resource = SettingsAuditResourceType.GLOBAL_SETTINGS,
            status = when (updateGlobalSettingsResult) {
                is AppResult.Success -> {
                    AuditStatus.SUCCESS
                }
                is AppResult.Error -> {
                    AuditStatus.FAILED
                }
            },
            metadata = metadata
        )

        if (updateGlobalSettingsResult is AppResult.Success) {
            val globalSettingsPayload = globalSettings.toGlobalSettingsPayload()
            webSocketManager.sendMessageToAll(
                SocketFrame(
                    type = SettingsWebSocketEventTypes.GLOBAL_SETTINGS_UPDATED,
                    timestamp = System.currentTimeMillis(),
                    payload = FoundationJson.encodeToJsonElement(
                        GlobalSettingsPayload.serializer(),
                        globalSettingsPayload
                    )
                )
            )
        }

        return updateGlobalSettingsResult
    }
}