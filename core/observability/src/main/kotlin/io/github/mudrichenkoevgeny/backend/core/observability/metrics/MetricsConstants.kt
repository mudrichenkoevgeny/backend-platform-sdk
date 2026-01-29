package io.github.mudrichenkoevgeny.backend.core.observability.metrics

object MetricsConstants {
    const val HTTP_REQUESTS_TOTAL = "http_requests_total"
    const val HTTP_REQUESTS_TOTAL_DESCRIPTION = "Total HTTP requests"
    const val HTTP_REQUEST_LATENCY_MS = "http_request_latency_ms"
    const val HTTP_REQUEST_LATENCY_MS_DESCRIPTION = "Request latency in ms"
    const val HTTP_REQUEST_ERRORS_TOTAL = "http_request_errors_total"
    const val HTTP_REQUEST_ERRORS_TOTAL_DESCRIPTION = "Total HTTP errors"

    const val COUNTER_UNIT = "1"
    const val LATENCY_UNIT = "ms"

    const val ATTR_ENDPOINT = "endpoint"
    const val ATTR_METHOD = "method"
    const val ATTR_STATUS_CODE = "status_code"

    const val COUNTER_VALUE = 1L
}