package io.github.mudrichenkoevgeny.backend.core.storage.scheduled

import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BackgroundScope
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Runs background maintenance jobs for the storage module:
 * 1. [StorageManager.cleanupDeletedStorageFiles] on a configurable interval.
 */
@Singleton
class StorageScheduledJobsImpl @Inject constructor(
    private val storageManager: StorageManager,
    private val storageSettingsProvider: StorageSettingsProvider,
    @param:BackgroundScope private val scope: CoroutineScope,
    private val appLogger: AppLogger
) : StorageScheduledJobs {

    @Volatile
    private var deletedFilesCleanupJob: Job? = null

    private val loopLock = Any()

    override fun start() {
        synchronized(loopLock) {
            startDeletedFilesCleanupJob()
        }
    }

    private fun startDeletedFilesCleanupJob() {
        if (deletedFilesCleanupJob?.isActive == true) {
            return
        }

        deletedFilesCleanupJob = scope.launch {
            while (isActive) {
                try {
                    val cleanupResult = storageManager.cleanupDeletedStorageFiles()
                    if (cleanupResult is AppResult.Error) {
                        appLogger.logError(cleanupResult.error)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    appLogger.logError(CommonError.Internal(e))
                }

                val intervalSeconds = storageSettingsProvider.getDeletedFilesCleanupIntervalSeconds()
                val delaySeconds = if (intervalSeconds > 0) {
                    intervalSeconds
                } else {
                    DEFAULT_CLEANUP_INTERVAL_SECONDS
                }
                delay(delaySeconds.seconds)
            }
        }
    }

    private companion object {
        const val DEFAULT_CLEANUP_INTERVAL_SECONDS = 300
    }
}
