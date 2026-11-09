package io.github.mudrichenkoevgeny.backend.core.common.logs.naming

/**
 * Common MDC keys used for tracing and correlating log records.
 */
public object TracingKeys {
    /**
     * MDC key under which the current trace identifier is stored.
     */
    public const val TRACE_ID_KEY: String = "traceId"
}
