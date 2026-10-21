package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auditevent

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.AuditManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventSummary
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetAuditEventsUseCaseTest {

    private val pageParams = PageParams()

    @Test
    fun `returns UserForbidden when current user is not found`() = runTest {
        val userId = UserId.generate()
        val userManager = mockk<UserManager>()
        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(null)
        val useCase = GetAuditEventsUseCase(userManager, mockk(relaxed = true))

        val result = useCase(pageParams, authenticatedRequestContext = createTestAuthenticatedRequestContext(userId = userId))

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserForbidden)
    }

    @Test
    fun `propagates error when getUserById fails`() = runTest {
        val userId = UserId.generate()
        val err = CommonError.Internal(Throwable("db"))
        val userManager = mockk<UserManager>()
        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Error(err)
        val useCase = GetAuditEventsUseCase(userManager, mockk(relaxed = true))

        val result = useCase(pageParams, authenticatedRequestContext = createTestAuthenticatedRequestContext(userId = userId))

        assertEquals(AppResult.Error(err), result)
    }

    @Test
    fun `delegates to audit manager with user permissions and returns result`() = runTest {
        val userId = UserId.generate()
        val userInternal = createTestUserInternal(id = userId, role = UserRole.ADMIN)
        val paged = PagedResult<AuditEventSummary>(emptyList(), 0L, 1, 10, 0L)
        val userManager = mockk<UserManager>()
        val auditManager = mockk<AuditManager>()

        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(userInternal)
        coEvery {
            auditManager.getAuditEventsSummaryPage(
                managementUserPermissionCodes = userInternal.permissionCodes,
                pageParams = any(),
                sortBy = any(),
                sortOrder = any(),
                actorIds = any(),
                actorTypes = any(),
                actorUserRoles = any(),
                actions = any(),
                resources = any(),
                resourceIds = any(),
                statuses = any(),
                messages = any()
            )
        } returns AppResult.Success(paged)

        val useCase = GetAuditEventsUseCase(userManager, auditManager)
        val result = useCase(pageParams, authenticatedRequestContext = createTestAuthenticatedRequestContext(userId = userId))

        assertEquals(AppResult.Success(paged), result)
        coVerify(exactly = 1) {
            auditManager.getAuditEventsSummaryPage(
                managementUserPermissionCodes = userInternal.permissionCodes,
                pageParams = pageParams,
                sortBy = AuditSortValues.AuditEventSortBy.DEFAULT,
                sortOrder = SortOrder.DEFAULT,
                actorIds = emptyList(),
                actorTypes = emptyList(),
                actorUserRoles = emptyList(),
                actions = emptyList(),
                resources = emptyList(),
                resourceIds = emptyList(),
                statuses = emptyList(),
                messages = emptyList()
            )
        }
    }
}
