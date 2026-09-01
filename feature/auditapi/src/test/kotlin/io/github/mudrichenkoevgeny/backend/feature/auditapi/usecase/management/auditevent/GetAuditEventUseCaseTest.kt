package io.github.mudrichenkoevgeny.backend.feature.auditapi.usecase.management.auditevent

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.auditapi.manager.AuditManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
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

    private fun clientInfo(): ClientInfo = ClientInfo(
        deviceInfo = ClientDeviceInfo(null, null, null, null, null, null),
        userAgent = null,
        ipAddress = "127.0.0.1",
        host = null,
        origin = null,
        apiVersion = null
    )

    private fun authContext(userId: UserId): AuthenticatedRequestContext = AuthenticatedRequestContext(
        traceId = null,
        userId = userId,
        userRole = UserRole.ADMIN,
        sessionId = UserSessionId.generate(),
        clientInfo = clientInfo()
    )

    private fun userDetails(id: UserId): UserDetails {
        val now = Clock.System.now()
        return UserDetails(
            id = id,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            accountStatusBeforeDeletion = UserAccountStatus.ACTIVE,
            authorityLevel = 1,
            permissionCodes = emptySet(),
            isTotpEnabled = false,
            lastLoginAt = now,
            lastActiveAt = now,
            createdAt = now
        )
    }

    private fun sampleEvent(id: AuditEventId = AuditEventId.generate()): AuditEvent = AuditEvent(
        id = id,
        actorId = null,
        actorType = AuditActorType.USER,
        actorUserRole = null,
        action = object : AuditActionType {
            override val serialName: String = "test_action"
            override fun parseOrNull(value: String): AuditActionType? = null
            override fun parseOrThrow(value: String): AuditActionType = throw UnsupportedOperationException()
        },
        resource = object : AuditResourceType {
            override val serialName: String = "test_resource"
            override fun parseOrNull(value: String): AuditResourceType? = null
            override fun parseOrThrow(value: String): AuditResourceType = throw UnsupportedOperationException()
        },
        resourceId = "r1",
        status = AuditStatus.SUCCESS,
        metadata = emptySet(),
        message = null,
        createdAt = Clock.System.now()
    )

    @Test
    fun `returns UserForbidden when current user is not found`() = runTest {
        val userId = UserId.generate()
        val userManager = mockk<UserManager>()
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(null)
        val useCase = GetAuditEventUseCase(userManager, mockk(relaxed = true))

        val result = useCase(AuditEventId.generate(), authContext(userId))

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserForbidden)
    }

    @Test
    fun `returns NotFound when audit manager returns null event`() = runTest {
        val userId = UserId.generate()
        val details = userDetails(userId)
        val eventId = AuditEventId.generate()
        val userManager = mockk<UserManager>()
        val auditManager = mockk<AuditManager>()

        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(details)
        coEvery {
            auditManager.getEventById(eventId, userId, details.permissionCodes)
        } returns AppResult.Success(null)

        val useCase = GetAuditEventUseCase(userManager, auditManager)
        val result = useCase(eventId, authContext(userId))

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is CommonError.NotFound)
        assertEquals(AuditEvent::class.java.simpleName, (error as CommonError.NotFound).resource)
    }

    @Test
    fun `returns event from audit manager`() = runTest {
        val userId = UserId.generate()
        val details = userDetails(userId)
        val eventId = AuditEventId.generate()
        val event = sampleEvent(eventId)
        val userManager = mockk<UserManager>()
        val auditManager = mockk<AuditManager>()

        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(details)
        coEvery {
            auditManager.getEventById(eventId, userId, details.permissionCodes)
        } returns AppResult.Success(event)

        val useCase = GetAuditEventUseCase(userManager, auditManager)
        val result = useCase(eventId, authContext(userId))

        assertEquals(AppResult.Success(event), result)
        coVerify(exactly = 1) {
            auditManager.getEventById(eventId, userId, details.permissionCodes)
        }
    }
}