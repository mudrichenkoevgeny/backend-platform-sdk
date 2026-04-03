package io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter

import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.model.UserDeviceId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model.RateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class RateLimitEnforcerImplTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditService = mockk<AuditService>(relaxed = true)
    private val enforcer = RateLimitEnforcerImpl(rateLimiter, auditService)

    @Test
    fun `enforce returns success when allowed and does not write audit`() = runTest {
        val requestContext = testRequestContext()
        coEvery { rateLimiter.isRateLimited(RateLimitAction.LOGIN_ATTEMPT, "ip:1") } returns
            AppResult.Success(RateLimitResult.Allowed)

        val result = enforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = RateLimitAction.LOGIN_ATTEMPT,
            rateLimitIdentifier = "ip:1",
            auditAction = "login",
            auditResource = "session",
            auditResourceId = "abc"
        )

        assertEquals(AppResult.Success(Unit), result)
        verify(exactly = 0) { auditService.log(any()) }
    }

    @Test
    fun `enforce writes denied audit and returns too many requests error when exceeded`() = runTest {
        val requestContext = testRequestContext()
        val error = CommonError.TooManyRequests(
            rateLimitActionCode = RateLimitAction.LOGIN_ATTEMPT.id,
            limit = RateLimitAction.LOGIN_ATTEMPT.limit,
            identifier = RateLimitAction.LOGIN_ATTEMPT.createKey("ip:2"),
            retryAfterSeconds = 10
        )

        coEvery { rateLimiter.isRateLimited(RateLimitAction.LOGIN_ATTEMPT, "ip:2") } returns
            AppResult.Success(RateLimitResult.Exceeded(error))

        val auditEventSlot = slot<AuditEvent>()
        every { auditService.log(capture(auditEventSlot)) } returns Unit

        val result = enforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = RateLimitAction.LOGIN_ATTEMPT,
            rateLimitIdentifier = "ip:2",
            auditAction = "login",
            auditResource = "session",
            auditResourceId = null
        )

        val appError = (result as AppResult.Error).error
        assertSame(error, appError)

        val auditEvent = auditEventSlot.captured
        assertEquals(requestContext.userId!!, auditEvent.actorId)
        assertEquals("login", auditEvent.action.serialName)
        assertEquals("session", auditEvent.resource.serialName)
        assertEquals("unknown", auditEvent.resourceId)
        assertEquals(AuditStatus.DENIED, auditEvent.status)

        val metadata = auditEvent.metadata
        assertEquals("127.0.0.1", metadata.single { it.key == RateLimitAuditMetadata.Keys.IP_ADDRESS }.value)
        assertEquals("device-1", metadata.single { it.key == RateLimitAuditMetadata.Keys.DEVICE_ID }.value)
        assertEquals("ua", metadata.single { it.key == RateLimitAuditMetadata.Keys.USER_AGENT }.value)
        assertEquals(
            RateLimitAuditMetadata.Reasons.RATE_LIMIT,
            metadata.single { it.key == RateLimitAuditMetadata.Keys.REASON }.value
        )
        assertTrue(metadata.none { it.key == RateLimitAuditMetadata.Keys.CLIENT_TYPE }, "client_type should be absent when null")
    }

    @Test
    fun `enforce propagates dependency error and does not write audit`() = runTest {
        val requestContext = testRequestContext()
        val dependencyError = CommonError.Internal(Throwable("redis down"))

        coEvery { rateLimiter.isRateLimited(RateLimitAction.LOGIN_ATTEMPT, "ip:3") } returns
            AppResult.Error(dependencyError)

        val result = enforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = RateLimitAction.LOGIN_ATTEMPT,
            rateLimitIdentifier = "ip:3",
            auditAction = "login",
            auditResource = "session",
            auditResourceId = "x"
        )

        val appError = (result as AppResult.Error).error
        assertSame(dependencyError, appError)
        verify(exactly = 0) { auditService.log(any()) }
    }

    private fun testRequestContext(): RequestContext {
        return RequestContext(
            traceId = "trace",
            userId = Uuid.random().toString(),
            sessionId = null,
            clientInfo = ClientInfo(
                clientType = null,
                userAgent = "ua",
                ipAddress = "127.0.0.1",
                language = null,
                host = null,
                origin = null,
                deviceId = UserDeviceId("device-1"),
                deviceName = null,
                appVersion = null,
                operationSystemVersion = null
            )
        )
    }
}

