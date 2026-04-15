package io.github.mudrichenkoevgeny.backend.feature.user.network.utils

import io.github.mudrichenkoevgeny.backend.core.common.logs.naming.TracingKeys
import org.slf4j.MDC

fun getMdcTraceFromMdcOrNull(): String? = runCatching {
    MDC.get(TracingKeys.TRACE_ID_KEY)
}.getOrNull()