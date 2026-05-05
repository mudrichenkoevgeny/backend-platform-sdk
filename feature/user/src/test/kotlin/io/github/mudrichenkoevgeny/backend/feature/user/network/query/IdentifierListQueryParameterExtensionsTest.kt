package io.github.mudrichenkoevgeny.backend.feature.user.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000"
private const val TEST_IDENTIFIER = "test@example.com"
private const val INVALID_UUID = "invalid-uuid"
private const val GOOGLE_PROVIDER = "google"
private const val INVALID_PROVIDER = "unknown"

class ApplicationCallQueryParserTest {

    private val call = mockk<ApplicationCall>()
    private val request = mockk<ApplicationRequest>()
    private val filterNames = UserFilterValues.UserIdentifierFilterValues

    @BeforeEach
    fun setUp() {
        every { call.request } returns request
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

        val result = call.parseIdentifiersListQueryParams()

        assertEquals(TEST_USER_ID, result.userIds.first().value.toString())
        assertEquals(UserAuthProvider.GOOGLE, result.userAuthProviders.first())
        assertEquals(TEST_IDENTIFIER, result.identifiers.first())
        assertEquals(UserSortValues.UserIdentifierSortBy.CREATED_AT, result.listing.sortBy)
    }

    @Test
    fun `should throw RequestHandlingException when user id is missing`() {
        setupMockParameters(emptyMap())

        val exception = assertThrows<RequestHandlingException> {
            call.parseIdentifiersListQueryParams()
        }

        val error = exception.error as CommonError.MissingRequiredParameter
        assertEquals(filterNames.USER_ID, error.parameterName)
    }

    @Test
    fun `should throw RequestHandlingException when user id is invalid`() {
        setupMockParameters(mapOf(filterNames.USER_ID to listOf(INVALID_UUID)))

        val exception = assertThrows<RequestHandlingException> {
            call.parseIdentifiersListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(filterNames.USER_ID, error.parameterName)
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
            call.parseIdentifiersListQueryParams()
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