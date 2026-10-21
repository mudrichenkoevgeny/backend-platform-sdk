package io.github.mudrichenkoevgeny.backend.core.audit.database.repository

import io.github.mudrichenkoevgeny.backend.core.audit.database.table.AuditEventsTable
import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditAccessFilter
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.audit.domain.model.action.TestAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.audit.domain.model.event.createTestAuditEventInternal
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.audit.domain.model.resource.TestAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.audit.domain.model.action.createTestCompositeAuditActionTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.audit.domain.model.metadata.createTestCompositeAuditMetadataKeyParser
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.audit.domain.model.resource.createTestCompositeAuditResourceTypeParser
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditValueSensitivity
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditEventRepositoryImplTest {

    private val dataSource = createTestDataSource("audit_repo")
    private lateinit var repository: AuditEventRepository

    @BeforeAll
    fun setupRepository() {
        Database.connect(dataSource)
        repository = AuditEventRepositoryImpl(
            compositeAuditActionTypeParser = createTestCompositeAuditActionTypeParser(),
            compositeAuditResourceTypeParser = createTestCompositeAuditResourceTypeParser(),
            compositeAuditMetadataKeyParser = createTestCompositeAuditMetadataKeyParser()
        )
    }

    @BeforeEach
    fun setUpDatabase() = runBlocking {
        suspendTransaction {
            SchemaUtils.create(AuditEventsTable)
        }
    }

    @AfterEach
    fun tearDownDatabase() = runBlocking {
        suspendTransaction {
            SchemaUtils.drop(AuditEventsTable)
        }
    }

    @Test
    fun `createEvent persists and getEventById returns same event`() = runBlocking {
        val event = createTestAuditEventInternal(
            actorType = AuditActorType.SYSTEM,
            action = TestAuditActionType("login"),
            resource = TestAuditResourceType("session"),
            status = AuditStatus.SUCCESS
        )

        suspendTransaction { repository.createEvent(event) }
        val result = suspendTransaction { repository.getEventById(event.id) }

        val success = result as AppResult.Success
        assertNotNull(success.data)
        assertEquals(event.id, success.data!!.id)
        assertEquals("login", success.data!!.action.value)
        assertEquals("session", success.data!!.resource.value)
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
        val event = createTestAuditEventInternal(
            actorType = AuditActorType.SYSTEM,
            action = TestAuditActionType("list_sys"),
            resource = TestAuditResourceType("r"),
            status = AuditStatus.SUCCESS
        )
        suspendTransaction { repository.createEvent(event) }

        val result = suspendTransaction {
            repository.getEventsPageWithAccessFilter(
                accessFilter = AuditAccessFilter(setOf(AuditActorType.SYSTEM), emptySet()),
                pageParams = PageParams()
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
                createTestAuditEventInternal(
                    actorType = AuditActorType.SYSTEM,
                    action = TestAuditActionType("invisible"),
                    resource = TestAuditResourceType("x"),
                    status = AuditStatus.SUCCESS
                )
            )
        }

        val result = suspendTransaction {
            repository.getEventsPageWithAccessFilter(
                accessFilter = AuditAccessFilter(emptySet(), emptySet()),
                pageParams = PageParams()
            )
        }

        val success = result as AppResult.Success
        assertEquals(0L, success.data.totalCount)
        assertTrue(success.data.items.isEmpty())
    }

    @Test
    fun `getEventsList filters by actorIds`() = runBlocking {
        val actorId = UUID.randomUUID().toString()
        val event = createTestAuditEventInternal(
            actorId = actorId,
            actorType = AuditActorType.SYSTEM,
            action = TestAuditActionType("by_actor"),
            resource = TestAuditResourceType("res"),
            status = AuditStatus.SUCCESS
        )
        suspendTransaction { repository.createEvent(event) }

        val result = suspendTransaction {
            repository.getEventsPageWithAccessFilter(
                accessFilter = AuditAccessFilter(setOf(AuditActorType.SYSTEM), emptySet()),
                pageParams = PageParams(),
                actorIds = listOf(actorId)
            )
        }

        val success = result as AppResult.Success
        assertTrue(success.data.items.any { it.id == event.id })
        assertTrue(success.data.items.all { it.actorId == actorId })
    }

    @Test
    fun `getEventsList filters by messages using ILIKE`() = runBlocking {
        val message = "Sensitive data leak"
        val event = createTestAuditEventInternal(
            actorId = null,
            actorType = AuditActorType.SYSTEM,
            action = TestAuditActionType("alert"),
            resource = TestAuditResourceType("security"),
            resourceValueSensitivity = AuditValueSensitivity.NON_SENSITIVE,
            status = AuditStatus.FAILED,
            message = message
        )
        suspendTransaction { repository.createEvent(event) }

        val result = suspendTransaction {
            repository.getEventsPageWithAccessFilter(
                accessFilter = AuditAccessFilter(setOf(AuditActorType.SYSTEM), emptySet()),
                pageParams = PageParams(),
                messages = listOf("DATA")
            )
        }

        val success = result as AppResult.Success
        assertTrue(success.data.items.any { it.message == message })
    }
}
