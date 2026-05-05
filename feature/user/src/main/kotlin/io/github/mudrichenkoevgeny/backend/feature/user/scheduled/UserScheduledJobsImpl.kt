package io.github.mudrichenkoevgeny.backend.feature.user.scheduled

import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BackgroundScope
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
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
 * Runs [UserManager.deleteUsersDueForPermanentDeletionForSystem] on a fixed delay while the background scope stays active.
 */
@Singleton
class UserScheduledJobsImpl @Inject constructor(
    private val userManager: UserManager,
    private val authSettingsProvider: AuthSettingsProvider,
    @param:BackgroundScope private val scope: CoroutineScope,
    private val appLogger: AppLogger
) : UserScheduledJobs {

    @Volatile
    private var loopJob: Job? = null

    private val loopLock = Any()

    override fun start() {
        val intervalSeconds = authSettingsProvider.getAccountDeletionDelaySeconds()
        if (intervalSeconds <= 0L) {
            return
        }

        synchronized(loopLock) {
            if (loopJob?.isActive == true) {
                return
            }
            loopJob = scope.launch {
                while (isActive) {
                    try {
                        val deleteUsersResult = userManager.deleteUsersDueForPermanentDeletionForSystem()
                        if (deleteUsersResult is AppResult.Error) {
                            appLogger.logError(deleteUsersResult.error)
                        }
                    } catch (t: Throwable) {
                        appLogger.logError(CommonError.Internal(t))
                    }
                    delay(intervalSeconds.seconds)
                }
            }
        }
    }
}
