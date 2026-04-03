package io.github.mudrichenkoevgeny.backend.core.audit.logger

import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AuditLoggerImplTest {

    private val auditService: AuditService = mockk(relaxed = true)
    private val logger = AuditLoggerImpl(auditService = auditService)

    @Test
    fun `log forwards fields and status to auditService`() {
        val eventSlot = slot<AuditEvent>()

        logger.log(
            actorId = ACTOR_ID,
            actorType = AuditActorType.USER,
            actorUserRole = ROLE,
            action = TestAuditAction(ACTION),
            resource = TestAuditResource(RESOURCE),
            resourceId = RESOURCE_ID,
            status = AuditStatus.FAILED,
            message = MESSAGE,
            metadata = emptyMap()
        )

        verify(exactly = 1) { auditService.log(capture(eventSlot)) }

        val event = eventSlot.captured
        assertEquals(ACTOR_ID, event.actorId)
        assertEquals(AuditActorType.USER, event.actorType)
        assertEquals(ROLE, event.actorUserRole)
        assertEquals(ACTION, event.action.serialName)
        assertEquals(RESOURCE, event.resource.serialName)
        assertEquals(RESOURCE_ID, event.resourceId)
        assertEquals(AuditStatus.FAILED, event.status)
        assertEquals(MESSAGE, event.message)
        assertNotNull(event.createdAt)
    }

    @Test
    fun `log encodes metadata and drops null values`() {
        val eventSlot = slot<AuditEvent>()
        val metadataKey = "k"
        val metadataValue = "v"

        logger.log(
            actorId = null,
            actorType = AuditActorType.SYSTEM,
            actorUserRole = null,
            action = TestAuditAction(ACTION),
            resource = TestAuditResource(RESOURCE),
            resourceId = null,
            status = AuditStatus.SUCCESS,
            message = null,
            metadata = mapOf(
                metadataKey to metadataValue,
                "null_value" to null
            )
        )

        verify(exactly = 1) { auditService.log(capture(eventSlot)) }

        val event = eventSlot.captured
        assertEquals(metadataValue, event.metadata.single { it.key == metadataKey }.value)
        assertTrue(event.metadata.none { it.key == "null_value" })
    }

    @Test
    fun `log uses SUCCESS status`() {
        val eventSlot = slot<AuditEvent>()

        logger.log(
            actorId = null,
            actorType = AuditActorType.SYSTEM,
            actorUserRole = null,
            action = TestAuditAction(ACTION),
            resource = TestAuditResource(RESOURCE),
            status = AuditStatus.SUCCESS,
            metadata = emptyMap()
        )

        verify(exactly = 1) { auditService.log(capture(eventSlot)) }
        assertEquals(AuditStatus.SUCCESS, eventSlot.captured.status)
    }

    @Test
    fun `log uses DENIED status`() {
        val eventSlot = slot<AuditEvent>()

        logger.log(
            actorId = null,
            actorType = AuditActorType.SYSTEM,
            actorUserRole = null,
            action = TestAuditAction(ACTION),
            resource = TestAuditResource(RESOURCE),
            status = AuditStatus.DENIED,
            metadata = emptyMap()
        )

        verify(exactly = 1) { auditService.log(capture(eventSlot)) }
        assertEquals(AuditStatus.DENIED, eventSlot.captured.status)
    }

    private companion object {
        const val ACTION = "test_action"
        const val RESOURCE = "test_resource"
        const val ACTOR_ID = "actor-1"
        const val RESOURCE_ID = "res-1"
        const val ROLE = "user"
        const val MESSAGE = "msg"
    }
}

private data class TestAuditAction(
    override val serialName: String
) : AuditActionType {
    override fun parseOrNull(value: String): AuditActionType? =
        if (value == serialName) this else null

    override fun parseOrThrow(value: String): AuditActionType =
        parseOrNull(value) ?: throw IllegalArgumentException("Invalid audit action: '$value'")
}

private data class TestAuditResource(
    override val serialName: String
) : AuditResourceType {
    override fun parseOrNull(value: String): AuditResourceType? =
        if (value == serialName) this else null

    override fun parseOrThrow(value: String): AuditResourceType =
        parseOrNull(value) ?: throw IllegalArgumentException("Invalid audit resource: '$value'")
}
