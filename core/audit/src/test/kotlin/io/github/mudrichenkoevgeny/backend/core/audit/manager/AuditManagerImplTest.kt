package io.github.mudrichenkoevgeny.backend.core.audit.manager

import io.github.mudrichenkoevgeny.backend.core.audit.database.repository.AuditEventRepositoryImpl
import io.github.mudrichenkoevgeny.backend.core.audit.database.table.AuditEventsTable
import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEvent
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditManagerImplTest {

    private val dataSource = createTestDataSource("audit_mgr")

    private lateinit var manager: AuditManagerImpl

    @BeforeAll
    fun setup() {
        Database.connect(dataSource)
        runBlocking {
            suspendTransaction {
                SchemaUtils.create(AuditEventsTable)
            }
        }
        val repository = AuditEventRepositoryImpl()
        manager = AuditManagerImpl(repository)
    }

    @Test
    fun `createEvent delegates to repository and returns success`() = runBlocking {
        val event = AuditEvent(
            action = "login",
            resource = "session",
            status = AuditStatus.SUCCESS
        )

        val result = manager.createEvent(event)

        val success = result as AppResult.Success
        assertEquals(event.id, success.data.id)
        assertEquals("login", success.data.action)
    }

    @Test
    fun `getEventById delegates to repository and returns event when found`() = runBlocking {
        val event = AuditEvent(
            action = "action",
            resource = "resource",
            status = AuditStatus.FAILED
        )
        manager.createEvent(event)

        val result = manager.getEventById(event.id)

        val success = result as AppResult.Success
        assertNotNull(success.data)
        assertEquals(event.id, success.data!!.id)
        assertEquals(AuditStatus.FAILED, success.data!!.status)
    }

    @Test
    fun `getEventById returns null when not found`() = runBlocking {
        val missingId = AuditEventId.generate()

        val result = manager.getEventById(missingId)

        val success = result as AppResult.Success
        assertNull(success.data)
    }

    @Test
    fun `getEventsList returns paginated list`() = runBlocking {
        val event = AuditEvent(
            action = "getEventsList_test_action",
            resource = "getEventsList_test_resource",
            status = AuditStatus.SUCCESS
        )
        manager.createEvent(event)

        val result = manager.getEventsList(
            params = PageParams(page = 1, size = 5),
            actorId = null,
            action = null,
            resource = null,
            resourceId = null,
            status = null,
            fromTimestamp = null,
            toTimestamp = null
        )

        val success = result as AppResult.Success
        assertTrue(success.data.totalCount >= 1, "expected at least 1 event")
        assertTrue(success.data.items.any { it.id == event.id }, "created event should be in the list")
        assertEquals(1, success.data.page)
        assertEquals(5, success.data.size)
    }
}
