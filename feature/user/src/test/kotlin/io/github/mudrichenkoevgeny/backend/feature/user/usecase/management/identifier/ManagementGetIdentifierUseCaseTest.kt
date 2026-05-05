package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManagementGetIdentifierUseCaseTest {

    private val userManager = mockk<UserManager>()
    private val identifierManager = mockk<IdentifierManager>()

    private val useCase = ManagementGetIdentifierUseCase(
        userManager = userManager,
        identifierManager = identifierManager
    )

    private fun createAuthContext(userId: UserId = UserId.generate()) = AuthenticatedRequestContext(
        traceId = null,
        userId = userId,
        userRole = UserRole.ADMIN,
        sessionId = UserSessionId.generate(),
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully retrieves identifier for management`() = runTest {
        val managerId = UserId.generate()
        val context = createAuthContext(managerId)
        val targetIdentifierId = UserIdentifierId.generate()
        val permissions = setOf(PermissionCode("management.view"))

        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { permissionCodes } returns permissions
        }

        val identifier = mockk<UserIdentifier>()

        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery {
            identifierManager.getUserIdentifierByIdForManagement(
                userIdentifierId = targetIdentifierId,
                managementUserId = managerId,
                managementUserPermissionCodes = permissions
            )
        } returns AppResult.Success(identifier)

        val result = useCase(targetIdentifierId, context)

        assertEquals(AppResult.Success(identifier), result)
    }

    @Test
    fun `returns error when management user status is illegal`() = runTest {
        val managerId = UserId.generate()
        val context = createAuthContext(managerId)
        val targetIdentifierId = UserIdentifierId.generate()

        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.BANNED
        }

        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserIllegalAccountStatus)
        coVerify(exactly = 0) { identifierManager.getUserIdentifierByIdForManagement(any(), any(), any()) }
    }

    @Test
    fun `returns error when management user not found`() = runTest {
        val managerId = UserId.generate()
        val context = createAuthContext(managerId)
        val targetIdentifierId = UserIdentifierId.generate()

        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(null)

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }

    @Test
    fun `returns error when identifier is missing`() = runTest {
        val managerId = UserId.generate()
        val context = createAuthContext(managerId)
        val targetIdentifierId = UserIdentifierId.generate()
        val permissions = setOf(PermissionCode("management.view"))

        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { permissionCodes } returns permissions
        }

        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery {
            identifierManager.getUserIdentifierByIdForManagement(any(), any(), any())
        } returns AppResult.Success(null)

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error && result.error is CommonError.NotFound)
    }
}