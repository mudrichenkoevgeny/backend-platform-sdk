package io.github.mudrichenkoevgeny.backend.feature.user.scheduled

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

private const val TEST_INTERVAL_SECONDS = 60

class UserScheduledJobsImplTest {

    private val userManager = mockk<UserManager>()
    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val appLogger = mockk<AppLogger>(relaxed = true)

    private fun createJobs(scope: CoroutineScope) = UserScheduledJobsImpl(
        userManager = userManager,
        authSettingsProvider = authSettingsProvider,
        scope = scope,
        appLogger = appLogger
    )

    @Test
    fun `should invoke delete and unlock immediately on start and then after interval`() = runTest {
        every { authSettingsProvider.getAccountDeletionDelaySeconds() } returns TEST_INTERVAL_SECONDS
        every { authSettingsProvider.getAccountLockoutCheckIntervalSeconds() } returns TEST_INTERVAL_SECONDS
        coEvery { userManager.deleteUsersDueForPermanentDeletionForSystem() } returns AppResult.Success(1)
        coEvery { userManager.unlockExpiredAccountLockoutsForSystem() } returns AppResult.Success(2)
        val jobs = createJobs(backgroundScope)

        jobs.start()

        advanceTimeBy(1.seconds)
        coVerify(exactly = 1) { userManager.deleteUsersDueForPermanentDeletionForSystem() }
        coVerify(exactly = 1) { userManager.unlockExpiredAccountLockoutsForSystem() }

        advanceTimeBy(TEST_INTERVAL_SECONDS.seconds)
        coVerify(exactly = 2) { userManager.deleteUsersDueForPermanentDeletionForSystem() }
        coVerify(exactly = 2) { userManager.unlockExpiredAccountLockoutsForSystem() }
    }

    @Test
    fun `should not launch second loops if already active`() = runTest {
        every { authSettingsProvider.getAccountDeletionDelaySeconds() } returns TEST_INTERVAL_SECONDS
        every { authSettingsProvider.getAccountLockoutCheckIntervalSeconds() } returns TEST_INTERVAL_SECONDS
        coEvery { userManager.deleteUsersDueForPermanentDeletionForSystem() } returns AppResult.Success(0)
        coEvery { userManager.unlockExpiredAccountLockoutsForSystem() } returns AppResult.Success(0)
        val jobs = createJobs(backgroundScope)

        jobs.start()
        jobs.start()

        advanceTimeBy(1.seconds)
        coVerify(exactly = 1) { userManager.deleteUsersDueForPermanentDeletionForSystem() }
        coVerify(exactly = 1) { userManager.unlockExpiredAccountLockoutsForSystem() }
    }

    @Test
    fun `should log error when manager returns failure result`() = runTest {
        val error = CommonError.Internal(Exception("DB error"))
        every { authSettingsProvider.getAccountDeletionDelaySeconds() } returns TEST_INTERVAL_SECONDS
        every { authSettingsProvider.getAccountLockoutCheckIntervalSeconds() } returns TEST_INTERVAL_SECONDS
        coEvery { userManager.deleteUsersDueForPermanentDeletionForSystem() } returns AppResult.Error(error)
        coEvery { userManager.unlockExpiredAccountLockoutsForSystem() } returns AppResult.Error(error)
        val jobs = createJobs(backgroundScope)

        jobs.start()
        advanceTimeBy(1.seconds)

        verify(atLeast = 1) { appLogger.logError(error) }
    }

    @Test
    fun `should log internal error when exception is thrown during execution`() = runTest {
        val exception = RuntimeException("Unexpected")
        every { authSettingsProvider.getAccountDeletionDelaySeconds() } returns TEST_INTERVAL_SECONDS
        every { authSettingsProvider.getAccountLockoutCheckIntervalSeconds() } returns TEST_INTERVAL_SECONDS
        coEvery { userManager.deleteUsersDueForPermanentDeletionForSystem() } throws exception
        coEvery { userManager.unlockExpiredAccountLockoutsForSystem() } returns AppResult.Success(0)
        val jobs = createJobs(backgroundScope)

        jobs.start()
        advanceTimeBy(1.seconds)

        verify {
            appLogger.logError(match { error ->
                error is CommonError.Internal && error.throwable == exception
            })
        }
    }

    @Test
    fun `loops should stop when scope is cancelled`() = runTest {
        every { authSettingsProvider.getAccountDeletionDelaySeconds() } returns TEST_INTERVAL_SECONDS
        every { authSettingsProvider.getAccountLockoutCheckIntervalSeconds() } returns TEST_INTERVAL_SECONDS
        coEvery { userManager.deleteUsersDueForPermanentDeletionForSystem() } returns AppResult.Success(0)
        coEvery { userManager.unlockExpiredAccountLockoutsForSystem() } returns AppResult.Success(0)

        val testScope = CoroutineScope(coroutineContext + SupervisorJob())
        val jobs = createJobs(testScope)

        jobs.start()
        advanceTimeBy(1.seconds)
        coVerify(exactly = 1) { userManager.deleteUsersDueForPermanentDeletionForSystem() }
        coVerify(exactly = 1) { userManager.unlockExpiredAccountLockoutsForSystem() }

        testScope.cancel()
        advanceTimeBy(TEST_INTERVAL_SECONDS.seconds)

        coVerify(exactly = 1) { userManager.deleteUsersDueForPermanentDeletionForSystem() }
        coVerify(exactly = 1) { userManager.unlockExpiredAccountLockoutsForSystem() }
    }
}
