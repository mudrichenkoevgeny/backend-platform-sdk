package io.github.mudrichenkoevgeny.backend.core.common.error.model

/**
 * Severity of an [AppError], for logging, alerting, or metrics.
 *
 * Use to decide log level, whether to notify ops, or to aggregate error rates by severity.
 */
enum class AppErrorSeverity {

    /** Minor or expected validation/rate-limit issues; often not escalated. */
    LOW,

    /** Notable but not critical; may warrant attention (e.g. repeated auth failures). */
    MEDIUM,

    /** Critical or unexpected; should be logged prominently and may trigger alerts. */
    HIGH
}