package io.github.mudrichenkoevgeny.backend.core.audit.logger

import io.github.mudrichenkoevgeny.shared.foundation.testing.core.audit.domain.model.action.TestAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.audit.domain.model.resource.TestAuditResourceType
import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.CommonAuditMetadataKey
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
    fun `log builds AuditEventInternal and forwards to auditService`() {
        val eventSlot = slot<AuditEventInternal>()
        val metadata = setOf(AuditEventMetadata(CommonAuditMetadataKey.ERROR_CODE, "E42"))

        logger.log(
            actorId = ACTOR_ID,
            actorType = AuditActorType.USER,
            actorUserRole = ROLE,
            action = TestAuditActionType(ACTION),
            resource = TestAuditResourceType(RESOURCE),
            resourceId = RESOURCE_ID,
            status = AuditStatus.FAILED,
            metadata = metadata,
            message = MESSAGE
        )

        verify(exactly = 1) { auditService.log(capture(eventSlot)) }

        val event = eventSlot.captured
        assertEquals(ACTOR_ID, event.actorId)
        assertEquals(AuditActorType.USER, event.actorType)
        assertEquals(ROLE, event.actorUserRole)
        assertEquals(ACTION, event.action.value)
        assertEquals(RESOURCE, event.resource.value)
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
