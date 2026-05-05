package io.github.mudrichenkoevgeny.backend.feature.audit.api.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.CompositeAuditActionTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.CompositeAuditResourceTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
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

class AuditEventsListQueryParamsParserTest {

    private val actionParser = mockk<CompositeAuditActionTypeParser>()
    private val resourceParser = mockk<CompositeAuditResourceTypeParser>()
    private val call = mockk<ApplicationCall>()
    private val request = mockk<ApplicationRequest>()
    private val params = AuditFilterValues.AuditEventFilterValues

    @BeforeEach
    fun setup() {
        every { call.request } returns request
    }

    private fun setupParameters(parameters: Parameters) {
        every { call.parameters } returns parameters
        every { request.queryParameters } returns parameters
    }

    @Test
    fun `parseAuditEventsListQueryParams returns default listing when no params provided`() {
        setupParameters(Parameters.Empty)

        val result = call.parseAuditEventsListQueryParams(actionParser, resourceParser)

        assertTrue(result.actorIds.isEmpty())
        assertTrue(result.actorTypes.isEmpty())
        assertTrue(result.actions.isEmpty())
        assertTrue(result.resources.isEmpty())
    }

    @Test
    fun `parseAuditEventsListQueryParams correctly parses all simple filter fields`() {
        val queryParameters = Parameters.build {
            append(params.ACTOR_ID, "user-123")
            append(params.ACTOR_TYPE, AuditActorType.USER.serialName)
            append(params.ACTOR_USER_ROLE, "admin")
            append(params.RESOURCE_ID, "res-456")
            append(params.STATUS, AuditStatus.SUCCESS.serialName)
            append(params.MESSAGE, "test-message")
        }
        setupParameters(queryParameters)

        val result = call.parseAuditEventsListQueryParams(actionParser, resourceParser)

        assertEquals(listOf("user-123"), result.actorIds)
        assertEquals(listOf(AuditActorType.USER), result.actorTypes)
        assertEquals(listOf("admin"), result.actorUserRoles)
        assertEquals(listOf("res-456"), result.resourceIds)
        assertEquals(listOf(AuditStatus.SUCCESS), result.statuses)
        assertEquals(listOf("test-message"), result.messages)
    }

    @Test
    fun `parseAuditEventsListQueryParams parses composite actions and resources`() {
        val mockAction = mockk<AuditActionType>()
        val mockResource = mockk<AuditResourceType>()

        every { actionParser.fromValueOrThrow("create_user") } returns mockAction
        every { resourceParser.fromValueOrThrow("user_profile") } returns mockResource

        val queryParameters = Parameters.build {
            append(params.ACTION, "create_user")
            append(params.RESOURCE, "user_profile")
        }
        setupParameters(queryParameters)

        val result = call.parseAuditEventsListQueryParams(actionParser, resourceParser)

        assertEquals(listOf(mockAction), result.actions)
        assertEquals(listOf(mockResource), result.resources)
    }

    @Test
    fun `parseAuditEventsListQueryParams throws RequestHandlingException on invalid actor type`() {
        val queryParameters = Parameters.build {
            append(params.ACTOR_TYPE, "INVALID_TYPE")
        }
        setupParameters(queryParameters)

        val exception = assertThrows<RequestHandlingException> {
            call.parseAuditEventsListQueryParams(actionParser, resourceParser)
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(params.ACTOR_TYPE, error.parameterName)
    }

    @Test
    fun `parseAuditEventsListQueryParams throws RequestHandlingException when action parser fails`() {
        every { actionParser.fromValueOrThrow("wrong_action") } throws RuntimeException("Parse error")

        val queryParameters = Parameters.build {
            append(params.ACTION, "wrong_action")
        }
        setupParameters(queryParameters)

        val exception = assertThrows<RequestHandlingException> {
            call.parseAuditEventsListQueryParams(actionParser, resourceParser)
        }

        val error = exception.error as CommonError.InvalidParameterValue
        assertEquals(params.ACTION, error.parameterName)
    }

    @Test
    fun `parseAuditEventsListQueryParams filters out blank values`() {
        val queryParameters = Parameters.build {
            appendAll(params.ACTOR_ID, listOf("id1", "", "   ", "id2"))
        }
        setupParameters(queryParameters)

        val result = call.parseAuditEventsListQueryParams(actionParser, resourceParser)

        assertEquals(listOf("id1", "id2"), result.actorIds)
    }
}