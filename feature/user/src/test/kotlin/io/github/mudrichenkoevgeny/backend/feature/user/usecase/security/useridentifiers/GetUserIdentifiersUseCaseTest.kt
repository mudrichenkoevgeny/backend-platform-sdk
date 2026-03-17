package io.github.mudrichenkoevgeny.backend.feature.user.usecase.security.useridentifiers

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetUserIdentifiersUseCaseTest {

    private val userAuditLogger = mockk<UserAuditLogger>(relaxed = true)
    private val userIdentifierManager = mockk<UserIdentifierManager>()

    private val useCase = GetUserIdentifiersUseCase(
        userAuditLogger = userAuditLogger,
        userIdentifierManager = userIdentifierManager
    )

    @Test
    fun `execute returns InvalidAccessToken when request context has no userId`() = runBlocking {
        val ctx = requestContext(userId = null)

        val result = useCase.execute(requestContext = ctx)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidAccessToken)
    }

    @Test
    fun `execute returns success with identifiers from manager`() = runBlocking {
        val userId = UserId.generate()
        val ctx = requestContext(userId = userId)
        val identifiers = emptyList<UserIdentifier>()
        coEvery { userIdentifierManager.getUserIdentifierListByUserId(userId) } returns AppResult.Success(identifiers)

        val result = useCase.execute(requestContext = ctx)

        assertTrue(result is AppResult.Success)
        assertEquals(identifiers, (result as AppResult.Success).data)
    }

    private fun requestContext(userId: UserId?) = RequestContext(
        traceId = null,
        userId = userId,
        sessionId = null,
        clientInfo = CLIENT_INFO
    )

    private companion object {
        val CLIENT_INFO = ClientInfo(
            clientType = null,
            userAgent = null,
            ipAddress = null,
            language = null,
            host = null,
            origin = null,
            deviceId = null,
            deviceName = null,
            appVersion = null,
            operationSystemVersion = null
        )
    }
}
