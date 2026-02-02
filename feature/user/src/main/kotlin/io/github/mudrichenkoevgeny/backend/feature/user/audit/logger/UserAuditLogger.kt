package io.github.mudrichenkoevgeny.backend.feature.user.audit.logger

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext

interface UserAuditLogger {
    fun logInternalError(
        requestContext: RequestContext,
        action: String,
        resource: String,
        resourceId: String? = null,
        metadata: Map<String, Any?> = mapOf()
    )

    fun logFail(
        requestContext: RequestContext,
        action: String,
        resource: String,
        resourceId: String? = null,
        type: String? = null,
        metadata: Map<String, Any?> = mapOf()
    )

    fun logSuccess(
        requestContext: RequestContext,
        action: String,
        resource: String,
        resourceId: String? = null,
        type: String? = null,
        metadata: Map<String, Any?> = mapOf()
    )
}