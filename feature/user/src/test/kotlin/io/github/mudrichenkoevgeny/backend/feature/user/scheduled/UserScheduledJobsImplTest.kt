package io.github.mudrichenkoevgeny.backend.feature.user.scheduled

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserScheduledJobsConfig
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.ofType
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class UserScheduledJobsImplTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `start does not call delete when interval is zero`() = runTest(dispatcher) {
        val bgScope = CoroutineScope(SupervisorJob() + dispatcher)
        val userManager = mockk<UserManager>(relaxed = true)
        val appLogger = mockk<AppLogger>(relaxed = true)
        val jobs = UserScheduledJobsImpl(
            userManager = userManager,
            config = UserScheduledJobsConfig(permanentAccountDeletionPollIntervalMinutes = 0L),
            scope = bgScope,
            appLogger = appLogger
        )

        jobs.start()
        advanceUntilIdle()

        coVerify(exactly = 0) { userManager.deleteUsersDueForPermanentDeletion() }
    }

    @Test
    fun `start invokes delete on positive interval`() = runTest(dispatcher) {
        val bgScope = CoroutineScope(SupervisorJob() + dispatcher)
        val userManager = mockk<UserManager>()
        coEvery { userManager.deleteUsersDueForPermanentDeletion() } returns AppResult.Success(0)
        val appLogger = mockk<AppLogger>(relaxed = true)
        val jobs = UserScheduledJobsImpl(
            userManager = userManager,
            config = UserScheduledJobsConfig(permanentAccountDeletionPollIntervalMinutes = 1L),
            scope = bgScope,
            appLogger = appLogger
        )

        jobs.start()
        advanceUntilIdle()

        coVerify(exactly = 1) { userManager.deleteUsersDueForPermanentDeletion() }
    }

    @Test
    fun `second start does not launch a second loop`() = runTest(dispatcher) {
        val bgScope = CoroutineScope(SupervisorJob() + dispatcher)
        val userManager = mockk<UserManager>()
        coEvery { userManager.deleteUsersDueForPermanentDeletion() } returns AppResult.Success(0)
        val appLogger = mockk<AppLogger>(relaxed = true)
        val jobs = UserScheduledJobsImpl(
            userManager = userManager,
            config = UserScheduledJobsConfig(permanentAccountDeletionPollIntervalMinutes = 1L),
            scope = bgScope,
            appLogger = appLogger
        )

        jobs.start()
        jobs.start()
        advanceUntilIdle()

        coVerify(exactly = 1) { userManager.deleteUsersDueForPermanentDeletion() }
    }

    @Test
    fun `start logs error when delete returns Error`() = runTest(dispatcher) {
        val bgScope = CoroutineScope(SupervisorJob() + dispatcher)
        val userManager = mockk<UserManager>()
        val failure = CommonError.Unknown("purge failed")
        coEvery { userManager.deleteUsersDueForPermanentDeletion() } returns AppResult.Error(failure)
        val appLogger = mockk<AppLogger>(relaxed = true)
        val jobs = UserScheduledJobsImpl(
            userManager = userManager,
            config = UserScheduledJobsConfig(permanentAccountDeletionPollIntervalMinutes = 1L),
            scope = bgScope,
            appLogger = appLogger
        )

        jobs.start()
        advanceUntilIdle()

        coVerify(exactly = 1) { userManager.deleteUsersDueForPermanentDeletion() }
        verify(exactly = 1) { appLogger.logError(ofType(CommonError.Unknown::class)) }
    }

    @Test
    fun `start invokes delete again after interval elapses`() = runTest(dispatcher) {
        val bgScope = CoroutineScope(SupervisorJob() + dispatcher)
        val userManager = mockk<UserManager>()
        coEvery { userManager.deleteUsersDueForPermanentDeletion() } returns AppResult.Success(0)
        val appLogger = mockk<AppLogger>(relaxed = true)
        val jobs = UserScheduledJobsImpl(
            userManager = userManager,
            config = UserScheduledJobsConfig(permanentAccountDeletionPollIntervalMinutes = 1L),
            scope = bgScope,
            appLogger = appLogger
        )

        jobs.start()
        advanceUntilIdle()
        coVerify(exactly = 1) { userManager.deleteUsersDueForPermanentDeletion() }

        advanceTimeBy(ONE_MINUTE_MS)
        advanceUntilIdle()

        coVerify(exactly = 2) { userManager.deleteUsersDueForPermanentDeletion() }
    }

    private companion object {
        const val ONE_MINUTE_MS = 60_000L
    }
}
