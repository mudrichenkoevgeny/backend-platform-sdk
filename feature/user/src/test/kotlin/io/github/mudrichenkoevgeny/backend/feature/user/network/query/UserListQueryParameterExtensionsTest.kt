package io.github.mudrichenkoevgeny.backend.feature.user.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val TEST_ROLE = "admin"
private const val TEST_STATUS = "active"
private const val TEST_PERMISSION = "USER_READ"
private const val TEST_LEVEL = "5"
private const val INVALID_VAL = "invalid_value"

class UserListQueryParameterExtensionsTest {

    private val call = mockk<ApplicationCall>()
    private val request = mockk<ApplicationRequest>()
    private val filterNames = UserFilterValues.UserFilterValues

    @BeforeEach
    fun setUp() {
        every { call.request } returns request
    }

    @Test
    fun `should parse all user list query parameters successfully`() {
        setupMockParameters(
            mapOf(
                filterNames.ROLE to listOf(TEST_ROLE),
                filterNames.ACCOUNT_STATUS to listOf(TEST_STATUS),
                filterNames.ACCOUNT_STATUS_BEFORE_DELETION to listOf(TEST_STATUS),
                filterNames.AUTHORITY_LEVEL_FROM to listOf(TEST_LEVEL),
                filterNames.AUTHORITY_LEVEL_TO to listOf("10"),
                filterNames.PERMISSION_CODES to listOf(TEST_PERMISSION),
                filterNames.IS_TOTP_ENABLED to listOf("true"),
                "sortBy" to listOf("createdAt")
            )
        )

        val result = call.parseUsersListQueryParams()

        assertEquals(UserRole.ADMIN, result.roles.first())
        assertEquals(UserAccountStatus.ACTIVE, result.accountStatuses.first())
        assertEquals(UserAccountStatus.ACTIVE, result.accountStatusesBeforeDeletion.first())
        assertEquals(5, result.authorityLevelFrom)
        assertEquals(10, result.authorityLevelTo)
        assertTrue(result.requiredPermissionCodes.contains(PermissionCode(TEST_PERMISSION)))
        assertEquals(true, result.isTotpEnabled)
        assertEquals(UserSortValues.UserSortBy.CREATED_AT, result.listing.sortBy)
    }

    @Test
    fun `should throw RequestHandlingException when role is invalid`() {
        setupMockParameters(mapOf(filterNames.ROLE to listOf(INVALID_VAL)))

        val exception = assertThrows<RequestHandlingException> {
            call.parseUsersListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(filterNames.ROLE, error.parameterName)
    }

    @Test
    fun `should throw RequestHandlingException when account status is invalid`() {
        setupMockParameters(mapOf(filterNames.ACCOUNT_STATUS to listOf(INVALID_VAL)))

        val exception = assertThrows<RequestHandlingException> {
            call.parseUsersListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(filterNames.ACCOUNT_STATUS, error.parameterName)
    }

    @Test
    fun `should throw RequestHandlingException when account status before deletion is invalid`() {
        setupMockParameters(mapOf(filterNames.ACCOUNT_STATUS_BEFORE_DELETION to listOf(INVALID_VAL)))

        val exception = assertThrows<RequestHandlingException> {
            call.parseUsersListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(filterNames.ACCOUNT_STATUS_BEFORE_DELETION, error.parameterName)
    }

    @Test
    fun `should handle null when authority levels are not numeric`() {
        setupMockParameters(
            mapOf(
                filterNames.AUTHORITY_LEVEL_FROM to listOf("not_a_number"),
                filterNames.AUTHORITY_LEVEL_TO to listOf("neither_this")
            )
        )

        val result = call.parseUsersListQueryParams()

        assertEquals(null, result.authorityLevelFrom)
        assertEquals(null, result.authorityLevelTo)
    }

    @Test
    fun `should handle null when totp enabled is not a boolean`() {
        setupMockParameters(mapOf(filterNames.IS_TOTP_ENABLED to listOf("maybe")))

        val result = call.parseUsersListQueryParams()

        assertEquals(null, result.isTotpEnabled)
    }

    private fun setupMockParameters(params: Map<String, List<String>>) {
        val parameters = io.ktor.http.Parameters.build {
            params.forEach { (key, values) -> appendAll(key, values) }
        }
        every { request.queryParameters } returns parameters
    }
}