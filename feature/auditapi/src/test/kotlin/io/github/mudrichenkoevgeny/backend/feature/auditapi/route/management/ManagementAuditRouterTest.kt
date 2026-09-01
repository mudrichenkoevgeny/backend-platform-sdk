package io.github.mudrichenkoevgeny.backend.feature.auditapi.route.management

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.auditapi.usecase.management.auditevent.GetAuditEventUseCase
import io.github.mudrichenkoevgeny.backend.feature.auditapi.usecase.management.auditevent.GetAuditEventsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.audit.createTestAuditEvent
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.createTestUserDetails
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.network.application.setupManagementTestEnvironment
import io.github.mudrichenkoevgeny.backend.feature.user.network.route.BaseRouterTest
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.CompositeAuditActionTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.CompositeAuditResourceTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.clearMocks
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ManagementAuditRouterTest : BaseRouterTest() {

    private val actionParser = mockk<CompositeAuditActionTypeParser>(relaxed = true)
    private val resourceParser = mockk<CompositeAuditResourceTypeParser>(relaxed = true)
    private val getAuditEventsUseCase = mockk<GetAuditEventsUseCase>(relaxed = true)
    private val getAuditEventUseCase = mockk<GetAuditEventUseCase>(relaxed = true)

    private val router = ManagementAuditRouter(
        authenticationProvider = authProvider,
        appLogger = appLogger,
        appErrorParser = appErrorParser,
        compositeAuditActionTypeParser = actionParser,
        compositeAuditResourceTypeParser = resourceParser,
        getAuditEventsUseCase = getAuditEventsUseCase,
        getAuditEventUseCase = getAuditEventUseCase
    )

    @BeforeEach
    fun setUp() {
        clearMocks(getAuditEventsUseCase, getAuditEventUseCase)
    }

    @Test
    fun `get audit event - success when admin`() = testApplication {
        val token = setupManagementTestEnvironment(router)
        authProvider.shouldReturnSuccess(createTestUserDetails(role = UserRole.ADMIN))

        val event = createTestAuditEvent()
        coEvery { getAuditEventUseCase(any(), any()) } returns AppResult.Success(event)

        val response = client.get("/management/audit/events/${event.id.value}") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `get audit event - forbidden when user role`() = testApplication {
        val token = setupManagementTestEnvironment(router)
        authProvider.shouldReturnSuccess(createTestUserDetails(role = UserRole.USER))

        coEvery {
            getAuditEventUseCase(any(), any())
        } returns AppResult.Error(UserError.UserForbidden())

        val response = client.get("/management/audit/events/${AuditEventId.generate().value}") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `get audit event - not found when usecase returns error`() = testApplication {
        val token = setupManagementTestEnvironment(router)
        authProvider.shouldReturnSuccess(createTestUserDetails(role = UserRole.STAFF))

        coEvery {
            getAuditEventUseCase(any(), any())
        } returns AppResult.Error(UserError.UserNotFound())

        val response = client.get("/management/audit/events/${AuditEventId.generate().value}") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `get audit events - success with items`() = testApplication {
        val token = setupManagementTestEnvironment(router)
        authProvider.shouldReturnSuccess(createTestUserDetails(role = UserRole.STAFF))

        val pagedResult = PagedResult(
            items = listOf(createTestAuditEvent(), createTestAuditEvent()),
            totalCount = 2,
            pageNumber = 1,
            pageSize = 20,
            totalPages = 1
        )

        coEvery {
            getAuditEventsUseCase(
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
                messages = any(),
                authenticatedRequestContext = any()
            )
        } returns AppResult.Success(pagedResult)

        val response = client.get("/management/audit/events") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `get audit events - success with empty list`() = testApplication {
        val token = setupManagementTestEnvironment(router)
        authProvider.shouldReturnSuccess(createTestUserDetails(role = UserRole.ADMIN))

        val emptyPagedResult = PagedResult(
            items = emptyList<AuditEvent>(),
            totalCount = 0,
            pageNumber = 1,
            pageSize = 10,
            totalPages = 0
        )

        coEvery {
            getAuditEventsUseCase(
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any()
            )
        } returns AppResult.Success(emptyPagedResult)

        val response = client.get("/management/audit/events") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `get audit events - unauthorized when auth provider returns error`() = testApplication {
        val token = setupManagementTestEnvironment(router)
        authProvider.shouldReturnError(AppResult.Error(UserError.InvalidAccessToken()))

        val response = client.get("/management/audit/events") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}