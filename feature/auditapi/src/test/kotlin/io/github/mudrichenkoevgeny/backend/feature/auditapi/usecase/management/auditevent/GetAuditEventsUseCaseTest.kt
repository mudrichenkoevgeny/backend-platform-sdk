package io.github.mudrichenkoevgeny.backend.feature.auditapi.usecase.management.auditevent

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.auditapi.manager.AuditManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class GetAuditEventsUseCaseTest {

    private val pageParams = PageParams(page = 1, size = 10)

    private fun clientInfo(): ClientInfo = ClientInfo(
        deviceInfo = ClientDeviceInfo(null, null, null, null, null, null),
        userAgent = null,
        ipAddress = "127.0.0.1",
        host = null,
        origin = null,
        apiVersion = null
    )

    private fun authContext(userId: UserId): AuthenticatedRequestContext = AuthenticatedRequestContext(
        traceId = null,
        userId = userId,
        userRole = UserRole.ADMIN,
        sessionId = UserSessionId.generate(),
        clientInfo = clientInfo()
    )

    private fun userDetails(id: UserId): UserDetails {
        val now = Clock.System.now()
        return UserDetails(
            id = id,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            accountStatusBeforeDeletion = UserAccountStatus.ACTIVE,
            authorityLevel = 1,
            permissionCodes = emptySet(),
            isTotpEnabled = false,
            lastLoginAt = now,
            lastActiveAt = now,
            createdAt = now
        )
    }

    @Test
    fun `returns UserForbidden when current user is not found`() = runTest {
        val userId = UserId.generate()
        val userManager = mockk<UserManager>()
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(null)
        val useCase = GetAuditEventsUseCase(userManager, mockk(relaxed = true))

        val result = useCase(pageParams, authenticatedRequestContext = authContext(userId))

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserForbidden)
    }

    @Test
    fun `propagates error when getUserById fails`() = runTest {
        val userId = UserId.generate()
        val err = CommonError.Internal(Throwable("db"))
        val userManager = mockk<UserManager>()
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Error(err)
        val useCase = GetAuditEventsUseCase(userManager, mockk(relaxed = true))

        val result = useCase(pageParams, authenticatedRequestContext = authContext(userId))

        assertEquals(AppResult.Error(err), result)
    }

    @Test
    fun `delegates to audit manager with user permissions and returns result`() = runTest {
        val userId = UserId.generate()
        val details = userDetails(userId)
        val paged = PagedResult<AuditEvent>(emptyList(), 0L, 1, 10, 0L)
        val userManager = mockk<UserManager>()
        val auditManager = mockk<AuditManager>()

        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(details)
        coEvery {
            auditManager.getEventsPage(
                managementUserPermissionCodes = details.permissionCodes,
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
        val result = useCase(pageParams, authenticatedRequestContext = authContext(userId))

        assertEquals(AppResult.Success(paged), result)
        coVerify(exactly = 1) {
            auditManager.getEventsPage(
                managementUserPermissionCodes = details.permissionCodes,
                pageParams = pageParams,
                sortBy = AuditSortValues.AuditEventSortBy.CREATED_AT,
                sortOrder = SortOrder.DESC,
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