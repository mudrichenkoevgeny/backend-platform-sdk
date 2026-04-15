package io.github.mudrichenkoevgeny.backend.feature.audit.api.usecase.management.auditevent

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.audit.api.manager.AuditManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class GetAuditEventUseCaseTest {

    private companion object {
        private const val IP_ADDRESS = "127.0.0.1"
    }

    private fun clientInfo(): ClientInfo = ClientInfo(
        deviceInfo = ClientDeviceInfo(
            deviceId = null,
            deviceName = null,
            clientType = null,
            language = null,
            appVersion = null,
            operationSystemVersion = null
        ),
        userAgent = null,
        ipAddress = IP_ADDRESS,
        host = null,
        origin = null,
        apiVersion = null
    )

    private fun requestContext(userId: UserId?): RequestContext = RequestContext(
        traceId = null,
        userId = userId,
        userRole = null,
        sessionId = null,
        clientInfo = clientInfo()
    )

    private fun userDetails(id: UserId): UserDetails {
        val now = Clock.System.now()
        return UserDetails(
            id = id,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            accountStatusBeforeDeletion = UserAccountStatus.ACTIVE,
            permissions = emptySet(),
            lastLoginAt = now,
            lastActiveAt = now,
            createdAt = now,
            updatedAt = null,
            scheduledPermanentDeletionAt = null
        )
    }

    private fun sampleEvent(): AuditEvent = AuditEvent(
        actorId = null,
        actorType = AuditActorType.USER,
        actorUserRole = null,
        action = StringBackedAuditAction("login"),
        resource = StringBackedAuditResource("session"),
        resourceId = "r1",
        status = AuditStatus.SUCCESS,
        metadata = emptySet(),
        message = null,
        createdAt = Clock.System.now()
    )

    @Test
    fun `returns UserForbidden when request has no userId`() = runTest {
        val useCase = GetAuditEventUseCase(mockk(), mockk(relaxed = true))

        val result = useCase(AuditEventId.generate(), requestContext = requestContext(userId = null))

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserForbidden)
    }

    @Test
    fun `returns UserForbidden when current user is not found`() = runTest {
        val userId = UserId.generate()
        val userManager = mockk<UserManager>()
        coEvery { userManager.getUserById(userId) } returns AppResult.Success(null)
        val useCase = GetAuditEventUseCase(userManager, mockk(relaxed = true))

        val result = useCase(AuditEventId.generate(), requestContext = requestContext(userId))

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserForbidden)
    }

    @Test
    fun `propagates error when getUserById fails`() = runTest {
        val userId = UserId.generate()
        val err = CommonError.Internal(Throwable("db"))
        val userManager = mockk<UserManager>()
        coEvery { userManager.getUserById(userId) } returns AppResult.Error(err)
        val useCase = GetAuditEventUseCase(userManager, mockk(relaxed = true))

        val result = useCase(AuditEventId.generate(), requestContext = requestContext(userId))

        assertEquals(AppResult.Error(err), result)
    }

    @Test
    fun `returns NotFound when audit manager returns null event`() = runTest {
        val userId = UserId.generate()
        val details = userDetails(userId)
        val eventId = AuditEventId.generate()
        val userManager = mockk<UserManager>()
        val auditManager = mockk<AuditManager>()
        coEvery { userManager.getUserById(userId) } returns AppResult.Success(details)
        coEvery {
            auditManager.getEventById(eventId, userId, details.permissions)
        } returns AppResult.Success(null)

        val useCase = GetAuditEventUseCase(userManager, auditManager)
        val result = useCase(eventId, requestContext = requestContext(userId))

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is CommonError.NotFound)
        val notFound = error as CommonError.NotFound
        assertEquals(AuditEvent::class.java.simpleName, notFound.resource)
        assertEquals(eventId.asHexDashString(), notFound.identifier)
    }

    @Test
    fun `returns event from audit manager`() = runTest {
        val userId = UserId.generate()
        val details = userDetails(userId)
        val event = sampleEvent()
        val userManager = mockk<UserManager>()
        val auditManager = mockk<AuditManager>()
        coEvery { userManager.getUserById(userId) } returns AppResult.Success(details)
        coEvery {
            auditManager.getEventById(event.id, userId, details.permissions)
        } returns AppResult.Success(event)

        val useCase = GetAuditEventUseCase(userManager, auditManager)
        val result = useCase(event.id, requestContext = requestContext(userId))

        assertEquals(AppResult.Success(event), result)
        coVerify(exactly = 1) {
            auditManager.getEventById(event.id, userId, details.permissions)
        }
    }

    private data class StringBackedAuditAction(
        override val serialName: String
    ) : AuditActionType {
        override fun parseOrNull(value: String): AuditActionType = StringBackedAuditAction(value)
        override fun parseOrThrow(value: String): AuditActionType = StringBackedAuditAction(value)
    }

    private data class StringBackedAuditResource(
        override val serialName: String
    ) : AuditResourceType {
        override fun parseOrNull(value: String): AuditResourceType = StringBackedAuditResource(value)
        override fun parseOrThrow(value: String): AuditResourceType = StringBackedAuditResource(value)
    }
}
