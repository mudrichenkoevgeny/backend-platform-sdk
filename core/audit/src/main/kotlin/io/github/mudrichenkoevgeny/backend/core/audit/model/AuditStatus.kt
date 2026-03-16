package io.github.mudrichenkoevgeny.backend.core.audit.model

/**
 * Result status of an audited action.
 *
 * Used in [AuditEvent] to indicate whether the action completed successfully,
 * failed, or was denied (e.g. by authorization).
 */
enum class AuditStatus {
    /** Action completed successfully. */
    SUCCESS,

    /** Action was attempted but failed (e.g. validation, business error). */
    FAILED,

    /** Action was denied (e.g. insufficient permissions). */
    DENIED
}