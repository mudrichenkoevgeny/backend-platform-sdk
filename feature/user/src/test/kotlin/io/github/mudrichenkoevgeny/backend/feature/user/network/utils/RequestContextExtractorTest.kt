package io.github.mudrichenkoevgeny.backend.feature.user.network.utils

import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.extractClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getJWTPrincipal
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getSessionId
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserId
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserRole
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonHttpHeaders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.toUserSessionIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.toUserIdOrThrow
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RequestContextUtilsTest {

    private val call = mockk<RoutingCall>()
    private val request = mockk<RoutingRequest>()
    private val principal = mockk<JWTPrincipal>()
    private val clientInfo = ClientInfo()

    @BeforeEach
    fun setUp() {
        every { call.request } returns request
        mockkStatic(JWT_EXTENSIONS_KT, CLIENT_INFO_KT)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getRequestContext should return context with nulls when no principal exists`() {
        every { request.headers[CommonHttpHeaders.TRACE_HEADER_NAME] } returns TEST_TRACE_ID
        every { call.getJWTPrincipal() } returns null
        every { call.extractClientInfo() } returns clientInfo

        val result = call.getRequestContext()

        assertEquals(TEST_TRACE_ID, result.traceId)
        assertNull(result.userId)
        assertNull(result.sessionId)
    }

    @Test
    fun `getAuthenticatedRequestContext should return full context when principal is valid`() {
        val expectedUserId = TEST_USER_ID.toUserIdOrThrow()
        val expectedSessionId = TEST_SESSION_ID.toUserSessionIdOrThrow()

        every { request.headers[CommonHttpHeaders.TRACE_HEADER_NAME] } returns TEST_TRACE_ID
        every { call.getJWTPrincipal() } returns principal
        every { principal.getUserId() } returns expectedUserId
        every { principal.getUserRole() } returns UserRole.USER
        every { principal.getSessionId() } returns expectedSessionId
        every { call.extractClientInfo() } returns clientInfo

        val result = call.getAuthenticatedRequestContext()

        assertEquals(TEST_TRACE_ID, result.traceId)
        assertEquals(expectedUserId, result.userId)
        assertEquals(UserRole.USER, result.userRole)
        assertEquals(expectedSessionId, result.sessionId)
    }

    @Test
    fun `getAuthenticatedRequestContext should throw exception when principal is missing`() {
        every { request.headers[CommonHttpHeaders.TRACE_HEADER_NAME] } returns TEST_TRACE_ID
        every { call.getJWTPrincipal() } returns null

        val exception = assertThrows<RequestHandlingException> {
            call.getAuthenticatedRequestContext()
        }

        assertTrue(exception.error is UserError.InvalidAccessToken)
    }

    @Test
    fun `getAuthenticatedRequestContext should throw exception when userId is missing in principal`() {
        every { request.headers[CommonHttpHeaders.TRACE_HEADER_NAME] } returns TEST_TRACE_ID
        every { call.getJWTPrincipal() } returns principal
        every { principal.getUserId() } returns null

        assertThrows<RequestHandlingException> {
            call.getAuthenticatedRequestContext()
        }
    }

    companion object {
        private const val TEST_TRACE_ID = "trace-123"
        private const val TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000"
        private const val TEST_SESSION_ID = "123e4567-e89b-12d3-a456-426614174000"
        private const val JWT_EXTENSIONS_KT = "io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.JwtExtensionsKt"
        private const val CLIENT_INFO_KT = "io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfoKt"
    }
}