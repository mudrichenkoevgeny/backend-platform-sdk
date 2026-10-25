package io.github.mudrichenkoevgeny.backend.core.storage.scheduled

/**
 * Entry point for managing background maintenance tasks within the storage module.
 */
interface StorageScheduledJobs {

    /**
     * Starts periodic jobs (e.g. background cleanup of deleted files from S3/storage). If the configured interval is zero or negative, does nothing.
     * Subsequent calls while a loop is already running are ignored. Runs on the qualified application background scope.
     */
    fun start()
}
