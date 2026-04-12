package io.github.mudrichenkoevgeny.backend.core.audit.service

import io.github.mudrichenkoevgeny.backend.core.audit.compositeAuditActionTypeParserForRepositoryTests
import io.github.mudrichenkoevgeny.backend.core.audit.compositeAuditResourceTypeParserForRepositoryTests
import io.github.mudrichenkoevgeny.backend.core.audit.database.repository.AuditEventRepository
import io.github.mudrichenkoevgeny.backend.core.audit.database.repository.AuditEventRepositoryImpl
import io.github.mudrichenkoevgeny.backend.core.audit.database.table.AuditEventsTable
import io.github.mudrichenkoevgeny.backend.core.audit.RepositoryTestAuditAction
import io.github.mudrichenkoevgeny.backend.core.audit.RepositoryTestAuditResource
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Clock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditServiceImplTest {

    private val dataSource = createTestDataSource("audit_svc")

    private lateinit var repository: AuditEventRepositoryImpl

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

    private fun ioBackgroundScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Test
    fun `log persists event via repository after awaitAll`() = runBlocking {
        val appLogger = mockk<AppLogger>(relaxed = true)
        val service = AuditServiceImpl(repository, ioBackgroundScope(), appLogger)

        val event = sampleAuditEvent(action = "login", resource = "session")
        service.log(event)
        service.awaitAll()

        val stored = suspendTransaction { repository.getEventById(event.id) }
        val success = stored as AppResult.Success
        assertNotNull(success.data)
        assertEquals(event.id, success.data!!.id)
        assertEquals("login", success.data!!.action.serialName)
    }

    @Test
    fun `log records error when createEvent fails`() = runBlocking {
        val repo = mockk<AuditEventRepository>(relaxed = true)
        coEvery { repo.createEvent(any()) } returns AppResult.Error(CommonError.Unknown("db error"))
        val appLogger = mockk<AppLogger>(relaxed = true)
        val service = AuditServiceImpl(repo, ioBackgroundScope(), appLogger)

        val event = sampleAuditEvent(action = "action", resource = "resource", status = AuditStatus.FAILED)
        service.log(event)
        service.awaitAll()

        coVerify(exactly = 1) { repo.createEvent(event) }
        verify(exactly = 1) { appLogger.logError(any()) }
    }

    private fun sampleAuditEvent(
        action: String,
        resource: String,
        status: AuditStatus = AuditStatus.SUCCESS,
    ): AuditEvent = AuditEvent(
        actorType = AuditActorType.SYSTEM,
        action = RepositoryTestAuditAction(action),
        resource = RepositoryTestAuditResource(resource),
        status = status,
        createdAt = Clock.System.now()
    )
}
