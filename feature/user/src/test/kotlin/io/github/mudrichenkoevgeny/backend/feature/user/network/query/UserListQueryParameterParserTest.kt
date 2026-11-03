package io.github.mudrichenkoevgeny.backend.feature.user.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.testing.contract.assertAllSortByEnumEntriesParsed
import io.github.mudrichenkoevgeny.backend.core.common.testing.contract.assertDtoSynchronization
import io.github.mudrichenkoevgeny.backend.core.common.testing.contract.assertExhaustiveFilterKeysParsed
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.UserListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.ListingParamNames
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortBy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.navigation.screen.management.user.globallist.ManagementUserListParams
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

private const val TEST_ROLE = "admin"
private const val TEST_STATUS = "active"
private const val TEST_PERMISSION = "USER_READ"
private const val TEST_LEVEL = "5"
private const val TEST_MAX_LEVEL = "10"
private const val TEST_LOCKOUT_TYPE = "temporary"
private const val TEST_SORT_BY = "created_at"
private const val TEST_TIMESTAMP = "2026-01-01T00:00:00Z"
private const val TEST_TRUE = "true"
private const val INVALID_VAL = "invalid_value"

class UserListQueryParameterParserTest {

    private val call = mockk<ApplicationCall>()
    private val request = mockk<ApplicationRequest>()
    private val filterNames = UserFilterValues

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
                filterNames.ACCOUNT_STATUS_ON_RESTORE to listOf(TEST_STATUS),
                filterNames.AUTHORITY_LEVEL_FROM to listOf(TEST_LEVEL),
                filterNames.AUTHORITY_LEVEL_TO to listOf(TEST_MAX_LEVEL),
                filterNames.PERMISSION_CODES to listOf(TEST_PERMISSION),
                filterNames.IS_TOTP_ENABLED to listOf(TEST_TRUE),
                filterNames.ACCOUNT_LOCKOUT_TYPE to listOf(TEST_LOCKOUT_TYPE),
                ListingParamNames.Sort.SORT_BY to listOf(TEST_SORT_BY)
            )
        )

        val result = call.parseUsersListQueryParams()

        assertEquals(UserRole.ADMIN, result.roles.first())
        assertEquals(UserAccountStatus.ACTIVE, result.accountStatuses.first())
        assertEquals(UserAccountStatus.ACTIVE, result.accountStatusesOnRestore.first())
        assertEquals(5, result.authorityLevelFrom)
        assertEquals(10, result.authorityLevelTo)
        assertTrue(result.requiredPermissionCodes.contains(PermissionCode(TEST_PERMISSION)))
        assertEquals(true, result.isTotpEnabled)
        assertEquals(UserSortBy.CREATED_AT, result.listing.sortBy)
    }

    @Test
    fun `UserListQueryParams backend model should stay synchronized with ManagementUserListParams shared model`() {
        assertDtoSynchronization(
            sharedDtoClass = ManagementUserListParams::class,
            backendDtoClass = UserListQueryParams::class,
            propertyAliases = mapOf(
                "userRoles" to "roles",
                "excludeUserRoles" to "excludeRoles",
                "permissionCodes" to "requiredPermissionCodes"
            ),
            ignoredSharedProperties = setOf(
                "pageNumber",
                "pageSize",
                "sortBy",
                "sortOrder",
                "parser"
            ),
            ignoredBackendProperties = setOf("listing")
        )
    }

    @Test
    fun `parseUsersListQueryParams should extract and parse all wire keys defined in UserFilterValues`() {
        assertExhaustiveFilterKeysParsed(
            filterKeysClass = UserFilterValues::class,
            sampleValueProvider = ::getSampleValidValueForFilterKey,
            parseFunction = { mockCall -> mockCall.parseUsersListQueryParams() }
        )
    }

    @Test
    fun `parseUsersListQueryParams should accept and parse all UserSortBy wire values`() {
        assertAllSortByEnumEntriesParsed(
            enumEntries = UserSortBy.entries,
            wireValueExtractor = { sortByEntry -> sortByEntry.value },
            parseFunction = { mockCall -> mockCall.parseUsersListQueryParams() },
            assertSortByFunction = { parsedQueryParams, expectedSortBy ->
                assertEquals(expectedSortBy, parsedQueryParams.listing.sortBy)
            }
        )
    }

    @ParameterizedTest
    @MethodSource("invalidQueryParameterNameProvider")
    fun `should throw RequestHandlingException when validated query parameter contains invalid value`(paramName: String) {
        setupMockParameters(mapOf(paramName to listOf(INVALID_VAL)))

        val exception = assertThrows<RequestHandlingException> {
            call.parseUsersListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(paramName, error.parameterName)
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

        assertNull(result.authorityLevelFrom)
        assertNull(result.authorityLevelTo)
    }

    @Test
    fun `should handle null when totp enabled is not a boolean`() {
        setupMockParameters(mapOf(filterNames.IS_TOTP_ENABLED to listOf("maybe")))

        val result = call.parseUsersListQueryParams()

        assertNull(result.isTotpEnabled)
    }

    private fun getSampleValidValueForFilterKey(key: String): String = when {
        key == filterNames.USER_ID || key == "avatar_file_id" || key.endsWith("_id") -> "00000000-0000-0000-0000-000000000001"
        key.endsWith("_from") || key.endsWith("_to") -> when {
            key.startsWith("authority_level") || key.startsWith("storage_") -> TEST_LEVEL
            else -> TEST_TIMESTAMP
        }
        key.startsWith("is_") -> TEST_TRUE
        key == UserFilterValues.ROLE || key == UserFilterValues.EXCLUDE_ROLE -> TEST_ROLE
        key == UserFilterValues.ACCOUNT_STATUS || key == UserFilterValues.EXCLUDE_ACCOUNT_STATUS ||
                key == UserFilterValues.ACCOUNT_STATUS_ON_RESTORE || key == UserFilterValues.EXCLUDE_ACCOUNT_STATUS_ON_RESTORE -> TEST_STATUS
        key == UserFilterValues.ACCOUNT_LOCKOUT_TYPE || key == UserFilterValues.EXCLUDE_ACCOUNT_LOCKOUT_TYPE -> TEST_LOCKOUT_TYPE
        key == UserFilterValues.PERMISSION_CODES || key == UserFilterValues.EXCLUDE_PERMISSION_CODES -> TEST_PERMISSION
        else -> "sample_value"
    }

    private fun setupMockParameters(params: Map<String, List<String>>) {
        val parameters = Parameters.build {
            params.forEach { (key, values) -> appendAll(key, values) }
        }
        every { request.queryParameters } returns parameters
    }

    companion object {
        @JvmStatic
        fun invalidQueryParameterNameProvider(): List<String> {
            val filterNames = UserFilterValues
            return listOf(
                filterNames.ROLE,
                filterNames.EXCLUDE_ROLE,
                filterNames.ACCOUNT_STATUS,
                filterNames.EXCLUDE_ACCOUNT_STATUS,
                filterNames.ACCOUNT_STATUS_ON_RESTORE,
                filterNames.EXCLUDE_ACCOUNT_STATUS_ON_RESTORE,
                filterNames.ACCOUNT_LOCKOUT_TYPE,
                filterNames.EXCLUDE_ACCOUNT_LOCKOUT_TYPE
            )
        }
    }
}
