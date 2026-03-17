package io.github.mudrichenkoevgeny.backend.feature.user.error.model

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorSeverity
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorCodes
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserErrorTest {

    @Test
    fun `UserBlocked stores userId only in secretArgs`() {
        val userId = UserId.generate()

        val error = UserError.UserBlocked(userId = userId)

        assertEquals(UserErrorCodes.USER_BLOCKED, error.code)
        assertEquals(HttpStatusCode.Forbidden, error.httpStatusCode)
        assertEquals(AppErrorSeverity.LOW, error.appErrorSeverity)

        assertNull(error.publicArgs)
        assertEquals(
            mapOf(UserErrorArgs.USER_ID to userId.asHexDashString()),
            error.secretArgs
        )
    }

    @Test
    fun `UserBlocked without userId has null secretArgs`() {
        val error = UserError.UserBlocked(userId = null)

        assertNull(error.publicArgs)
        assertNull(error.secretArgs)
    }

    @Test
    fun `UserForbidden stores userId only in secretArgs`() {
        val userId = UserId.generate()

        val error = UserError.UserForbidden(userId = userId)

        assertEquals(UserErrorCodes.USER_FORBIDDEN, error.code)
        assertEquals(HttpStatusCode.Forbidden, error.httpStatusCode)
        assertEquals(AppErrorSeverity.MEDIUM, error.appErrorSeverity)
        assertNull(error.publicArgs)
        assertEquals(
            mapOf(UserErrorArgs.USER_ID to userId.asHexDashString()),
            error.secretArgs
        )
    }

    @Test
    fun `UserNotFound stores userId only in secretArgs`() {
        val userId = UserId.generate()

        val error = UserError.UserNotFound(userId = userId)

        assertEquals(UserErrorCodes.USER_NOT_FOUND, error.code)
        assertEquals(HttpStatusCode.NotFound, error.httpStatusCode)
        assertEquals(AppErrorSeverity.LOW, error.appErrorSeverity)
        assertNull(error.publicArgs)
        assertEquals(
            mapOf(UserErrorArgs.USER_ID to userId.asHexDashString()),
            error.secretArgs
        )
    }

    @Test
    fun `InvalidAccessToken has no args`() {
        val error = UserError.InvalidAccessToken()

        assertEquals(UserErrorCodes.INVALID_ACCESS_TOKEN, error.code)
        assertEquals(HttpStatusCode.Unauthorized, error.httpStatusCode)
        assertEquals(AppErrorSeverity.LOW, error.appErrorSeverity)
        assertNull(error.publicArgs)
        assertNull(error.secretArgs)
        assertTrue(error.errorId.value.toString().isNotBlank())
    }
}

