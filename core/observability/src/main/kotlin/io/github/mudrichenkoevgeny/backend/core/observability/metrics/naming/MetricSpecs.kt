package io.github.mudrichenkoevgeny.backend.core.observability.metrics.naming

/**
 * OpenTelemetry metric names and descriptions for HTTP request metrics (total requests, latency, errors).
 */
public object MetricSpecs {
    public const val HTTP_REQUESTS_TOTAL: String = "http_requests_total"
    public const val HTTP_REQUESTS_TOTAL_DESCRIPTION: String = "Total HTTP requests"
    public const val HTTP_REQUEST_LATENCY_MS: String = "http_request_latency_ms"
    public const val HTTP_REQUEST_LATENCY_MS_DESCRIPTION: String = "Request latency in ms"
    public const val HTTP_REQUEST_ERRORS_TOTAL: String = "http_request_errors_total"
    public const val HTTP_REQUEST_ERRORS_TOTAL_DESCRIPTION: String = "Total HTTP errors"
}
