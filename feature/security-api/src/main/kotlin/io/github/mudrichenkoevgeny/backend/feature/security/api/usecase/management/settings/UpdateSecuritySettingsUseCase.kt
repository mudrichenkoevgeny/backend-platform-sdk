package io.github.mudrichenkoevgeny.backend.feature.security.api.usecase.management.settings

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.audit.toErrorUserAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.securitysettings.toSecuritySettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.contract.SecurityWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.securitysettings.SecuritySettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.security.api.domain.audit.action.SecurityAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.security.api.domain.audit.resource.SecurityAuditResourceType
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid

/**
 * Use case for updating persisted security settings from a management context.
 *
 * On successful persistence, notifies all connected WebSocket clients (including public sessions)
 * with [SecurityWebSocketEventTypes.SECURITY_SETTINGS_UPDATED] and the updated [SecuritySettingsPayload].
 */
@Singleton
class UpdateSecuritySettingsUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider,
    private val auditLogger: AuditLogger,
    private val webSocketManager: WebSocketManager
) {
    suspend operator fun invoke(
        securitySettings: SecuritySettings,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        val updateSecuritySettingsResult = securitySettingsProvider.updateSecuritySettings(securitySettings)

        val metadata: Set<AuditEventMetadata> = authenticatedRequestContext.clientInfo.toAuditMetadata()
        if (updateSecuritySettingsResult is AppResult.Error) {
            metadata + updateSecuritySettingsResult.error.toErrorUserAuditEventMetadata()
        }

        auditLogger.log(
            actorId = authenticatedRequestContext.userId.asHexDashString(),
            actorType = AuditActorType.USER,
            actorUserRole = null,
            action = SecurityAuditActionType.MANAGEMENT_UPDATE_SECURITY_SETTINGS,
            resource = SecurityAuditResourceType.SECURITY_SETTINGS,
            status = when (updateSecuritySettingsResult) {
                is AppResult.Success -> {
                    AuditStatus.SUCCESS
                }
                is AppResult.Error -> {
                    AuditStatus.FAILED
                }
            },
            metadata = metadata
        )

        if (updateSecuritySettingsResult is AppResult.Success) {
            val securitySettingsPayload = securitySettings.toSecuritySettingsPayload()
            webSocketManager.sendMessageToAll(
                SocketFrame(
                    id = Uuid.random().toHexDashString(),
                    type = SecurityWebSocketEventTypes.SECURITY_SETTINGS_UPDATED,
                    timestamp = System.currentTimeMillis(),
                    payload = FoundationJson.encodeToJsonElement(
                        SecuritySettingsPayload.serializer(),
                        securitySettingsPayload
                    )
                )
            )
        }

        return updateSecuritySettingsResult
    }
}
