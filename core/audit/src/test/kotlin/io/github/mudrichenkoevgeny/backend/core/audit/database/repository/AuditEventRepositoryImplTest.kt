package io.github.mudrichenkoevgeny.backend.core.audit.database.repository

import io.github.mudrichenkoevgeny.backend.core.audit.compositeAuditActionTypeParserForRepositoryTests
import io.github.mudrichenkoevgeny.backend.core.audit.compositeAuditResourceTypeParserForRepositoryTests
import io.github.mudrichenkoevgeny.backend.core.audit.database.table.AuditEventsTable
import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditAccessFilter
import io.github.mudrichenkoevgeny.backend.core.audit.RepositoryTestAuditAction
import io.github.mudrichenkoevgeny.backend.core.audit.RepositoryTestAuditResource
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
import java.util.UUID
import kotlin.time.Clock

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
            compositeAuditResourceTypeParser = compositeAuditResourceTypeParserForRepositoryTests()
        )
    }

    @Test
    fun `createEvent persists and getEventById returns same event`() = runBlocking {
        val event = AuditEvent(
            actorType = AuditActorType.SYSTEM,
            action = RepositoryTestAuditAction("login"),
            resource = RepositoryTestAuditResource("session"),
            status = AuditStatus.SUCCESS,
            createdAt = Clock.System.now()
        )

        suspendTransaction { repository.createEvent(event) }
        val result = suspendTransaction { repository.getEventById(event.id) }

        val success = result as AppResult.Success
        assertNotNull(success.data)
        assertEquals(event.id, success.data!!.id)
        assertEquals("login", success.data!!.action.serialName)
        assertEquals("session", success.data!!.resource.serialName)
        assertEquals(AuditStatus.SUCCESS, success.data!!.status)
    }

    @Test
    fun `getEventById returns null when not found`() = runBlocking {
        val missingId = AuditEventId.generate()

        val result = suspendTransaction { repository.getEventById(missingId) }

        val success = result as AppResult.Success
        assertNull(success.data)
    }

    @Test
    fun `getEventsList applies access filter for system actor`() = runBlocking {
        val event = AuditEvent(
            actorType = AuditActorType.SYSTEM,
            action = RepositoryTestAuditAction("list_sys"),
            resource = RepositoryTestAuditResource("r"),
            status = AuditStatus.SUCCESS,
            createdAt = Clock.System.now()
        )
        suspendTransaction { repository.createEvent(event) }

        val result = suspendTransaction {
            repository.getEventsList(
                accessFilter = AuditAccessFilter(setOf(AuditActorType.SYSTEM), emptySet()),
                pageParams = PageParams(page = 1, size = 20)
            )
        }

        val success = result as AppResult.Success
        assertTrue(success.data.items.any { it.id == event.id })
        assertTrue(success.data.items.all { it.actorType == AuditActorType.SYSTEM })
    }

    @Test
    fun `getEventsList returns no rows when access filter is empty`() = runBlocking {
        suspendTransaction {
            repository.createEvent(
                AuditEvent(
                    actorType = AuditActorType.SYSTEM,
                    action = RepositoryTestAuditAction("invisible"),
                    resource = RepositoryTestAuditResource("x"),
                    status = AuditStatus.SUCCESS,
                    createdAt = Clock.System.now()
                )
            )
        }

        val result = suspendTransaction {
            repository.getEventsList(
                accessFilter = AuditAccessFilter(emptySet(), emptySet()),
                pageParams = PageParams(page = 1, size = 50)
            )
        }

        val success = result as AppResult.Success
        assertEquals(0L, success.data.totalCount)
        assertTrue(success.data.items.isEmpty())
    }

    @Test
    fun `getEventsList filters by actorId`() = runBlocking {
        val actorId = UUID.randomUUID().toString()
        val event = AuditEvent(
            actorId = actorId,
            actorType = AuditActorType.SYSTEM,
            action = RepositoryTestAuditAction("by_actor"),
            resource = RepositoryTestAuditResource("res"),
            status = AuditStatus.SUCCESS,
            createdAt = Clock.System.now()
        )
        suspendTransaction { repository.createEvent(event) }

        val result = suspendTransaction {
            repository.getEventsList(
                accessFilter = AuditAccessFilter(setOf(AuditActorType.SYSTEM), emptySet()),
                pageParams = PageParams(page = 1, size = 50),
                actorId = actorId
            )
        }

        val success = result as AppResult.Success
        assertTrue(success.data.items.any { it.id == event.id })
        assertTrue(success.data.items.all { it.actorId == actorId })
    }
}
