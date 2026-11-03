package io.github.mudrichenkoevgeny.backend.feature.user.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.testing.contract.assertAllSortByEnumEntriesParsed
import io.github.mudrichenkoevgeny.backend.core.common.testing.contract.assertExhaustiveFilterKeysParsed
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSessionFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSessionSortBy
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

class SelfSessionListQueryParameterParserTest {

    private val call = mockk<ApplicationCall>()
    private val request = mockk<ApplicationRequest>()
    private val filterNames = UserSessionFilterValues

    @BeforeEach
    fun setUp() {
        every { call.request } returns request
    }

    @Test
    fun `parseSelfSessionsListQueryParams should extract and parse all wire keys defined in UserSessionFilterValues`() {
        assertExhaustiveFilterKeysParsed(
            filterKeysClass = UserSessionFilterValues::class,
            sampleValueProvider = ::getSampleValidValueForSessionFilterKey,
            parseFunction = { mockCall -> mockCall.parseSelfSessionsListQueryParams() }
        )
    }

    @Test
    fun `parseSelfSessionsListQueryParams should accept and parse all UserSessionSortBy wire values`() {
        assertAllSortByEnumEntriesParsed(
            enumEntries = UserSessionSortBy.entries,
            wireValueExtractor = { sortByEntry -> sortByEntry.value },
            parseFunction = { mockCall -> mockCall.parseSelfSessionsListQueryParams() },
            assertSortByFunction = { parsedQueryParams, expectedSortBy ->
                assertEquals(expectedSortBy, parsedQueryParams.listing.sortBy)
            }
        )
    }

    private fun getSampleValidValueForSessionFilterKey(key: String): String = when {
        key == filterNames.IDENTIFIER_AUTH_PROVIDER || key == filterNames.EXCLUDE_IDENTIFIER_AUTH_PROVIDER -> "google"
        key == filterNames.CLIENT_TYPE || key == filterNames.EXCLUDE_CLIENT_TYPE -> "android"
        key == filterNames.SESSION_ID || key == filterNames.USER_ID || key == filterNames.IDENTIFIER_ID -> "00000000-0000-0000-0000-000000000001"
        key.endsWith("_from") || key.endsWith("_to") -> "2026-01-01T00:00:00Z"
        key.startsWith("is_") -> "true"
        else -> "sample_value"
    }

    @Test
    fun `should parse valid session query parameters successfully`() {
        setupMockParameters(
            mapOf(
                filterNames.IDENTIFIER_ID to listOf(TEST_IDENTIFIER_ID),
                filterNames.IDENTIFIER_AUTH_PROVIDER to listOf("google"),
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

        assertEquals(TEST_IDENTIFIER_ID, result.identifierIds.first())
        assertEquals(UserAuthProvider.GOOGLE, result.identifierAuthProviders.first())
        assertEquals(ClientType.ANDROID, result.clientTypes.first())
        assertEquals(TEST_IDENTIFIER, result.identifier)
        assertEquals(TEST_USER_AGENT, result.userAgent)
        assertEquals(TEST_IP, result.ipAddress)
        assertEquals(TEST_LANGUAGE, result.language)
        assertEquals(TEST_DEVICE_ID, result.deviceId)
        assertEquals(TEST_DEVICE_NAME, result.deviceName)
        assertEquals(TEST_APP_VERSION, result.appVersion)
        assertEquals(TEST_OS_VERSION, result.osVersion)
        assertEquals(UserSessionSortBy.DEFAULT, result.listing.sortBy)
    }

    @Test
    fun `should parse empty query parameters successfully`() {
        setupMockParameters(emptyMap())

        val result = call.parseSelfSessionsListQueryParams()

        assertTrue(result.identifierIds.isEmpty())
        assertTrue(result.identifierAuthProviders.isEmpty())
        assertTrue(result.clientTypes.isEmpty())
        assertTrue(result.identifier == null)
        assertTrue(result.userAgent == null)
        assertTrue(result.ipAddress == null)
        assertTrue(result.language == null)
        assertTrue(result.deviceId == null)
        assertTrue(result.deviceName == null)
        assertTrue(result.appVersion == null)
        assertTrue(result.osVersion == null)
        assertEquals(UserSessionSortBy.DEFAULT, result.listing.sortBy)
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
        setupMockParameters(mapOf(filterNames.IDENTIFIER_AUTH_PROVIDER to listOf(INVALID_VAL)))

        val exception = assertThrows<RequestHandlingException> {
            call.parseSelfSessionsListQueryParams()
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(filterNames.IDENTIFIER_AUTH_PROVIDER, error.parameterName)
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
