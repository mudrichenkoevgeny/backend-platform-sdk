package io.github.mudrichenkoevgeny.backend.feature.user.audit.logger

import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext

// todo delete
/**
 * Writes audit records for user/auth flows.
 *
 * This abstraction is used by the feature to record security-relevant actions and outcomes
 * (success, failure, internal errors) while consistently enriching events with metadata derived
 * from the [RequestContext].
 */
interface UserAuditLogger {
    /**
     * Logs an audit record for an internal error.
     *
     * @param requestContext request context used to populate actor and client metadata
     * @param action action name (e.g. "login", "register")
     * @param resource resource name (e.g. "session", "user")
     * @param resourceId optional resource id
     * @param metadata additional metadata values to attach to the audit record
     */
    fun logInternalError(
        requestContext: RequestContext,
        action: String,
        resource: String,
        resourceId: String? = null,
        metadata: Map<String, Any?> = mapOf()
    )

    /**
     * Logs an audit record for a failed attempt.
     *
     * @param requestContext request context used to populate actor and client metadata
     * @param action action name (e.g. "login", "register")
     * @param resource resource name (e.g. "session", "user")
     * @param resourceId optional resource id
     * @param type optional stable failure classification (e.g. a value from `UserAuditMetadata.Types`)
     * @param metadata additional metadata values to attach to the audit record
     */
    fun logFail(
        requestContext: RequestContext,
        action: String,
        resource: String,
        resourceId: String? = null,
        type: String? = null,
        metadata: Map<String, Any?> = mapOf()
    )

    /**
     * Logs an audit record for a successful action.
     *
     * @param requestContext request context used to populate actor and client metadata
     * @param action action name (e.g. "login", "register")
     * @param resource resource name (e.g. "session", "user")
     * @param resourceId optional resource id
     * @param type optional stable success classification (e.g. a value from `UserAuditMetadata.Types`)
     * @param metadata additional metadata values to attach to the audit record
     */
    fun logSuccess(
        requestContext: RequestContext,
        action: String,
        resource: String,
        resourceId: String? = null,
        type: String? = null,
        metadata: Map<String, Any?> = mapOf()
    )
}