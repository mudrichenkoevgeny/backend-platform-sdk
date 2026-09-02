package io.github.mudrichenkoevgeny.backend.feature.user.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val TEST_IDENTIFIER_ID = "123e4567-e89b-12d3-a456-426614174000"
private const val TEST_IDENTIFIER = "test@example.com"
private const val TEST_IP = "127.0.0.1"
private const val TEST_USER_AGENT = "Mozilla/5.0"
private const val TEST_LANGUAGE = "en"
private const val TEST_DEVICE_ID = "device-123"
private const val TEST_DEVICE_NAME = "Pixel 8"
private const val TEST_APP_VERSION = "1.0.0"
private const val TEST_OS_VERSION = "Android 14"
private const val INVALID_VAL = "invalid"

class SelfSessionListQueryParameterExtensionsTest {

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
                filterNames.IDENTIFIER_ID to listOf(TEST_IDENTIFIER_ID),
                filterNames.USER_AUTH_PROVIDER to listOf("google"),
                filterNames.CLIENT_TYPE to listOf("android"),
                filterNames.IDENTIFIER to listOf(TEST_IDENTIFIER),
                filterNames.USER_AGENT to listOf(TEST_USER_AGENT),
                filterNames.IP_ADDRESS to listOf(TEST_IP),
                filterNames.LANGUAGE to listOf(TEST_LANGUAGE),
                filterNames.DEVICE_ID to listOf(TEST_DEVICE_ID),
                filterNames.DEVICE_NAME to listOf(TEST_DEVICE_NAME),
                filterNames.APP_VERSION to listOf(TEST_APP_VERSION),
                filterNames.OPERATION_SYSTEM_VERSION to listOf(TEST_OS_VERSION)
            )
        )

        val result = call.parseSelfSessionsListQueryParams()

        assertEquals(TEST_IDENTIFIER_ID, result.identifierIds.first().value.toString())
        assertEquals(UserAuthProvider.GOOGLE, result.identifierAuthProviders.first())
        assertEquals(ClientType.ANDROID, result.clientTypes.first())
        assertEquals(TEST_IDENTIFIER, result.identifiers.first())
        assertEquals(TEST_USER_AGENT, result.userAgents.first())
        assertEquals(TEST_IP, result.ipAddresses.first())
        assertEquals(TEST_LANGUAGE, result.languages.first())
        assertEquals(TEST_DEVICE_ID, result.deviceIds.first())
        assertEquals(TEST_DEVICE_NAME, result.deviceNames.first())
        assertEquals(TEST_APP_VERSION, result.appVersions.first())
        assertEquals(TEST_OS_VERSION, result.operationSystemVersions.first())
        assertEquals(UserSortValues.UserSessionSortBy.CREATED_AT, result.listing.sortBy)
    }

    @Test
    fun `should parse empty query parameters successfully`() {
        setupMockParameters(emptyMap())

        val result = call.parseSelfSessionsListQueryParams()

        assertTrue(result.identifierIds.isEmpty())
        assertTrue(result.identifierAuthProviders.isEmpty())
        assertTrue(result.clientTypes.isEmpty())
        assertTrue(result.identifiers.isEmpty())
        assertTrue(result.userAgents.isEmpty())
        assertTrue(result.ipAddresses.isEmpty())
        assertTrue(result.languages.isEmpty())
        assertTrue(result.deviceIds.isEmpty())
        assertTrue(result.deviceNames.isEmpty())
        assertTrue(result.appVersions.isEmpty())
        assertTrue(result.operationSystemVersions.isEmpty())
        assertEquals(UserSortValues.UserSessionSortBy.CREATED_AT, result.listing.sortBy)
    }

    @Test
    fun `should throw RequestHandlingException when identifier id is invalid`() {
        setupMockParameters(mapOf(filterNames.IDENTIFIER_ID to listOf(INVALID_VAL)))

        val exception = assertThrows<RequestHandlingException> {
            call.parseSelfSessionsListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(filterNames.IDENTIFIER_ID, error.parameterName)
    }

    @Test
    fun `should throw RequestHandlingException when auth provider is invalid`() {
        setupMockParameters(mapOf(filterNames.USER_AUTH_PROVIDER to listOf(INVALID_VAL)))

        val exception = assertThrows<RequestHandlingException> {
            call.parseSelfSessionsListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(filterNames.USER_AUTH_PROVIDER, error.parameterName)
    }

    @Test
    fun `should throw RequestHandlingException when client type is invalid`() {
        setupMockParameters(mapOf(filterNames.CLIENT_TYPE to listOf(INVALID_VAL)))

        val exception = assertThrows<RequestHandlingException> {
            call.parseSelfSessionsListQueryParams()
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