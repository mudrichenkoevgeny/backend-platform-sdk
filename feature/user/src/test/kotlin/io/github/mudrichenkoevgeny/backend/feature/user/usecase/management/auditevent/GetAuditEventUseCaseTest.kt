package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auditevent

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.event.createTestAuditEvent
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.manager.AuditManager
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.createTestUserDetails
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetAuditEventUseCaseTest {

    @Test
    fun `returns UserForbidden when current user is not found`() = runTest {
        val userId = UserId.generate()
        val userManager = mockk<UserManager>()
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(null)
        val useCase = GetAuditEventUseCase(userManager, mockk(relaxed = true))

        val result = useCase(AuditEventId.generate(), createTestAuthenticatedRequestContext(userId = userId))

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserForbidden)
    }

    @Test
    fun `returns NotFound when audit manager returns null event`() = runTest {
        val userId = UserId.generate()
        val details = createTestUserDetails(id = userId, role = UserRole.ADMIN)
        val eventId = AuditEventId.generate()
        val userManager = mockk<UserManager>()
        val auditManager = mockk<AuditManager>()

        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(details)
        coEvery {
            auditManager.getEventById(eventId, userId, details.permissionCodes)
        } returns AppResult.Success(null)

        val useCase = GetAuditEventUseCase(userManager, auditManager)
        val result = useCase(eventId, createTestAuthenticatedRequestContext(userId = userId))

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is CommonError.NotFound)
        assertEquals(AuditEvent::class.java.simpleName, (error as CommonError.NotFound).resource)
    }

    @Test
    fun `returns event from audit manager`() = runTest {
        val userId = UserId.generate()
        val details = createTestUserDetails(id = userId, role = UserRole.ADMIN)
        val eventId = AuditEventId.generate()
        val event = createTestAuditEvent(id = eventId, actorType = AuditActorType.USER, resourceId = "r1")
        val userManager = mockk<UserManager>()
        val auditManager = mockk<AuditManager>()

        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(details)
        coEvery {
            auditManager.getEventById(eventId, userId, details.permissionCodes)
        } returns AppResult.Success(event)

        val useCase = GetAuditEventUseCase(userManager, auditManager)
        val result = useCase(eventId, createTestAuthenticatedRequestContext(userId = userId))

        assertEquals(AppResult.Success(event), result)
        coVerify(exactly = 1) {
            auditManager.getEventById(eventId, userId, details.permissionCodes)
        }
    }
}
