package io.github.mudrichenkoevgeny.backend.feature.user.usecase.user

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class GetUserUseCaseTest {

    private val userAuditLogger = mockk<UserAuditLogger>(relaxed = true)
    private val userManager = mockk<UserManager>()

    private val useCase = GetUserUseCase(
        userAuditLogger = userAuditLogger,
        userManager = userManager
    )

    @Test
    fun `execute returns InvalidAccessToken when request context has no userId`() = runBlocking {
        val ctx = requestContext(userId = null)

        val result = useCase.execute(userId = UserId.generate(), requestContext = ctx)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidAccessToken)
    }

    @Test
    fun `execute returns InvalidAccessToken when userId does not match current user`() = runBlocking {
        val currentUserId = UserId.generate()
        val otherUserId = UserId.generate()
        val ctx = requestContext(userId = currentUserId)

        val result = useCase.execute(userId = otherUserId, requestContext = ctx)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidAccessToken)
    }

    @Test
    fun `execute returns UserNotFound when user does not exist`() = runBlocking {
        val userId = UserId.generate()
        val ctx = requestContext(userId = userId)
        coEvery { userManager.getUserById(userId) } returns AppResult.Success(null)

        val result = useCase.execute(userId = userId, requestContext = ctx)

        assertTrue(result is AppResult.Error)
        val err = (result as AppResult.Error).error
        assertTrue(err is UserError.UserNotFound)
        assertEquals(userId, (err as UserError.UserNotFound).userId)
    }

    @Test
    fun `execute returns success with user when user exists`() = runBlocking {
        val userId = UserId.generate()
        val ctx = requestContext(userId = userId)
        val user = user(userId)
        coEvery { userManager.getUserById(userId) } returns AppResult.Success(user)

        val result = useCase.execute(userId = userId, requestContext = ctx)

        assertTrue(result is AppResult.Success)
        assertEquals(user, (result as AppResult.Success).data)
    }

    private fun requestContext(userId: UserId?) = RequestContext(
        traceId = null,
        userId = userId,
        sessionId = null,
        clientInfo = CLIENT_INFO
    )

    private fun user(id: UserId) = User(
        id = id,
        role = UserRole.USER,
        accountStatus = UserAccountStatus.ACTIVE,
        lastLoginAt = null,
        lastActiveAt = null,
        createdAt = Instant.now(),
        updatedAt = null
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
