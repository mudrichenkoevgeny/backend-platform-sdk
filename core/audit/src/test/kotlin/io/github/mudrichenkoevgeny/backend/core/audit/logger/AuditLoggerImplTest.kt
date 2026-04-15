package io.github.mudrichenkoevgeny.backend.core.audit.logger

import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.CommonAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AuditLoggerImplTest {

    private val auditService: AuditService = mockk(relaxed = true)
    private val logger = AuditLoggerImpl(auditService = auditService)

    @Test
    fun `log builds AuditEvent and forwards to auditService`() {
        val eventSlot = slot<AuditEvent>()
        val metadata = setOf(AuditEventMetadata(CommonAuditMetadataKey.ERROR_CODE, "E42"))

        logger.log(
            actorId = ACTOR_ID,
            actorType = AuditActorType.USER,
            actorUserRole = ROLE,
            action = TestAuditAction(ACTION),
            resource = TestAuditResource(RESOURCE),
            resourceId = RESOURCE_ID,
            status = AuditStatus.FAILED,
            message = MESSAGE,
            metadata = metadata
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
        assertEquals(metadata, event.metadata)
        assertNotNull(event.createdAt)
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
