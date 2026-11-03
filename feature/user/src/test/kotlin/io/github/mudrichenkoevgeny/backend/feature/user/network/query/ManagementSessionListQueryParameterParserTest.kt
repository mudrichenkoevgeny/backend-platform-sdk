package io.github.mudrichenkoevgeny.backend.feature.user.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.testing.contract.assertAllSortByEnumEntriesParsed
import io.github.mudrichenkoevgeny.backend.core.common.testing.contract.assertExhaustiveFilterKeysParsed
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSessionFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSessionSortBy
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

private const val TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000"
private const val TEST_IDENTIFIER_ID = "123e4567-e89b-12d3-a456-426614174000"
private const val TEST_IDENTIFIER = "test@example.com"
private const val TEST_IP = "127.0.0.1"
private const val INVALID_VAL = "invalid"

class ManagementSessionListQueryParameterParserTest {

    private val call = mockk<ApplicationCall>()
    private val request = mockk<ApplicationRequest>()
    private val filterNames = UserSessionFilterValues

    @BeforeEach
    fun setUp() {
        every { call.request } returns request
    }

    @Test
    fun `parseManagementSessionsListQueryParams should extract and parse all wire keys defined in UserSessionFilterValues`() {
        assertExhaustiveFilterKeysParsed(
            filterKeysClass = UserSessionFilterValues::class,
            sampleValueProvider = ::getSampleValidValueForSessionFilterKey,
            parseFunction = { mockCall -> mockCall.parseManagementSessionsListQueryParams() }
        )
    }

    @Test
    fun `parseManagementSessionsListQueryParams should accept and parse all UserSessionSortBy wire values`() {
        assertAllSortByEnumEntriesParsed(
            enumEntries = UserSessionSortBy.entries,
            wireValueExtractor = { sortByEntry -> sortByEntry.value },
            parseFunction = { mockCall -> mockCall.parseManagementSessionsListQueryParams() },
            assertSortByFunction = { parsedQueryParams, expectedSortBy ->
                assertEquals(expectedSortBy, parsedQueryParams.listing.sortBy)
            }
        )
    }

    private fun getSampleValidValueForSessionFilterKey(key: String): String = when {
        key == filterNames.USER_ROLE || key == filterNames.EXCLUDE_USER_ROLE -> "user"
        key == filterNames.IDENTIFIER_AUTH_PROVIDER || key == filterNames.EXCLUDE_IDENTIFIER_AUTH_PROVIDER -> "google"
        key == filterNames.CLIENT_TYPE || key == filterNames.EXCLUDE_CLIENT_TYPE -> "android"
        key.endsWith("_from") || key.endsWith("_to") -> "2026-01-01T00:00:00Z"
        key.startsWith("is_") -> "true"
        else -> "sample_value"
    }

    @Test
    fun `should parse valid session query parameters successfully`() {
        setupMockParameters(
            mapOf(
                filterNames.USER_ID to listOf(TEST_USER_ID),
                filterNames.USER_ROLE to listOf("user"),
                filterNames.IDENTIFIER_ID to listOf(TEST_IDENTIFIER_ID),
                filterNames.IDENTIFIER_AUTH_PROVIDER to listOf("google"),
                filterNames.CLIENT_TYPE to listOf("android"),
                filterNames.IDENTIFIER to listOf(TEST_IDENTIFIER),
                filterNames.IP_ADDRESS to listOf(TEST_IP)
            )
        )

        val result = call.parseManagementSessionsListQueryParams()

        assertEquals(TEST_USER_ID, result.userIds.first())
        assertEquals(UserRole.USER, result.userRoles.first())
        assertEquals(TEST_IDENTIFIER_ID, result.identifierIds.first())
        assertEquals(UserAuthProvider.GOOGLE, result.identifierAuthProviders.first())
        assertEquals(ClientType.ANDROID, result.clientTypes.first())
        assertEquals(TEST_IDENTIFIER, result.identifier)
        assertEquals(TEST_IP, result.ipAddress)
        assertEquals(UserSessionSortBy.DEFAULT, result.listing.sortBy)
    }

    @Test
    fun `should return empty userIds when user id parameter is missing`() {
        setupMockParameters(emptyMap())

        val result = call.parseManagementSessionsListQueryParams()

        assertTrue(result.userIds.isEmpty())
    }

    @Test
    fun `should throw RequestHandlingException when user role is invalid`() {
        setupMockParameters(
            mapOf(
                filterNames.USER_ROLE to listOf(INVALID_VAL)
            )
        )

        val exception = assertThrows<RequestHandlingException> {
            call.parseManagementSessionsListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(filterNames.USER_ROLE, error.parameterName)
    }

    @Test
    fun `should throw RequestHandlingException when auth provider is invalid`() {
        setupMockParameters(
            mapOf(
                filterNames.IDENTIFIER_AUTH_PROVIDER to listOf(INVALID_VAL)
            )
        )

        val exception = assertThrows<RequestHandlingException> {
            call.parseManagementSessionsListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(filterNames.IDENTIFIER_AUTH_PROVIDER, error.parameterName)
    }

    @Test
    fun `should throw RequestHandlingException when client type is invalid`() {
        setupMockParameters(
            mapOf(
                filterNames.CLIENT_TYPE to listOf(INVALID_VAL)
            )
        )

        val exception = assertThrows<RequestHandlingException> {
            call.parseManagementSessionsListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(filterNames.CLIENT_TYPE, error.parameterName)
    }

    private fun setupMockParameters(params: Map<String, List<String>>) {
        val parameters = io.ktor.http.Parameters.build {
            params.forEach { (key, values) -> appendAll(key, values) }
        }
        every { request.queryParameters } returns parameters
    }
}
