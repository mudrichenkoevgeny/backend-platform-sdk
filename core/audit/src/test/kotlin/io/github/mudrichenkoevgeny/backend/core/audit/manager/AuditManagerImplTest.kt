package io.github.mudrichenkoevgeny.backend.core.audit.manager

import io.github.mudrichenkoevgeny.backend.core.audit.compositeAuditActionTypeParserForRepositoryTests
import io.github.mudrichenkoevgeny.backend.core.audit.compositeAuditResourceTypeParserForRepositoryTests
import io.github.mudrichenkoevgeny.backend.core.audit.database.repository.AuditEventRepositoryImpl
import io.github.mudrichenkoevgeny.backend.core.audit.database.table.AuditEventsTable
import io.github.mudrichenkoevgeny.backend.core.audit.domain.wire.AuditWireAction
import io.github.mudrichenkoevgeny.backend.core.audit.domain.wire.AuditWireResource
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.backend.core.common.mask.PayloadMaskingType
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditManagerImplTest {

    private val dataSource = createTestDataSource("audit_mgr")

    private lateinit var manager: AuditManager

    @BeforeAll
    fun setup() {
        Database.connect(dataSource)
        runBlocking {
            suspendTransaction {
                SchemaUtils.create(AuditEventsTable)
            }
        }
        val repository = AuditEventRepositoryImpl(
            compositeAuditActionTypeParser = compositeAuditActionTypeParserForRepositoryTests(),
            compositeAuditResourceTypeParser = compositeAuditResourceTypeParserForRepositoryTests(),
        )
        manager = AuditManagerImpl(repository)
    }

    @Test
    fun `createEvent delegates to repository and returns success`() = runBlocking {
        val event = AuditEvent(
            actorType = AuditActorType.SYSTEM,
            action = AuditWireAction("login"),
            resource = AuditWireResource("session"),
            status = AuditStatus.SUCCESS,
            createdAt = Clock.System.now()
        )

        val result = manager.createEvent(event)

        val success = result as AppResult.Success
        assertEquals(event.id, success.data.id)
        assertEquals("login", success.data.action.serialName)
    }

    @Test
    fun `getEventById delegates to repository and returns event when found`() = runBlocking {
        val event = AuditEvent(
            actorType = AuditActorType.SYSTEM,
            action = AuditWireAction("action"),
            resource = AuditWireResource("resource"),
            status = AuditStatus.FAILED,
            createdAt = Clock.System.now()
        )
        manager.createEvent(event)

        val result = manager.getEventById(
            payloadMaskingType = PayloadMaskingType.UNMASKED,
            eventId = event.id,
        )

        val success = result as AppResult.Success
        assertNotNull(success.data)
        assertEquals(event.id, success.data!!.id)
        assertEquals(AuditStatus.FAILED, success.data!!.status)
    }

    @Test
    fun `getEventById returns null when not found`() = runBlocking {
        val missingId = AuditEventId.generate()

        val result = manager.getEventById(
            payloadMaskingType = PayloadMaskingType.UNMASKED,
            eventId = missingId,
        )

        val success = result as AppResult.Success
        assertNull(success.data)
    }

    @Test
    fun `getEventsList returns paginated list`() = runBlocking {
        val event = AuditEvent(
            actorType = AuditActorType.SYSTEM,
            action = AuditWireAction("getEventsList_test_action"),
            resource = AuditWireResource("getEventsList_test_resource"),
            status = AuditStatus.SUCCESS,
            createdAt = Clock.System.now()
        )
        manager.createEvent(event)

        val result = manager.getEventsList(
            payloadMaskingType = PayloadMaskingType.UNMASKED,
            pageParams = PageParams(page = 1, size = 5),
        )

        val success = result as AppResult.Success
        assertTrue(success.data.totalCount >= 1, "expected at least 1 event")
        assertTrue(success.data.items.any { it.id == event.id }, "created event should be in the list")
        assertEquals(1, success.data.pageNumber)
        assertEquals(5, success.data.pageSize)
        assertTrue(success.data.totalPages >= 1L)
    }
}
