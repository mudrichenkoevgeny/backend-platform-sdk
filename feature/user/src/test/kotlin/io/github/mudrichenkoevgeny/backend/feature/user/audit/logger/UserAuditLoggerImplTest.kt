package io.github.mudrichenkoevgeny.backend.feature.user.audit.logger

import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserAuditLoggerImplTest {

    private val auditService: AuditService = mockk(relaxed = true)
    private val logger = UserAuditLoggerImpl(auditService = auditService)

    @Test
    fun `logInternalError logs failed event with metadata`() {
        val requestContext = requestContext(
            ipAddress = IP_ADDRESS,
            deviceName = DEVICE_NAME_PIXEL
        )

        val eventSlot = slot<AuditEvent>()

        logger.logInternalError(
            requestContext = requestContext,
            action = ACTION_LOGIN,
            resource = RESOURCE_SESSION,
            resourceId = RESOURCE_ID_SESSION_1,
            metadata = mapOf(
                "custom" to "value",
                "dropped" to null
            )
        )

        verify(exactly = 1) { auditService.log(capture(eventSlot)) }

        val event = eventSlot.captured
        assertNull(event.actorId)
        assertEquals(ACTION_LOGIN, event.action.serialName)
        assertEquals(RESOURCE_SESSION, event.resource.serialName)
        assertEquals(RESOURCE_ID_SESSION_1, event.resourceId)
        assertEquals(AuditStatus.FAILED, event.status)

        assertEquals(IP_ADDRESS, event.metadata.single { it.key == UserAuditMetadata.Keys.IP_ADDRESS }.value)
        assertEquals(DEVICE_NAME_PIXEL, event.metadata.single { it.key == UserAuditMetadata.Keys.DEVICE_NAME }.value)
        assertEquals(
            UserAuditMetadata.Types.INTERNAL_ERROR,
            event.metadata.single { it.key == UserAuditMetadata.Keys.TYPE }.value
        )
        assertEquals("value", event.metadata.single { it.key == "custom" }.value)
        assertTrue(event.metadata.none { it.key == "dropped" })
    }

    @Test
    fun `logFail without type does not include type key`() {
        val requestContext = requestContext(ipAddress = IP_ADDRESS, deviceName = null)
        val eventSlot = slot<AuditEvent>()

        logger.logFail(
            requestContext = requestContext,
            action = ACTION_REGISTER,
            resource = RESOURCE_USER,
            resourceId = null,
            type = null,
            metadata = emptyMap()
        )

        verify(exactly = 1) { auditService.log(capture(eventSlot)) }

        val event = eventSlot.captured
        assertEquals(AuditStatus.FAILED, event.status)
        assertTrue(event.metadata.any { it.key == UserAuditMetadata.Keys.IP_ADDRESS })
        assertTrue(event.metadata.none { it.key == UserAuditMetadata.Keys.TYPE })
    }

    @Test
    fun `logSuccess logs success event`() {
        val requestContext = requestContext(ipAddress = IP_ADDRESS, deviceName = DEVICE_NAME_IPHONE)
        val eventSlot = slot<AuditEvent>()

        logger.logSuccess(
            requestContext = requestContext,
            action = ACTION_REFRESH,
            resource = RESOURCE_SESSION,
            resourceId = null,
            type = UserAuditMetadata.Types.VERIFICATION_CODE_SENT,
            metadata = emptyMap()
        )

        verify(exactly = 1) { auditService.log(capture(eventSlot)) }

        val event = eventSlot.captured
        assertEquals(AuditStatus.SUCCESS, event.status)
        assertEquals(
            UserAuditMetadata.Types.VERIFICATION_CODE_SENT,
            event.metadata.single { it.key == UserAuditMetadata.Keys.TYPE }.value
        )
    }

    private fun requestContext(ipAddress: String?, deviceName: String?): RequestContext {
        return RequestContext(
            traceId = TRACE_ID,
            userId = null,
            sessionId = null,
            clientInfo = ClientInfo(
                clientType = null,
                userAgent = null,
                ipAddress = ipAddress,
                language = null,
                host = null,
                origin = null,
                deviceId = null,
                deviceName = deviceName,
                appVersion = null,
                operationSystemVersion = null
            )
        )
    }

    private companion object {
        const val TRACE_ID = "t1"

        const val IP_ADDRESS = "127.0.0.1"
        const val DEVICE_NAME_PIXEL = "pixel"
        const val DEVICE_NAME_IPHONE = "iphone"

        const val ACTION_LOGIN = "login"
        const val ACTION_REGISTER = "register"
        const val ACTION_REFRESH = "refresh"

        const val RESOURCE_SESSION = "session"
        const val RESOURCE_USER = "user"

        const val RESOURCE_ID_SESSION_1 = "s1"
    }
}

