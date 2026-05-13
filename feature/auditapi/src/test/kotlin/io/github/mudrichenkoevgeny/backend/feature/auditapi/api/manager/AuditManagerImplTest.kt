package io.github.mudrichenkoevgeny.backend.feature.auditapi.api.manager

import io.github.mudrichenkoevgeny.backend.core.audit.database.repository.AuditEventRepository
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.auditapi.domain.permissions.AuditPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class AuditManagerImplTest {

    private val repository = mockk<AuditEventRepository>()
    private val auditManager = AuditManagerImpl(repository)
    private val userId = UserId.generate()

    @BeforeEach
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
    }

    private fun createSampleEvent(
        actorType: AuditActorType = AuditActorType.SYSTEM,
        actorUserRole: String? = null
    ) = AuditEvent(
        id = AuditEventId.generate(),
        actorId = null,
        actorType = actorType,
        actorUserRole = actorUserRole,
        action = object : AuditActionType {
            override val serialName: String = "test_action"
            override fun parseOrNull(value: String): AuditActionType? = null
            override fun parseOrThrow(value: String): AuditActionType = throw UnsupportedOperationException()
        },
        resource = object : AuditResourceType {
            override val serialName: String = "test_resource"
            override fun parseOrNull(value: String): AuditResourceType? = null
            override fun parseOrThrow(value: String): AuditResourceType = throw UnsupportedOperationException()
        },
        resourceId = null,
        status = AuditStatus.SUCCESS,
        metadata = emptySet(),
        message = null,
        createdAt = Clock.System.now()
    )

    @Test
    fun `getEventById returns Success when user has UNMASKED permission`() = runTest {
        val event = createSampleEvent(AuditActorType.SYSTEM)
        coEvery { repository.getEventById(event.id) } returns AppResult.Success(event)

        val permissions = setOf(AuditPermissionCode.AUDIT_GET_FOR_SYSTEM_ACTOR_UNMASKED)

        val result = auditManager.getEventById(event.id, userId, permissions)

        assertTrue(result is AppResult.Success)
        assertEquals(event, (result as AppResult.Success).data)
    }

    @Test
    fun `getEventById returns Error UserMissingPermissions when user has no permissions`() = runTest {
        val event = createSampleEvent(AuditActorType.SYSTEM)
        coEvery { repository.getEventById(event.id) } returns AppResult.Success(event)

        val result = auditManager.getEventById(event.id, userId, emptySet())

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserMissingPermissions)
    }

    @Test
    fun `getEventsPage filters out forbidden events from the list`() = runTest {
        val eventSystem = createSampleEvent(AuditActorType.SYSTEM)
        val eventService = createSampleEvent(AuditActorType.SERVICE)

        val pagedResult = PagedResult(
            items = listOf(eventSystem, eventService),
            totalCount = 2,
            pageNumber = 1,
            pageSize = 10,
            totalPages = 1
        )

        coEvery {
            repository.getEventsPageWithAccessFilter(
                accessFilter = any(),
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
        } returns AppResult.Success(pagedResult)

        val permissions = setOf(AuditPermissionCode.AUDIT_GET_FOR_SYSTEM_ACTOR_UNMASKED)

        val result = auditManager.getEventsPage(
            managementUserPermissionCodes = permissions,
            pageParams = PageParams(1, 10),
            sortBy = AuditSortValues.AuditEventSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC
        )

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(1, data.items.size)
        assertEquals(AuditActorType.SYSTEM, data.items.first().actorType)
    }
}