package io.github.mudrichenkoevgeny.backend.feature.user.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000"
private const val TEST_IDENTIFIER_ID = "123e4567-e89b-12d3-a456-426614174000"
private const val TEST_IDENTIFIER = "test@example.com"
private const val TEST_IP = "127.0.0.1"
private const val INVALID_VAL = "invalid"
private const val ROLE_KEY = "user_role"

class ManagementSessionListQueryParameterExtensionsTest {

    private val call = mockk<ApplicationCall>()
    private val request = mockk<ApplicationRequest>()
    private val filterNames = UserFilterValues.UserSessionFilterValues

    @BeforeEach
    fun setUp() {
        every { call.request } returns request
    }

    @Test
    fun `should parse valid session query parameters successfully`() {
        setupMockParameters(
            mapOf(
                filterNames.USER_ID to listOf(TEST_USER_ID),
                ROLE_KEY to listOf("user"),
                filterNames.IDENTIFIER_ID to listOf(TEST_IDENTIFIER_ID),
                filterNames.USER_AUTH_PROVIDER to listOf("google"),
                filterNames.CLIENT_TYPE to listOf("android"),
                filterNames.IDENTIFIER to listOf(TEST_IDENTIFIER),
                filterNames.IP_ADDRESS to listOf(TEST_IP)
            )
        )

        val result = call.parseManagementSessionsListQueryParams()

        assertEquals(TEST_USER_ID, result.userIds.first().value.toString())
        assertEquals(UserRole.USER, result.userRoles.first())
        assertEquals(TEST_IDENTIFIER_ID, result.identifierIds.first().value.toString())
        assertEquals(UserAuthProvider.GOOGLE, result.identifierAuthProviders.first())
        assertEquals(ClientType.ANDROID, result.clientTypes.first())
        assertEquals(TEST_IDENTIFIER, result.identifiers.first())
        assertEquals(TEST_IP, result.ipAddresses.first())
        assertEquals(UserSortValues.UserSessionSortBy.CREATED_AT, result.listing.sortBy)
    }

    @Test
    fun `should throw RequestHandlingException when user id is missing`() {
        setupMockParameters(emptyMap())

        val exception = assertThrows<RequestHandlingException> {
            call.parseManagementSessionsListQueryParams()
        }

        val error = exception.error as CommonError.MissingRequiredParameter
        assertEquals(filterNames.USER_ID, error.parameterName)
    }

    @Test
    fun `should throw RequestHandlingException when user id is invalid`() {
        setupMockParameters(mapOf(filterNames.USER_ID to listOf(INVALID_VAL)))

        val exception = assertThrows<RequestHandlingException> {
            call.parseManagementSessionsListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(filterNames.USER_ID, error.parameterName)
    }

    @Test
    fun `should throw RequestHandlingException when user role is invalid`() {
        setupMockParameters(
            mapOf(
                filterNames.USER_ID to listOf(TEST_USER_ID),
                ROLE_KEY to listOf(INVALID_VAL)
            )
        )

        val exception = assertThrows<RequestHandlingException> {
            call.parseManagementSessionsListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(ROLE_KEY, error.parameterName)
    }

    @Test
    fun `should throw RequestHandlingException when identifier id is invalid`() {
        setupMockParameters(
            mapOf(
                filterNames.USER_ID to listOf(TEST_USER_ID),
                filterNames.IDENTIFIER_ID to listOf(INVALID_VAL)
            )
        )

        val exception = assertThrows<RequestHandlingException> {
            call.parseManagementSessionsListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(filterNames.IDENTIFIER_ID, error.parameterName)
    }

    @Test
    fun `should throw RequestHandlingException when auth provider is invalid`() {
        setupMockParameters(
            mapOf(
                filterNames.USER_ID to listOf(TEST_USER_ID),
                filterNames.USER_AUTH_PROVIDER to listOf(INVALID_VAL)
            )
        )

        val exception = assertThrows<RequestHandlingException> {
            call.parseManagementSessionsListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(filterNames.USER_AUTH_PROVIDER, error.parameterName)
    }

    @Test
    fun `should throw RequestHandlingException when client type is invalid`() {
        setupMockParameters(
            mapOf(
                filterNames.USER_ID to listOf(TEST_USER_ID),
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