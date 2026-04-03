package io.github.mudrichenkoevgeny.backend.feature.user.config.model

/**
 * Intervals for optional user-feature background work started by the host app (not by the SDK).
 *
 * @property permanentAccountDeletionPollIntervalMinutes Minutes between host runs of permanent account deletion
 *   (users at or past `scheduled_permanent_deletion_at`). Use a positive value for periodic runs; use `0` for
 *   «do not schedule» (host interprets). Typical production: `60` (hourly) … `1440` (daily).
 */
data class UserScheduledJobsConfig(
    val permanentAccountDeletionPollIntervalMinutes: Long
)
