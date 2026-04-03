package io.github.mudrichenkoevgeny.backend.core.audit.database.repository

import io.github.mudrichenkoevgeny.backend.core.audit.compositeAuditActionTypeParserForRepositoryTests
import io.github.mudrichenkoevgeny.backend.core.audit.compositeAuditResourceTypeParserForRepositoryTests
import io.github.mudrichenkoevgeny.backend.core.audit.database.table.AuditEventsTable
import io.github.mudrichenkoevgeny.backend.core.audit.domain.wire.AuditWireAction
import io.github.mudrichenkoevgeny.backend.core.audit.domain.wire.AuditWireResource
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
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
import kotlin.time.Clock
import kotlin.uuid.Uuid

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditEventRepositoryImplTest {

    private val dataSource = createTestDataSource("audit_repo")

    private lateinit var repository: AuditEventRepository

    @BeforeAll
    fun setup() {
        Database.connect(dataSource)
        runBlocking {
            suspendTransaction {
                SchemaUtils.create(AuditEventsTable)
            }
        }
        repository = AuditEventRepositoryImpl(
            compositeAuditActionTypeParser = compositeAuditActionTypeParserForRepositoryTests(),
            compositeAuditResourceTypeParser = compositeAuditResourceTypeParserForRepositoryTests(),
        )
    }

    @Test
    fun `createEvent persists event and returns success`() = runBlocking {
        val event = AuditEvent(
            actorType = AuditActorType.SYSTEM,
            action = AuditWireAction("login"),
            resource = AuditWireResource("session"),
            status = AuditStatus.SUCCESS,
            createdAt = Clock.System.now()
        )

        val result = suspendTransaction { repository.createEvent(event) }

        assertNotNull(result)
        val success = result as AppResult.Success
        assertEquals(event.id, success.data.id)
        assertEquals("login", success.data.action.serialName)
        assertEquals("session", success.data.resource.serialName)
        assertEquals(AuditStatus.SUCCESS, success.data.status)
    }

    @Test
    fun `getEventById returns event when found`() = runBlocking {
        val event = AuditEvent(
            actorType = AuditActorType.SYSTEM,
            action = AuditWireAction("create_order"),
            resource = AuditWireResource("order"),
            status = AuditStatus.SUCCESS,
            createdAt = Clock.System.now()
        )
        suspendTransaction { repository.createEvent(event) }

        val result = suspendTransaction { repository.getEventById(event.id) }

        val success = result as AppResult.Success
        assertNotNull(success.data)
        assertEquals(event.id, success.data!!.id)
        assertEquals("create_order", success.data!!.action.serialName)
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
        val event1 = AuditEvent(
            actorType = AuditActorType.SYSTEM,
            action = AuditWireAction("getEventsList_a1"),
            resource = AuditWireResource("getEventsList_r"),
            status = AuditStatus.SUCCESS,
            createdAt = Clock.System.now()
        )
        val event2 = AuditEvent(
            actorType = AuditActorType.SYSTEM,
            action = AuditWireAction("getEventsList_a2"),
            resource = AuditWireResource("getEventsList_r"),
            status = AuditStatus.SUCCESS,
            createdAt = Clock.System.now()
        )
        suspendTransaction {
            repository.createEvent(event1)
            repository.createEvent(event2)
        }

        val result = suspendTransaction { repository.getEventsList(PageParams(page = 1, size = 10)) }

        val success = result as AppResult.Success
        assertTrue(success.data.totalCount >= 2, "expected at least 2 events")
        assertTrue(success.data.items.any { it.id == event1.id }, "event1 should be in the list")
        assertTrue(success.data.items.any { it.id == event2.id }, "event2 should be in the list")
        assertEquals(1, success.data.pageNumber)
        assertEquals(10, success.data.pageSize)
        assertTrue(success.data.totalPages >= 1L)
    }

    @Test
    fun `getEventsList filters by actorId`() = runBlocking {
        val actorId = Uuid.random().toString()
        val event = AuditEvent(
            actorId = actorId,
            actorType = AuditActorType.USER,
            action = AuditWireAction("action"),
            resource = AuditWireResource("resource"),
            status = AuditStatus.SUCCESS,
            createdAt = Clock.System.now()
        )
        suspendTransaction { repository.createEvent(event) }

        val result = suspendTransaction {
            repository.getEventsList(
                pageParams = PageParams(page = 1, size = 10),
                actorId = actorId
            )
        }

        val success = result as AppResult.Success
        assertEquals(1L, success.data.totalCount)
        assertEquals(actorId, success.data.items.single().actorId)
    }
}
