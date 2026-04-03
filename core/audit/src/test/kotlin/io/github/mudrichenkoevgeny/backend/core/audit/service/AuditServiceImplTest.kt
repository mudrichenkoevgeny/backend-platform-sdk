package io.github.mudrichenkoevgeny.backend.core.audit.service

import io.github.mudrichenkoevgeny.backend.core.audit.domain.wire.AuditWireAction
import io.github.mudrichenkoevgeny.backend.core.audit.domain.wire.AuditWireResource
import io.github.mudrichenkoevgeny.backend.core.audit.manager.AuditManager
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class AuditServiceImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + testDispatcher)

    @Test
    fun `log schedules createEvent in background and returns immediately`() = runTest(testDispatcher) {
        val auditManager = mockk<AuditManager>(relaxed = true)
        coEvery { auditManager.createEvent(any()) } returns AppResult.Success(
            sampleAuditEvent(action = "test", resource = "r")
        )
        val appLogger = mockk<AppLogger>(relaxed = true)
        val service = AuditServiceImpl(auditManager, scope, appLogger)

        val event = sampleAuditEvent(action = "login", resource = "session")
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

        val event = sampleAuditEvent(
            action = "action",
            resource = "resource",
            status = AuditStatus.FAILED
        )
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
            AppResult.Success(sampleAuditEvent(action = "a", resource = "r"))
        }
        val appLogger = mockk<AppLogger>(relaxed = true)
        val service = AuditServiceImpl(auditManager, scope, appLogger)

        service.log(sampleAuditEvent(action = "a", resource = "r"))
        service.awaitAll()

        coVerify(exactly = 1) { auditManager.createEvent(any()) }
    }

    private fun sampleAuditEvent(
        action: String,
        resource: String,
        status: AuditStatus = AuditStatus.SUCCESS,
    ): AuditEvent = AuditEvent(
        actorType = AuditActorType.SYSTEM,
        action = AuditWireAction(action),
        resource = AuditWireResource(resource),
        status = status,
        createdAt = Clock.System.now()
    )
}
