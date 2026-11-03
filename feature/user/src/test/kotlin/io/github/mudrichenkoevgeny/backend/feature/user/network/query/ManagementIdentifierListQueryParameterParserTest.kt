package io.github.mudrichenkoevgeny.backend.feature.user.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.testing.contract.assertAllSortByEnumEntriesParsed
import io.github.mudrichenkoevgeny.backend.core.common.testing.contract.assertExhaustiveFilterKeysParsed
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserIdentifierFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserIdentifierSortBy
import io.ktor.http.Parameters
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
private const val TEST_IDENTIFIER = "test@example.com"
private const val INVALID_UUID = "invalid-uuid"
private const val GOOGLE_PROVIDER = "google"
private const val INVALID_PROVIDER = "unknown"

class ManagementIdentifierListQueryParameterParserTest {

    private val call = mockk<ApplicationCall>()
    private val request = mockk<ApplicationRequest>()
    private val filterNames = UserIdentifierFilterValues

    @BeforeEach
    fun setUp() {
        every { call.request } returns request
    }

    @Test
    fun `parseManagementIdentifiersListQueryParams should extract and parse all wire keys defined in UserIdentifierFilterValues`() {
        assertExhaustiveFilterKeysParsed(
            filterKeysClass = UserIdentifierFilterValues::class,
            sampleValueProvider = ::getSampleValidValueForIdentifierFilterKey,
            parseFunction = { mockCall -> mockCall.parseManagementIdentifiersListQueryParams() }
        )
    }

    @Test
    fun `parseManagementIdentifiersListQueryParams should accept and parse all UserIdentifierSortBy wire values`() {
        assertAllSortByEnumEntriesParsed(
            enumEntries = UserIdentifierSortBy.entries,
            wireValueExtractor = { sortByEntry -> sortByEntry.value },
            parseFunction = { mockCall -> mockCall.parseManagementIdentifiersListQueryParams() },
            assertSortByFunction = { parsedQueryParams, expectedSortBy ->
                assertEquals(expectedSortBy, parsedQueryParams.listing.sortBy)
            }
        )
    }

    private fun getSampleValidValueForIdentifierFilterKey(key: String): String = when {
        key == filterNames.USER_AUTH_PROVIDER || key == filterNames.EXCLUDE_USER_AUTH_PROVIDER -> UserAuthProvider.GOOGLE.value
        key == filterNames.USER_ID || key == filterNames.IDENTIFIER_ID -> "00000000-0000-0000-0000-000000000001"
        key.endsWith("_from") || key.endsWith("_to") -> "2026-01-01T00:00:00Z"
        key.startsWith("is_") -> "true"
        else -> "sample_value"
    }

    @Test
    fun `should parse valid query parameters successfully`() {
        setupMockParameters(
            mapOf(
                filterNames.USER_ID to listOf(TEST_USER_ID),
                filterNames.USER_AUTH_PROVIDER to listOf(GOOGLE_PROVIDER),
                filterNames.IDENTIFIER to listOf(TEST_IDENTIFIER)
            )
        )

        val result = call.parseManagementIdentifiersListQueryParams()

        assertEquals(TEST_USER_ID, result.userIds.first())
        assertEquals(UserAuthProvider.GOOGLE, result.userAuthProviders.first())
        assertEquals(TEST_IDENTIFIER, result.identifier)
        assertEquals(UserIdentifierSortBy.DEFAULT, result.listing.sortBy)
    }

    @Test
    fun `should return empty userIds when user id parameter is missing`() {
        setupMockParameters(emptyMap())

        val result = call.parseManagementIdentifiersListQueryParams()

        assertTrue(result.userIds.isEmpty())
    }

    @Test
    fun `should throw RequestHandlingException when user id is invalid`() {
        setupMockParameters(mapOf(filterNames.USER_ID to listOf(INVALID_UUID)))
        assertThrows<RequestHandlingException> {
            call.parseManagementIdentifiersListQueryParams()
        }
    }

    @Test
    fun `should throw RequestHandlingException when auth provider is invalid`() {
        setupMockParameters(
            mapOf(
                filterNames.USER_ID to listOf(TEST_USER_ID),
                filterNames.USER_AUTH_PROVIDER to listOf(INVALID_PROVIDER)
            )
        )

        val exception = assertThrows<RequestHandlingException> {
            call.parseManagementIdentifiersListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(filterNames.USER_AUTH_PROVIDER, error.parameterName)
    }

    private fun setupMockParameters(params: Map<String, List<String>>) {
        val parameters = Parameters.build {
            params.forEach { (key, values) -> appendAll(key, values) }
        }
        every { request.queryParameters } returns parameters
    }
}
