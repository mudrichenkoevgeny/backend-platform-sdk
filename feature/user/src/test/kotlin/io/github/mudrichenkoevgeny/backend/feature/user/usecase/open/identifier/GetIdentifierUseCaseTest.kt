package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.identifier.createTestUserIdentifierPrivate
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetIdentifierUseCaseTest {

    private val userManager = mockk<UserManager>()
    private val identifierManager = mockk<IdentifierManager>()

    private val useCase = GetIdentifierUseCase(
        userManager = userManager,
        identifierManager = identifierManager
    )

    private val context = createTestAuthenticatedRequestContext()
    private val userId = context.userId

    @Test
    fun `successfully returns identifier when user is active and identifier exists`() = runTest {
        val targetIdentifierId = UserIdentifierId.generate()
        val userInternal = createTestUserInternal(id = userId, accountStatus = UserAccountStatus.ACTIVE)
        val userIdentifier = createTestUserIdentifierPrivate(id = targetIdentifierId)

        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(userInternal)
        coEvery {
            identifierManager.getUserIdentifierPrivateById(targetIdentifierId)
        } returns AppResult.Success(userIdentifier)

        val result = useCase(targetIdentifierId, context)

        assertEquals(AppResult.Success(userIdentifier), result)
    }

    @Test
    fun `returns forbidden error when account status is not allowed`() = runTest {
        val targetIdentifierId = UserIdentifierId.generate()
        val userInternal = createTestUserInternal(id = userId, accountStatus = UserAccountStatus.BANNED)

        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(userInternal)

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserForbidden)
        assertEquals(userId, (error as UserError.UserForbidden).userId)
    }

    @Test
    fun `returns success when account status is READ_ONLY`() = runTest {
        val targetIdentifierId = UserIdentifierId.generate()
        val userInternal = createTestUserInternal(id = userId, accountStatus = UserAccountStatus.READ_ONLY)
        val userIdentifier = createTestUserIdentifierPrivate(id = targetIdentifierId)

        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(userInternal)
        coEvery {
            identifierManager.getUserIdentifierPrivateById(targetIdentifierId)
        } returns AppResult.Success(userIdentifier)

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `returns not found error when identifier does not exist`() = runTest {
        val targetIdentifierId = UserIdentifierId.generate()
        val userInternal = createTestUserInternal(id = userId, accountStatus = UserAccountStatus.ACTIVE)

        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(userInternal)
        coEvery {
            identifierManager.getUserIdentifierPrivateById(targetIdentifierId)
        } returns AppResult.Success(null)

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is CommonError.NotFound)
        assertEquals(targetIdentifierId.asHexDashString(), (error as CommonError.NotFound).identifier)
    }

    @Test
    fun `returns error when user manager fails`() = runTest {
        val targetIdentifierId = UserIdentifierId.generate()
        val error = UserError.UserNotFound(userId)

        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Error(error)

        val result = useCase(targetIdentifierId, context)

        assertEquals(AppResult.Error(error), result)
    }
}
