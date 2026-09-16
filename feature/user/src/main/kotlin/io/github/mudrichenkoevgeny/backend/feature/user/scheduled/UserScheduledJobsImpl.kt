package io.github.mudrichenkoevgeny.backend.feature.user.scheduled

import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BackgroundScope
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Runs background maintenance jobs for the user feature:
 * 1. [UserManager.deleteUsersDueForPermanentDeletionForSystem] on a configurable interval.
 * 2. [UserManager.unlockExpiredAccountLockoutsForSystem] on a configurable interval.
 */
@Singleton
class UserScheduledJobsImpl @Inject constructor(
    private val userManager: UserManager,
    private val securitySettingsProvider: SecuritySettingsProvider,
    private val authSettingsProvider: AuthSettingsProvider,
    @param:BackgroundScope private val scope: CoroutineScope,
    private val appLogger: AppLogger,
) : UserScheduledJobs {

    @Volatile
    private var accountDeletionJob: Job? = null

    @Volatile
    private var accountLockoutCheckJob: Job? = null

    private val loopLock = Any()

    override fun start() {
        synchronized(loopLock) {
            startAccountDeletionJob()
            startAccountLockoutCheckJob()
        }
    }

    private fun startAccountDeletionJob() {
        if (accountDeletionJob?.isActive == true) {
            return
        }

        accountDeletionJob = scope.launch {
            while (isActive) {
                try {
                    val deleteUsersResult = userManager.deleteUsersDueForPermanentDeletionForSystem()
                    if (deleteUsersResult is AppResult.Error) {
                        appLogger.logError(deleteUsersResult.error)
                    }
                } catch (t: Throwable) {
                    appLogger.logError(CommonError.Internal(t))
                }

                val intervalSeconds = authSettingsProvider.getAccountDeletionCheckIntervalSeconds()
                val delaySeconds = if (intervalSeconds > 0) intervalSeconds else DEFAULT_CHECK_INTERVAL_SECONDS
                delay(delaySeconds.seconds)
            }
        }
    }

    private fun startAccountLockoutCheckJob() {
        if (accountLockoutCheckJob?.isActive == true) {
            return
        }

        accountLockoutCheckJob = scope.launch {
            while (isActive) {
                try {
                    val unlockResult = userManager.unlockExpiredAccountLockoutsForSystem()
                    if (unlockResult is AppResult.Error) {
                        appLogger.logError(unlockResult.error)
                    }
                } catch (t: Throwable) {
                    appLogger.logError(CommonError.Internal(t))
                }

                val intervalSeconds = securitySettingsProvider.getAccountLockoutCheckIntervalSeconds()
                val delaySeconds = if (intervalSeconds > 0) intervalSeconds else DEFAULT_CHECK_INTERVAL_SECONDS
                delay(delaySeconds.seconds)
            }
        }
    }

    private companion object {
        const val DEFAULT_CHECK_INTERVAL_SECONDS = 60
    }
}
