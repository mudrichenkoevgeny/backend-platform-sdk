package io.github.mudrichenkoevgeny.backend.feature.user.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.testing.contract.assertAllSortByEnumEntriesParsed
import io.github.mudrichenkoevgeny.backend.core.common.testing.contract.assertExhaustiveFilterKeysParsed
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserIdentifierFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserIdentifierSortBy
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val TEST_IDENTIFIER = "test@example.com"
private const val GOOGLE_PROVIDER = "google"
private const val INVALID_PROVIDER = "unknown"

class SelfIdentifierListQueryParameterParserTest {

    private val call = mockk<ApplicationCall>()
    private val request = mockk<ApplicationRequest>()
    private val filterNames = UserIdentifierFilterValues

    @BeforeEach
    fun setUp() {
        every { call.request } returns request
    }

    @Test
    fun `parseSelfIdentifiersListQueryParams should extract and parse all wire keys defined in UserIdentifierFilterValues`() {
        assertExhaustiveFilterKeysParsed(
            filterKeysClass = UserIdentifierFilterValues::class,
            sampleValueProvider = ::getSampleValidValueForIdentifierFilterKey,
            parseFunction = { mockCall -> mockCall.parseSelfIdentifiersListQueryParams() }
        )
    }

    @Test
    fun `parseSelfIdentifiersListQueryParams should accept and parse all UserIdentifierSortBy wire values`() {
        assertAllSortByEnumEntriesParsed(
            enumEntries = UserIdentifierSortBy.entries,
            wireValueExtractor = { sortByEntry -> sortByEntry.value },
            parseFunction = { mockCall -> mockCall.parseSelfIdentifiersListQueryParams() },
            assertSortByFunction = { parsedQueryParams, expectedSortBy ->
                assertEquals(expectedSortBy, parsedQueryParams.listing.sortBy)
            }
        )
    }

    private fun getSampleValidValueForIdentifierFilterKey(key: String): String = when {
        key == filterNames.USER_AUTH_PROVIDER -> UserAuthProvider.GOOGLE.value
        key.endsWith("_from") || key.endsWith("_to") -> "2026-01-01T00:00:00Z"
        key.startsWith("is_") -> "true"
        else -> "sample_value"
    }

    @Test
    fun `should parse valid query parameters successfully`() {
        setupMockParameters(
            mapOf(
                filterNames.USER_AUTH_PROVIDER to listOf(GOOGLE_PROVIDER),
                filterNames.IDENTIFIER to listOf(TEST_IDENTIFIER)
            )
        )

        val result = call.parseSelfIdentifiersListQueryParams()

        assertEquals(UserAuthProvider.GOOGLE, result.userAuthProviders.first())
        assertEquals(TEST_IDENTIFIER, result.identifier)
        assertEquals(UserIdentifierSortBy.DEFAULT, result.listing.sortBy)
    }

    @Test
    fun `should parse empty query parameters successfully`() {
        setupMockParameters(emptyMap())

        val result = call.parseSelfIdentifiersListQueryParams()

        assertTrue(result.userAuthProviders.isEmpty())
        assertTrue(result.identifier == null)
        assertEquals(UserIdentifierSortBy.DEFAULT, result.listing.sortBy)
    }

    @Test
    fun `should throw RequestHandlingException when auth provider is invalid`() {
        setupMockParameters(
            mapOf(
                filterNames.USER_AUTH_PROVIDER to listOf(INVALID_PROVIDER)
            )
        )

        val exception = assertThrows<RequestHandlingException> {
            call.parseSelfIdentifiersListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(filterNames.USER_AUTH_PROVIDER, error.parameterName)
    }

    private fun setupMockParameters(params: Map<String, List<String>>) {
        val parameters = io.ktor.http.Parameters.build {
            params.forEach { (key, values) -> appendAll(key, values) }
        }
        every { request.queryParameters } returns parameters
    }
}
