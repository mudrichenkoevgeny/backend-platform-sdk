package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetIdentifiersUseCaseTest {

    private val userManager = mockk<UserManager>()
    private val identifierManager = mockk<IdentifierManager>()

    private val useCase = GetIdentifiersUseCase(
        userManager = userManager,
        identifierManager = identifierManager
    )

    private fun createAuthContext() = AuthenticatedRequestContext(
        traceId = null,
        userId = UserId.generate(),
        userRole = UserRole.USER,
        sessionId = UserSessionId.generate(),
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully returns paginated identifiers`() = runTest {
        val context = createAuthContext()
        val userDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
        }
        val pagedResult = mockk<PagedResult<UserIdentifier>>()

        coEvery {
            userManager.getUserByIdForSelf(context.userId)
        } returns AppResult.Success(userDetails)

        coEvery {
            identifierManager.getIdentifiersPageForSelf(
                userId = context.userId,
                pageParams = any(),
                sortBy = any(),
                sortOrder = any(),
                userAuthProviders = any(),
                identifiers = any()
            )
        } returns AppResult.Success(pagedResult)

        val result = useCase(
            authenticatedRequestContext = context,
            pageParams = PageParams(page = 1, size = 10),
            sortBy = UserSortValues.UserIdentifierSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
            userAuthProviders = emptyList(),
            identifiers = emptyList()
        )

        assertEquals(AppResult.Success(pagedResult), result)
    }

    @Test
    fun `returns error when account status is not allowed`() = runTest {
        val context = createAuthContext()
        val userDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.PENDING_DELETION
            every { id } returns context.userId
        }

        coEvery { userManager.getUserByIdForSelf(context.userId) } returns AppResult.Success(userDetails)

        val result = useCase(
            authenticatedRequestContext = context,
            pageParams = PageParams(0, 10),
            sortBy = UserSortValues.UserIdentifierSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
            userAuthProviders = emptyList(),
            identifiers = emptyList()
        )

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserForbidden)
        assertEquals(context.userId, (error as UserError.UserForbidden).userId)
    }

    @Test
    fun `returns success when account status is READ_ONLY`() = runTest {
        val context = createAuthContext()
        val userDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.READ_ONLY
        }
        val pagedResult = mockk<PagedResult<UserIdentifier>>()

        coEvery { userManager.getUserByIdForSelf(context.userId) } returns AppResult.Success(userDetails)
        coEvery {
            identifierManager.getIdentifiersPageForSelf(any(), any(), any(), any(), any(), any())
        } returns AppResult.Success(pagedResult)

        val result = useCase(
            authenticatedRequestContext = context,
            pageParams = PageParams(0, 10),
            sortBy = UserSortValues.UserIdentifierSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
            userAuthProviders = emptyList(),
            identifiers = emptyList()
        )

        assertTrue(result is AppResult.Success)
    }
}