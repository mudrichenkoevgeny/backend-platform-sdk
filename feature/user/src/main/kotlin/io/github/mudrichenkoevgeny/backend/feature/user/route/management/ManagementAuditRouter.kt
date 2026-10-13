package io.github.mudrichenkoevgeny.backend.feature.user.route.management

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validatePathParameter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.query.parseAuditEventsListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.route.AuditSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auditevent.GetAuditEventsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auditevent.GetAuditEventUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.CompositeAuditActionTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.toAuditEventIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.CompositeAuditResourceTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditEventPrivatePayload
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditEventSummaryPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.network.contract.AuditApiPaths
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.network.model.event.AuditEventPrivatePayload
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.network.model.event.AuditEventSummaryPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.mapper.pagedresult.mapItems
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.network.route.management.ManagementAuditRoutes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Router for administrative access to audit logs and security events.
 *
 * Registered routes:
 * 1. [ManagementAuditRoutes.GET_AUDIT_EVENTS] — retrieves paginated audit logs via [GetAuditEventsUseCase].
 * 2. [ManagementAuditRoutes.GET_AUDIT_EVENT] — retrieves a specific audit event via [GetAuditEventUseCase].
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
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerGetAuditEventsRoute(this)
            registerGetAuditEventRoute(this)
        }
    }

    private fun registerGetAuditEventsRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.get(
            path = ManagementAuditRoutes.GET_AUDIT_EVENTS,
            builder = { getAuditEventsDocs(allowedRoles, allowedAccountStatuses) },
            body = { getAuditEvents(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerGetAuditEventRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.get(
            path = ManagementAuditRoutes.GET_AUDIT_EVENT,
            builder = { getAuditEventDocs(allowedRoles, allowedAccountStatuses) },
            body = { getAuditEvent(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.getAuditEventsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_AUDIT_EVENTS_ROUTE_SUMMARY
        operationId = GET_AUDIT_EVENTS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + AuditSwaggerTags.AUDIT_EVENTS)

        description = getFormattedDescription(
            description = GET_AUDIT_EVENTS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = false
        )

        response {
            code(HttpStatusCode.OK) {
                body<PagedResult<AuditEventSummaryPayload>>()
                description = GET_AUDIT_EVENTS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getAuditEvents(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
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
            actorIds = queryParams.actorIds,
            actorTypes = queryParams.actorTypes,
            actorUserRoles = queryParams.actorUserRoles,
            actions = queryParams.actions,
            resources = queryParams.resources,
            resourceIds = queryParams.resourceIds,
            statuses = queryParams.statuses,
            messages = queryParams.messages,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { pagedAuditEvent ->
            pagedAuditEvent.mapItems { auditEventSummary ->
                auditEventSummary.toAuditEventSummaryPayload()
            }
        }
    }

    private fun RouteConfig.getAuditEventDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_AUDIT_EVENT_ROUTE_SUMMARY
        operationId = GET_AUDIT_EVENT_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + AuditSwaggerTags.AUDIT_EVENTS)

        description = getFormattedDescription(
            description = GET_AUDIT_EVENT_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = false
        )

        request {
            pathParameter<String>(AuditApiPaths.EVENT_ID) {
                description = GET_AUDIT_EVENT_ROUTE_PATH_PARAMETER_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.OK) {
                body<AuditEventPrivatePayload>()
                description = GET_AUDIT_EVENT_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getAuditEvent(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
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
            event.toAuditEventPrivatePayload()
        }
    }

    companion object {
        const val GET_AUDIT_EVENTS_ROUTE_SUMMARY = "List audit events"
        const val GET_AUDIT_EVENTS_ROUTE_DESCRIPTION =
            "Returns a paginated list of audit events. Rows and field masking depend on the caller's audit permissions."
        const val GET_AUDIT_EVENTS_ROUTE_OPERATION_ID = "getManagementAuditEvents"
        const val GET_AUDIT_EVENTS_ROUTE_RESPONSE_OK_DESCRIPTION = "Paged audit events"

        const val GET_AUDIT_EVENT_ROUTE_SUMMARY = "Get audit event by id"
        const val GET_AUDIT_EVENT_ROUTE_DESCRIPTION =
            "Returns a single audit event. Payload masking depends on the caller's audit permissions."
        const val GET_AUDIT_EVENT_ROUTE_OPERATION_ID = "getManagementAuditEvent"
        const val GET_AUDIT_EVENT_ROUTE_PATH_PARAMETER_ID_DESCRIPTION =
            "Audit event id (UUID string, hex with dashes)"
        const val GET_AUDIT_EVENT_ROUTE_RESPONSE_OK_DESCRIPTION = "Audit event"
    }
}
