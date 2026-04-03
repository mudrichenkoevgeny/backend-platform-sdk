package io.github.mudrichenkoevgeny.backend.core.common.network.request.model

/**
 * Per-request context extracted from incoming HTTP calls.
 *
 * Carries identifiers used for tracing, authorization and client analytics,
 * and is typically propagated through handlers and use cases instead of
 * passing raw framework types.
 *
 * @param traceId correlation id for logs and distributed tracing.
 * @param userId authenticated user identifier, if available.
 * @param sessionId authenticated session identifier, if available.
 * @param clientInfo structured information about the calling client.
 */
data class RequestContext(
    val traceId: String?,
    val userId: String?,
    val sessionId: String?,
    val clientInfo: ClientInfo
)