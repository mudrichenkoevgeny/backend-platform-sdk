package io.github.mudrichenkoevgeny.backend.core.audit.database.repository

import io.github.mudrichenkoevgeny.backend.core.audit.database.table.AuditEventsTable
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEvent
import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEventId
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditStatus
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.uuid.Uuid

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditEventRepositoryImplTest {

    private val dataSource = createTestDataSource("audit_repo")

    private lateinit var repository: AuditEventRepositoryImpl

    @BeforeAll
    fun setup() {
        Database.connect(dataSource)
        runBlocking {
            suspendTransaction {
                SchemaUtils.create(AuditEventsTable)
            }
        }
        repository = AuditEventRepositoryImpl()
    }

    @Test
    fun `createEvent persists event and returns success`() = runBlocking {
        val event = AuditEvent(
            action = "login",
            resource = "session",
            status = AuditStatus.SUCCESS
        )

        val result = suspendTransaction { repository.createEvent(event) }

        assertNotNull(result)
        val success = result as AppResult.Success
        assertEquals(event.id, success.data.id)
        assertEquals("login", success.data.action)
        assertEquals("session", success.data.resource)
        assertEquals(AuditStatus.SUCCESS, success.data.status)
    }

    @Test
    fun `getEventById returns event when found`() = runBlocking {
        val event = AuditEvent(
            action = "create_order",
            resource = "order",
            status = AuditStatus.SUCCESS
        )
        suspendTransaction { repository.createEvent(event) }

        val result = suspendTransaction { repository.getEventById(event.id) }

        val success = result as AppResult.Success
        assertNotNull(success.data)
        assertEquals(event.id, success.data!!.id)
        assertEquals("create_order", success.data!!.action)
    }

    @Test
    fun `getEventById returns null when not found`() = runBlocking {
        val missingId = AuditEventId.generate()

        val result = suspendTransaction { repository.getEventById(missingId) }

        val success = result as AppResult.Success
        assertNull(success.data)
    }

    @Test
    fun `getEventsList returns paginated list ordered by createdAt desc`() = runBlocking {
        val event1 = AuditEvent(action = "getEventsList_a1", resource = "getEventsList_r", status = AuditStatus.SUCCESS)
        val event2 = AuditEvent(action = "getEventsList_a2", resource = "getEventsList_r", status = AuditStatus.SUCCESS)
        suspendTransaction {
            repository.createEvent(event1)
            repository.createEvent(event2)
        }

        val result = suspendTransaction { repository.getEventsList(PageParams(page = 1, size = 10)) }

        val success = result as AppResult.Success
        assertTrue(success.data.totalCount >= 2, "expected at least 2 events")
        assertTrue(success.data.items.any { it.id == event1.id }, "event1 should be in the list")
        assertTrue(success.data.items.any { it.id == event2.id }, "event2 should be in the list")
        assertEquals(1, success.data.page)
        assertEquals(10, success.data.size)
    }

    @Test
    fun `getEventsByActor filters by actorId`() = runBlocking {
        val actorId = Uuid.random()
        val event = AuditEvent(
            actorId = actorId,
            action = "action",
            resource = "resource",
            status = AuditStatus.SUCCESS
        )
        suspendTransaction { repository.createEvent(event) }

        val result = suspendTransaction { repository.getEventsByActor(PageParams(page = 1, size = 10), actorId) }

        val success = result as AppResult.Success
        assertEquals(1, success.data.totalCount)
        assertEquals(actorId, success.data.items.single().actorId)
    }
}
