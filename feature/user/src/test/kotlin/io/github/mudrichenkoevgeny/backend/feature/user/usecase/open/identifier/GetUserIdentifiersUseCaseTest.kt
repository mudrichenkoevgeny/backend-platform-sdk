package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GetUserIdentifiersUseCaseTest {

    private val userAuditLogger = mockk<UserAuditLogger>(relaxed = true)
    private val identifierManager = mockk<IdentifierManager>()

    private val useCase = GetUserIdentifiersUseCase(
        userAuditLogger = userAuditLogger,
        identifierManager = identifierManager
    )

    @Test
    fun `execute returns InvalidAccessToken when request context has no userId`() = runBlocking {
        val ctx = requestContext(userId = null)

        val result = useCase.execute(requestContext = ctx)

        Assertions.assertTrue(result is AppResult.Error)
        Assertions.assertTrue((result as AppResult.Error).error is UserError.InvalidAccessToken)
    }

    @Test
    fun `execute returns success with identifiers from manager`() = runBlocking {
        val userId = UserId.generate()
        val ctx = requestContext(userId = userId)
        val identifiers = emptyList<UserIdentifier>()
        coEvery { identifierManager.getUserIdentifiersByUserId(userId) } returns AppResult.Success(identifiers)

        val result = useCase.execute(requestContext = ctx)

        Assertions.assertTrue(result is AppResult.Success)
        Assertions.assertEquals(identifiers, (result as AppResult.Success).data)
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