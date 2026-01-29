package io.github.mudrichenkoevgeny.backend.feature.user.audit.logger

import io.github.mudrichenkoevgeny.backend.core.audit.enums.AuditStatus
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEvent
import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.util.toJsonElementMap
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAuditLoggerImpl @Inject constructor(
    private val auditService: AuditService
) : UserAuditLogger {

    override fun logInternalError(
        requestContext: RequestContext,
        action: String,
        resource: String,
        resourceId: String?,
        metadata: Map<String, Any?>
    ) {
        auditService.log(
            AuditEvent(
                actorId = requestContext.userId?.value,
                action = action,
                resource = resource,
                resourceId = resourceId,
                status = AuditStatus.FAILED,
                metadata = getMetadata(requestContext, UserAuditMetadata.Types.INTERNAL_ERROR, metadata)
            )
        )
    }

    override fun logFail(
        requestContext: RequestContext,
        action: String,
        resource: String,
        resourceId: String?,
        type: String?,
        metadata: Map<String, Any?>
    ) {
        auditService.log(
            AuditEvent(
                actorId = requestContext.userId?.value,
                action = action,
                resource = resource,
                resourceId = resourceId,
                status = AuditStatus.FAILED,
                metadata = getMetadata(requestContext, type, metadata)
            )
        )
    }

    override fun logSuccess(
        requestContext: RequestContext,
        action: String,
        resource: String,
        resourceId: String?,
        type: String?,
        metadata: Map<String, Any?>
    ) {
        auditService.log(
            AuditEvent(
                actorId = requestContext.userId?.value,
                action = action,
                resource = resource,
                resourceId = resourceId,
                status = AuditStatus.SUCCESS,
                metadata = getMetadata(requestContext, type, metadata)
            )
        )
    }

    private fun getMetadata(
        requestContext: RequestContext,
        type: String?,
        metadata: Map<String, Any?>
    ): Map<String, JsonElement> {
        return buildMap {
            put(UserAuditMetadata.Keys.IP_ADDRESS, requestContext.clientInfo.ipAddress)
            put(UserAuditMetadata.Keys.DEVICE_NAME, requestContext.clientInfo.deviceName)
            type?.let { put(UserAuditMetadata.Keys.TYPE, it) }

            metadata.forEach { (key, value) ->
                put(key, value)
            }
        }.toJsonElementMap()
    }
}