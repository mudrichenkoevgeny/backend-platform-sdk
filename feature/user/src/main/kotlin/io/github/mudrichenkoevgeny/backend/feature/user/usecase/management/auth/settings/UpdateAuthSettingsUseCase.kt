package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auth.settings

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.audit.toErrorUserAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.settings.toAuthSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.auth.settings.PublicAuthSettingsPayload
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid

/**
 * Use case for updating persisted auth settings from a management context.
 *
 * On successful persistence, notifies all connected WebSocket clients (including public sessions)
 * with [UserWebSocketEventTypes.AUTH_SETTINGS_UPDATED]. The frame payload is a serialized
 * [PublicAuthSettingsPayload] (same shape as the open GET auth settings response).
 */
@Singleton
class UpdateAuthSettingsUseCase @Inject constructor(
    private val authSettingsProvider: AuthSettingsProvider,
    private val auditLogger: AuditLogger,
    private val webSocketManager: WebSocketManager
) {
    suspend operator fun invoke(
        managementAuthSettings: ManagementAuthSettings,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        val updateResult = authSettingsProvider.updateManagementAuthSettings(managementAuthSettings)

        val metadata: Set<AuditEventMetadata> = authenticatedRequestContext.clientInfo.toAuditMetadata()
        if (updateResult is AppResult.Error) {
            metadata + updateResult.error.toErrorUserAuditEventMetadata()
        }

        auditLogger.log(
            actorId = authenticatedRequestContext.userId.asHexDashString(),
            actorType = AuditActorType.USER,
            actorUserRole = null,
            action = UserAuditActionType.MANAGEMENT_UPDATE_AUTH_SETTINGS,
            resource = UserAuditResourceType.AUTH_SETTINGS,
            status = when (updateResult) {
                is AppResult.Success -> AuditStatus.SUCCESS
                is AppResult.Error -> AuditStatus.FAILED
            },
            metadata = metadata
        )

        if (updateResult is AppResult.Success) {
            val publicResult = authSettingsProvider.getPublicAuthSettings()
            if (publicResult is AppResult.Success) {
                val payload = publicResult.data.toAuthSettingsPayload()
                webSocketManager.sendMessageToAll(
                    SocketFrame(
                        id = Uuid.random().toHexDashString(),
                        type = UserWebSocketEventTypes.AUTH_SETTINGS_UPDATED,
                        timestamp = System.currentTimeMillis(),
                        payload = FoundationJson.encodeToJsonElement(
                            PublicAuthSettingsPayload.serializer(),
                            payload
                        )
                    )
                )
            }
        }

        return updateResult
    }
}
