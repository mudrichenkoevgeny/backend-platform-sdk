package io.github.mudrichenkoevgeny.backend.core.audit.service

import io.github.mudrichenkoevgeny.backend.core.audit.manager.AuditManager
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEvent
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditStatus
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AuditServiceImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + testDispatcher)

    @Test
    fun `log schedules createEvent in background and returns immediately`() = runTest(testDispatcher) {
        val auditManager = mockk<AuditManager>(relaxed = true)
        coEvery { auditManager.createEvent(any()) } returns AppResult.Success(
            AuditEvent(action = "test", resource = "r", status = AuditStatus.SUCCESS)
        )
        val appLogger = mockk<AppLogger>(relaxed = true)
        val service = AuditServiceImpl(auditManager, scope, appLogger)

        val event = AuditEvent(action = "login", resource = "session", status = AuditStatus.SUCCESS)
        service.log(event)

        advanceUntilIdle()

        coVerify(exactly = 1) { auditManager.createEvent(event) }
    }

    @Test
    fun `log does not throw when createEvent returns error`() = runTest(testDispatcher) {
        val auditManager = mockk<AuditManager>(relaxed = true)
        coEvery { auditManager.createEvent(any()) } returns AppResult.Error(CommonError.Unknown("db error"))
        val appLogger = mockk<AppLogger>(relaxed = true)
        val service = AuditServiceImpl(auditManager, scope, appLogger)

        val event = AuditEvent(action = "action", resource = "resource", status = AuditStatus.FAILED)
        service.log(event)

        advanceUntilIdle()

        coVerify(exactly = 1) { auditManager.createEvent(event) }
    }

    @Test
    fun `awaitAll completes without blocking when no pending work`() = runTest(testDispatcher) {
        val auditManager = mockk<AuditManager>(relaxed = true)
        val appLogger = mockk<AppLogger>(relaxed = true)
        val service = AuditServiceImpl(auditManager, scope, appLogger)

        service.awaitAll()
    }

    @Test
    fun `awaitAll waits for pending log to complete`() = runTest(testDispatcher) {
        val auditManager = mockk<AuditManager>(relaxed = true)
        coEvery { auditManager.createEvent(any()) } coAnswers {
            kotlinx.coroutines.delay(50)
            AppResult.Success(AuditEvent(action = "a", resource = "r", status = AuditStatus.SUCCESS))
        }
        val appLogger = mockk<AppLogger>(relaxed = true)
        val service = AuditServiceImpl(auditManager, scope, appLogger)

        service.log(AuditEvent(action = "a", resource = "r", status = AuditStatus.SUCCESS))
        service.awaitAll()

        coVerify(exactly = 1) { auditManager.createEvent(any()) }
    }
}
