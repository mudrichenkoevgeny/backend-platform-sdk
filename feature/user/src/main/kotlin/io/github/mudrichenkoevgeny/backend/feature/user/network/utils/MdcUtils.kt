package io.github.mudrichenkoevgeny.backend.feature.user.network.utils

import io.github.mudrichenkoevgeny.backend.core.common.logs.naming.TracingKeys
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonHttpHeaders
import org.slf4j.MDC

/**
 * Safely extracts the current trace identifier from the SLF4J Mapped Diagnostic Context (MDC).
 *
 * Used as a fallback for correlation when the trace ID is not explicitly provided in the
 * [CommonHttpHeaders.TRACE_HEADER_NAME] request header.
 *
 * @return The trace ID string if present in MDC and accessible, null otherwise.
 */
fun getMdcTraceFromMdcOrNull(): String? = runCatching {
    MDC.get(TracingKeys.TRACE_ID_KEY)
}.getOrNull()