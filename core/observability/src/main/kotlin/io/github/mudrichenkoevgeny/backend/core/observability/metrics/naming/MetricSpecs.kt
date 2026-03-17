package io.github.mudrichenkoevgeny.backend.core.observability.metrics.naming

/**
 * OpenTelemetry metric names and descriptions for HTTP request metrics (total requests, latency, errors).
 */
object MetricSpecs {
    const val HTTP_REQUESTS_TOTAL = "http_requests_total"
    const val HTTP_REQUESTS_TOTAL_DESCRIPTION = "Total HTTP requests"
    const val HTTP_REQUEST_LATENCY_MS = "http_request_latency_ms"
    const val HTTP_REQUEST_LATENCY_MS_DESCRIPTION = "Request latency in ms"
    const val HTTP_REQUEST_ERRORS_TOTAL = "http_request_errors_total"
    const val HTTP_REQUEST_ERRORS_TOTAL_DESCRIPTION = "Total HTTP errors"
}