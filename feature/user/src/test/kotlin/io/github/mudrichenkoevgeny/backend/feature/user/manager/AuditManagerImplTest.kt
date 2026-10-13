package io.github.mudrichenkoevgeny.backend.feature.user.manager

import io.github.mudrichenkoevgeny.backend.core.audit.database.repository.AuditEventRepository
import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.event.createTestAuditEventInternal
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.permissions.AuditPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditEventPrivate
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.ListingDefaultValues
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuditManagerImplTest {

    private val repository = mockk<AuditEventRepository>()
    private val auditManager = AuditManagerImpl(repository)
    private val userId = UserId.generate()

    @BeforeEach
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
    }

    @Test
    fun `getAuditEventPrivateById returns Success when user has UNMASKED permission`() = runTest {
        val event = createTestAuditEventInternal(actorType = AuditActorType.SYSTEM)
        coEvery { repository.getEventById(event.id) } returns AppResult.Success(event)

        val permissions = setOf(AuditPermissionCode.AUDIT_GET_FOR_SYSTEM_ACTOR_UNMASKED)

        val result = auditManager.getAuditEventPrivateById(event.id, userId, permissions)

        assertTrue(result is AppResult.Success)
        assertEquals(event.toAuditEventPrivate(), (result as AppResult.Success).data)
    }

    @Test
    fun `getAuditEventPrivateById returns Error UserMissingPermissions when user has no permissions`() = runTest {
        val event = createTestAuditEventInternal(actorType = AuditActorType.SYSTEM)
        coEvery { repository.getEventById(event.id) } returns AppResult.Success(event)

        val result = auditManager.getAuditEventPrivateById(event.id, userId, emptySet())

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserMissingPermissions)
    }

    @Test
    fun `getAuditEventsSummaryPage filters out forbidden events from the list`() = runTest {
        val eventSystem = createTestAuditEventInternal(actorType = AuditActorType.SYSTEM)
        val eventService = createTestAuditEventInternal(actorType = AuditActorType.SERVICE)

        val pagedResult = PagedResult(
            items = listOf(eventSystem, eventService),
            totalCount = 2,
            pageNumber = ListingDefaultValues.Pagination.PAGE_NUMBER,
            pageSize = ListingDefaultValues.Pagination.PAGE_SIZE,
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

        val result = auditManager.getAuditEventsSummaryPage(
            managementUserPermissionCodes = permissions,
            pageParams = PageParams(),
            sortBy = AuditSortValues.AuditEventSortBy.DEFAULT,
            sortOrder = SortOrder.DEFAULT
        )

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(1, data.items.size)
        assertEquals(AuditActorType.SYSTEM, data.items.first().actorType)
    }
}
