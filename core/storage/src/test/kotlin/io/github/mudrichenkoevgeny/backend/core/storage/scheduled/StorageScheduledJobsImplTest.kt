package io.github.mudrichenkoevgeny.backend.core.storage.scheduled

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
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

private const val TEST_INTERVAL_SECONDS = 300

class StorageScheduledJobsImplTest {

    private val storageManager = mockk<StorageManager>()
    private val storageSettingsProvider = mockk<StorageSettingsProvider>()
    private val appLogger = mockk<AppLogger>(relaxed = true)

    private fun createJobs(scope: CoroutineScope) = StorageScheduledJobsImpl(
        storageManager = storageManager,
        storageSettingsProvider = storageSettingsProvider,
        scope = scope,
        appLogger = appLogger
    )

    @Test
    fun `should invoke cleanupDeletedStorageFiles immediately on start and then after interval`() = runTest {
        every { storageSettingsProvider.getDeletedFilesCleanupIntervalSeconds() } returns TEST_INTERVAL_SECONDS
        coEvery { storageManager.cleanupDeletedStorageFiles() } returns AppResult.Success(2)
        val jobs = createJobs(backgroundScope)

        jobs.start()

        advanceTimeBy(1.seconds)
        coVerify(exactly = 1) { storageManager.cleanupDeletedStorageFiles() }

        advanceTimeBy(TEST_INTERVAL_SECONDS.seconds)
        coVerify(exactly = 2) { storageManager.cleanupDeletedStorageFiles() }
    }

    @Test
    fun `should not launch second loops if already active`() = runTest {
        every { storageSettingsProvider.getDeletedFilesCleanupIntervalSeconds() } returns TEST_INTERVAL_SECONDS
        coEvery { storageManager.cleanupDeletedStorageFiles() } returns AppResult.Success(0)
        val jobs = createJobs(backgroundScope)

        jobs.start()
        jobs.start()

        advanceTimeBy(1.seconds)
        coVerify(exactly = 1) { storageManager.cleanupDeletedStorageFiles() }
    }

    @Test
    fun `should log error when manager returns failure result`() = runTest {
        val error = CommonError.Internal(Exception("Storage error"))
        every { storageSettingsProvider.getDeletedFilesCleanupIntervalSeconds() } returns TEST_INTERVAL_SECONDS
        coEvery { storageManager.cleanupDeletedStorageFiles() } returns AppResult.Error(error)
        val jobs = createJobs(backgroundScope)

        jobs.start()
        advanceTimeBy(1.seconds)

        verify(atLeast = 1) { appLogger.logError(error) }
    }

    @Test
    fun `should log internal error when exception is thrown during execution`() = runTest {
        val exception = RuntimeException("Unexpected")
        every { storageSettingsProvider.getDeletedFilesCleanupIntervalSeconds() } returns TEST_INTERVAL_SECONDS
        coEvery { storageManager.cleanupDeletedStorageFiles() } throws exception
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
        every { storageSettingsProvider.getDeletedFilesCleanupIntervalSeconds() } returns TEST_INTERVAL_SECONDS
        coEvery { storageManager.cleanupDeletedStorageFiles() } returns AppResult.Success(0)

        val testScope = CoroutineScope(coroutineContext + SupervisorJob())
        val jobs = createJobs(testScope)

        jobs.start()
        advanceTimeBy(1.seconds)
        coVerify(exactly = 1) { storageManager.cleanupDeletedStorageFiles() }

        testScope.cancel()
        advanceTimeBy(TEST_INTERVAL_SECONDS.seconds)

        coVerify(exactly = 1) { storageManager.cleanupDeletedStorageFiles() }
    }
}
