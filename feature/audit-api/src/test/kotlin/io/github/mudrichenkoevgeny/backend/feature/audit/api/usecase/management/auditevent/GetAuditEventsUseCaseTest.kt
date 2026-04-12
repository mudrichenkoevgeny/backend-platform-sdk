package io.github.mudrichenkoevgeny.backend.feature.audit.api.usecase.management.auditevent

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.audit.api.manager.AuditManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
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

    private companion object {
        private const val IP_ADDRESS = "127.0.0.1"
    }

    private val pageParams = PageParams(page = 1, size = 10)

    private fun clientInfo(): ClientInfo = ClientInfo(
        deviceInfo = ClientDeviceInfo(
            deviceId = null,
            deviceName = null,
            clientType = null,
            language = null,
            appVersion = null,
            operationSystemVersion = null
        ),
        userAgent = null,
        ipAddress = IP_ADDRESS,
        host = null,
        origin = null,
        apiVersion = null
    )

    private fun requestContext(userId: UserId?): RequestContext = RequestContext(
        traceId = null,
        userId = userId,
        userRole = null,
        sessionId = null,
        clientInfo = clientInfo()
    )

    private fun userDetails(id: UserId): UserDetails {
        val now = Clock.System.now()
        return UserDetails(
            id = id,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            accountStatusBeforeDeletion = UserAccountStatus.ACTIVE,
            permissions = emptySet(),
            lastLoginAt = now,
            lastActiveAt = now,
            createdAt = now,
            updatedAt = null,
            scheduledPermanentDeletionAt = null
        )
    }

    @Test
    fun `returns UserForbidden when request has no userId`() = runTest {
        val useCase = GetAuditEventsUseCase(mockk(), mockk(relaxed = true))

        val result = useCase(pageParams, requestContext = requestContext(userId = null))

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserForbidden)
    }

    @Test
    fun `returns UserForbidden when current user is not found`() = runTest {
        val userId = UserId.generate()
        val userManager = mockk<UserManager>()
        coEvery { userManager.getUserById(userId) } returns AppResult.Success(null)
        val useCase = GetAuditEventsUseCase(userManager, mockk(relaxed = true))

        val result = useCase(pageParams, requestContext = requestContext(userId))

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserForbidden)
    }

    @Test
    fun `propagates error when getUserById fails`() = runTest {
        val userId = UserId.generate()
        val err = CommonError.Internal(Throwable("db"))
        val userManager = mockk<UserManager>()
        coEvery { userManager.getUserById(userId) } returns AppResult.Error(err)
        val useCase = GetAuditEventsUseCase(userManager, mockk(relaxed = true))

        val result = useCase(pageParams, requestContext = requestContext(userId))

        assertEquals(AppResult.Error(err), result)
    }

    @Test
    fun `delegates to audit manager with user permissions and returns result`() = runTest {
        val userId = UserId.generate()
        val details = userDetails(userId)
        val paged = PagedResult<AuditEvent>(
            items = emptyList(),
            totalCount = 0L,
            pageNumber = 1,
            pageSize = 10,
            totalPages = 0L
        )
        val userManager = mockk<UserManager>()
        val auditManager = mockk<AuditManager>()
        coEvery { userManager.getUserById(userId) } returns AppResult.Success(details)
        coEvery {
            auditManager.getEventsList(
                userPermissionCodes = details.permissions,
                pageParams = pageParams,
                sortBy = AuditSortValues.AuditEventSortBy.CREATED_AT,
                sortOrder = SortOrder.DESC,
                actorId = null,
                actorType = null,
                actorUserRole = null,
                action = null,
                resource = null,
                resourceId = null,
                status = null,
                message = null
            )
        } returns AppResult.Success(paged)

        val useCase = GetAuditEventsUseCase(userManager, auditManager)
        val result = useCase(pageParams, requestContext = requestContext(userId))

        assertEquals(AppResult.Success(paged), result)
        coVerify(exactly = 1) {
            auditManager.getEventsList(
                userPermissionCodes = details.permissions,
                pageParams = pageParams,
                sortBy = AuditSortValues.AuditEventSortBy.CREATED_AT,
                sortOrder = SortOrder.DESC,
                actorId = null,
                actorType = null,
                actorUserRole = null,
                action = null,
                resource = null,
                resourceId = null,
                status = null,
                message = null
            )
        }
    }

    @Test
    fun `passes list filters through to audit manager`() = runTest {
        val userId = UserId.generate()
        val details = userDetails(userId)
        val userManager = mockk<UserManager>()
        val auditManager = mockk<AuditManager>()
        coEvery { userManager.getUserById(userId) } returns AppResult.Success(details)
        coEvery {
            auditManager.getEventsList(
                userPermissionCodes = details.permissions,
                pageParams = pageParams,
                sortBy = AuditSortValues.AuditEventSortBy.CREATED_AT,
                sortOrder = SortOrder.ASC,
                actorId = "actor-1",
                actorType = null,
                actorUserRole = null,
                action = null,
                resource = null,
                resourceId = null,
                status = null,
                message = null
            )
        } returns AppResult.Success(
            PagedResult(
                items = emptyList(),
                totalCount = 0L,
                pageNumber = 1,
                pageSize = 10,
                totalPages = 0L
            )
        )

        val useCase = GetAuditEventsUseCase(userManager, auditManager)
        useCase(
            pageParams = pageParams,
            sortOrder = SortOrder.ASC,
            actorId = "actor-1",
            requestContext = requestContext(userId)
        )

        coVerify(exactly = 1) {
            auditManager.getEventsList(
                userPermissionCodes = details.permissions,
                pageParams = pageParams,
                sortBy = AuditSortValues.AuditEventSortBy.CREATED_AT,
                sortOrder = SortOrder.ASC,
                actorId = "actor-1",
                actorType = null,
                actorUserRole = null,
                action = null,
                resource = null,
                resourceId = null,
                status = null,
                message = null
            )
        }
    }
}
