package io.github.mudrichenkoevgeny.backend.feature.user.scheduled

/**
 * Entry point for managing background maintenance tasks within the user feature.
 */
interface UserScheduledJobs {

    /**
     * Starts periodic jobs (e.g. permanent account purge). If the configured interval is zero or negative, does nothing.
     * Subsequent calls while a loop is already running are ignored. Runs on the qualified application background scope.
     */
    fun start()
}