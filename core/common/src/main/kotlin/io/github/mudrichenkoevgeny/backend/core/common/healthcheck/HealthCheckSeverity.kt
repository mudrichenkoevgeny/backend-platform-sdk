package io.github.mudrichenkoevgeny.backend.core.common.healthcheck

/**
 * Impact level of a health check failure.
 */
enum class HealthCheckSeverity {

    /**
     * A failure indicates that the service should not start or should fail fast.
     */
    CRITICAL,

    /**
     * A failure is logged and reported but does not prevent the service from running.
     */
    NON_CRITICAL,
}