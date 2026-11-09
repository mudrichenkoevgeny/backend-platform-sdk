package io.github.mudrichenkoevgeny.backend.core.observability.metrics.naming

/**
 * Default unit and value constants for observability metrics (counters and latency histograms).
 */
public object MetricDefaults {
    public const val COUNTER_UNIT: String = "1"
    public const val LATENCY_UNIT: String = "ms"
    public const val COUNTER_VALUE: Long = 1L
}
