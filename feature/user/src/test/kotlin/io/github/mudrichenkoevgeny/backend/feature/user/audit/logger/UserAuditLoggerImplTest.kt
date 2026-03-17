package io.github.mudrichenkoevgeny.backend.feature.user.audit.logger

import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditStatus
import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

        val eventSlot = slot<io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEvent>()

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
        assertEquals(ACTION_LOGIN, event.action)
        assertEquals(RESOURCE_SESSION, event.resource)
        assertEquals(RESOURCE_ID_SESSION_1, event.resourceId)
        assertEquals(AuditStatus.FAILED, event.status)

        assertEquals(JsonPrimitive(IP_ADDRESS), event.metadata[UserAuditMetadata.Keys.IP_ADDRESS])
        assertEquals(JsonPrimitive(DEVICE_NAME_PIXEL), event.metadata[UserAuditMetadata.Keys.DEVICE_NAME])
        assertEquals(JsonPrimitive(UserAuditMetadata.Types.INTERNAL_ERROR), event.metadata[UserAuditMetadata.Keys.TYPE])
        assertEquals(JsonPrimitive("value"), event.metadata["custom"])
        assertFalse(event.metadata.containsKey("dropped"))
    }

    @Test
    fun `logFail without type does not include type key`() {
        val requestContext = requestContext(ipAddress = IP_ADDRESS, deviceName = null)
        val eventSlot = slot<io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEvent>()

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
        assertTrue(event.metadata.containsKey(UserAuditMetadata.Keys.IP_ADDRESS))
        assertFalse(event.metadata.containsKey(UserAuditMetadata.Keys.TYPE))
    }

    @Test
    fun `logSuccess logs success event`() {
        val requestContext = requestContext(ipAddress = IP_ADDRESS, deviceName = DEVICE_NAME_IPHONE)
        val eventSlot = slot<io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEvent>()

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
            JsonPrimitive(UserAuditMetadata.Types.VERIFICATION_CODE_SENT),
            event.metadata[UserAuditMetadata.Keys.TYPE]
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

