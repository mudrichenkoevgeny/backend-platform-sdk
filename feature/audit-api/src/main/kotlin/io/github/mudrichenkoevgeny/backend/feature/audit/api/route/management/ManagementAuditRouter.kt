package io.github.mudrichenkoevgeny.backend.feature.audit.api.route.management

import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.pagination.mapItems
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validatePathParameter
import io.github.mudrichenkoevgeny.backend.feature.audit.api.route.AuditSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.audit.api.network.query.parseAuditEventsListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.audit.api.usecase.management.auditevent.GetAuditEventUseCase
import io.github.mudrichenkoevgeny.backend.feature.audit.api.usecase.management.auditevent.GetAuditEventsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.CompositeAuditActionTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.toAuditEventIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.CompositeAuditResourceTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditEventPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.network.contract.AuditApiPaths
import io.github.mudrichenkoevgeny.shared.foundation.feature.audit.api.network.route.management.ManagementAuditRoutes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management HTTP routes for reading audit events.
 *
 * Requires an authenticated staff or admin user. Access to event payloads is enforced inside
 * [GetAuditEventsUseCase] / [GetAuditEventUseCase] via permissions on the loaded user.
 * Denied authorization is returned to the client without writing audit records for these reads.
 */
@Singleton
class ManagementAuditRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val compositeAuditActionTypeParser: CompositeAuditActionTypeParser,
    private val compositeAuditResourceTypeParser: CompositeAuditResourceTypeParser,
    private val getAuditEventsUseCase: GetAuditEventsUseCase,
    private val getAuditEventUseCase: GetAuditEventUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.get(
            path = ManagementAuditRoutes.GET_AUDIT_EVENTS,
            builder = { getAuditEventsDocs() },
            body = { getAuditEvents() }
        )
        route.get(
            path = ManagementAuditRoutes.GET_AUDIT_EVENT,
            builder = { getAuditEventDocs() },
            body = { getAuditEvent() }
        )
    }

    private fun RouteConfig.getAuditEventsDocs() {
        summary = GET_AUDIT_EVENTS_ROUTE_SUMMARY
        description = GET_AUDIT_EVENTS_ROUTE_DESCRIPTION
        operationId = GET_AUDIT_EVENTS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, AuditSwaggerTags.AUDIT_EVENTS)
        response {
            code(HttpStatusCode.OK) {
                description = GET_AUDIT_EVENTS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getAuditEvents() {
        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN),
            requiredAccountStatus = setOf(UserAccountStatus.ACTIVE)
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val queryParams = call.parseAuditEventsListQueryParams(
            compositeAuditActionTypeParser = compositeAuditActionTypeParser,
            compositeAuditResourceTypeParser = compositeAuditResourceTypeParser,
        )

        val result = getAuditEventsUseCase(
            pageParams = queryParams.listing.pageParams,
            sortBy = queryParams.listing.sortBy,
            sortOrder = queryParams.listing.sortOrder,
            actorId = queryParams.actorId,
            actorType = queryParams.actorType,
            actorUserRole = queryParams.actorUserRole,
            action = queryParams.action,
            resource = queryParams.resource,
            resourceId = queryParams.resourceId,
            status = queryParams.status,
            message = queryParams.message,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { paged ->
            paged.mapItems { it.toAuditEventPayload() }
        }
    }

    private fun RouteConfig.getAuditEventDocs() {
        summary = GET_AUDIT_EVENT_ROUTE_SUMMARY
        description = GET_AUDIT_EVENT_ROUTE_DESCRIPTION
        operationId = GET_AUDIT_EVENT_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, AuditSwaggerTags.AUDIT_EVENTS)
        request {
            pathParameter<String>(AuditApiPaths.EVENT_ID) {
                description = GET_AUDIT_EVENT_ROUTE_PATH_PARAMETER_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = GET_AUDIT_EVENT_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getAuditEvent() {
        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN),
            requiredAccountStatus = setOf(UserAccountStatus.ACTIVE)
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val eventId = call.validatePathParameter(AuditApiPaths.EVENT_ID) { eventId ->
            eventId.toAuditEventIdOrThrow()
        }

        val result = getAuditEventUseCase(
            auditEventId = eventId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { event ->
            event.toAuditEventPayload()
        }
    }

    companion object {
        const val GET_AUDIT_EVENTS_ROUTE_SUMMARY = "List audit events"
        const val GET_AUDIT_EVENTS_ROUTE_DESCRIPTION =
            "Returns a paginated list of audit events. Rows and field masking depend on the caller's audit permissions."
        const val GET_AUDIT_EVENTS_ROUTE_OPERATION_ID = "getAuditEvents"
        const val GET_AUDIT_EVENTS_ROUTE_RESPONSE_OK_DESCRIPTION = "Paged audit events"

        const val GET_AUDIT_EVENT_ROUTE_SUMMARY = "Get audit event by id"
        const val GET_AUDIT_EVENT_ROUTE_DESCRIPTION =
            "Returns a single audit event. Payload masking depends on the caller's audit permissions."
        const val GET_AUDIT_EVENT_ROUTE_OPERATION_ID = "getAuditEvent"
        const val GET_AUDIT_EVENT_ROUTE_PATH_PARAMETER_ID_DESCRIPTION =
            "Audit event id (UUID string, hex with dashes)"
        const val GET_AUDIT_EVENT_ROUTE_RESPONSE_OK_DESCRIPTION = "Audit event"
    }
}
