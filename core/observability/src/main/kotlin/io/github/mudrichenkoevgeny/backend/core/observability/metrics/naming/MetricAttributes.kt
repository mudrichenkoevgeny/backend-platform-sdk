package io.github.mudrichenkoevgeny.backend.core.observability.metrics.naming

/**
 * OpenTelemetry attribute names used for HTTP request metrics (endpoint path, method, status code).
 */
object MetricAttributes {

    const val ENDPOINT = "endpoint"
    const val METHOD = "method"
    const val STATUS_CODE = "status_code"
}