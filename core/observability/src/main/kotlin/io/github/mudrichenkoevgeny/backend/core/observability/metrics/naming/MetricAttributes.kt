package io.github.mudrichenkoevgeny.backend.core.observability.metrics.naming

/**
 * OpenTelemetry attribute names used for HTTP request metrics (endpoint path, method, status code).
 */
public object MetricAttributes {
    public const val ENDPOINT: String = "endpoint"
    public const val METHOD: String = "method"
    public const val STATUS_CODE: String = "status_code"
}
