package io.github.mudrichenkoevgeny.backend.feature.user.route.management.session

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.pagination.mapItems
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validatePathParameter
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.query.parseManagementSessionsListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session.ManagementDeleteAllSessionsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session.ManagementDeleteSessionUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session.ManagementGetSessionUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session.ManagementGetSessionsUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.toUserSessionIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.toUserIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.session.toUserSessionPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiPaths
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.management.session.ManagementSessionRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management HTTP routes for overseeing and terminating user sessions.
 *
 * Registered routes:
 * 1. [ManagementSessionRoutes.GET_SESSIONS] — retrieves a filtered list of user sessions via [ManagementGetSessionsUseCase].
 * 2. [ManagementSessionRoutes.GET_SESSION] — retrieves specific session details via [ManagementGetSessionUseCase].
 * 3. [ManagementSessionRoutes.DELETE_SESSION] — terminates a specific user session via [ManagementDeleteSessionUseCase].
 * 4. [ManagementSessionRoutes.DELETE_ALL_USER_SESSIONS] — terminates all active sessions for a user via [ManagementDeleteAllSessionsUseCase].
 */
@Singleton
class ManagementSessionRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val managementGetSessionsUseCase: ManagementGetSessionsUseCase,
    private val managementGetSessionUseCase: ManagementGetSessionUseCase,
    private val managementDeleteSessionUseCase: ManagementDeleteSessionUseCase,
    private val managementDeleteAllSessionsUseCase: ManagementDeleteAllSessionsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerGetSessionsRoute(this)
            registerGetSessionRoute(this)
            registerDeleteSessionRoute(this)
            registerDeleteAllUserSessionsRoute(this)
        }
    }

    private fun registerGetSessionsRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.get(
            path = ManagementSessionRoutes.GET_SESSIONS,
            builder = { getSessionsDocs(allowedRoles, allowedAccountStatuses) },
            body = { getSessions(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerGetSessionRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.get(
            path = ManagementSessionRoutes.GET_SESSION,
            builder = { getSessionDocs(allowedRoles, allowedAccountStatuses) },
            body = { getSession(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerDeleteSessionRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.delete(
            path = ManagementSessionRoutes.DELETE_SESSION,
            builder = { deleteSessionDocs(allowedRoles, allowedAccountStatuses) },
            body = { deleteSession(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerDeleteAllUserSessionsRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.delete(
            path = ManagementSessionRoutes.DELETE_ALL_USER_SESSIONS,
            builder = { deleteAllUserSessionsDocs(allowedRoles, allowedAccountStatuses) },
            body = { deleteAllUserSessions(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.getSessionsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_SESSIONS_ROUTE_SUMMARY
        operationId = GET_SESSIONS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.SESSION)
        description = getFormattedDescription(
            description = GET_SESSIONS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        response {
            code(HttpStatusCode.OK) {
                description = GET_SESSIONS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getSessions(
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
        val queryParams = call.parseManagementSessionsListQueryParams()

        val result = managementGetSessionsUseCase(
            pageParams = queryParams.listing.pageParams,
            sortBy = queryParams.listing.sortBy,
            sortOrder = queryParams.listing.sortOrder,
            userIds = queryParams.userIds,
            userRoles = queryParams.userRoles,
            identifiers = queryParams.identifiers,
            identifierIds = queryParams.identifierIds,
            identifierAuthProviders = queryParams.identifierAuthProviders,
            clientTypes = queryParams.clientTypes,
            userAgents = queryParams.userAgents,
            ipAddresses = queryParams.ipAddresses,
            languages = queryParams.languages,
            deviceIds = queryParams.deviceIds,
            deviceNames = queryParams.deviceNames,
            appVersions = queryParams.appVersions,
            operationSystemVersions = queryParams.operationSystemVersions,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { pagedSessions ->
            pagedSessions.mapItems { userSession -> userSession.toUserSessionPayload() }
        }
    }

    private fun RouteConfig.getSessionDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_SESSION_ROUTE_SUMMARY
        operationId = GET_SESSION_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.SESSION)
        description = getFormattedDescription(
            description = GET_SESSION_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request {
            pathParameter<String>(UserApiPaths.USER_ID) {
                description = GET_SESSION_ROUTE_PATH_USER_ID_DESCRIPTION
            }
            pathParameter<String>(UserApiPaths.SESSION_ID) {
                description = GET_SESSION_ROUTE_PATH_SESSION_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = GET_SESSION_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getSession(
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
        val userSessionId = call.validatePathParameter(UserApiPaths.SESSION_ID) { userSessionId ->
            userSessionId.toUserSessionIdOrThrow()
        }

        val result = managementGetSessionUseCase(
            userSessionId = userSessionId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { session ->
            session.toUserSessionPayload()
        }
    }

    private fun RouteConfig.deleteSessionDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = DELETE_SESSION_ROUTE_SUMMARY
        operationId = DELETE_SESSION_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.SESSION)
        description = getFormattedDescription(
            description = DELETE_SESSION_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request {
            pathParameter<String>(UserApiPaths.USER_ID) {
                description = DELETE_SESSION_ROUTE_PATH_USER_ID_DESCRIPTION
            }
            pathParameter<String>(UserApiPaths.SESSION_ID) {
                description = DELETE_SESSION_ROUTE_PATH_SESSION_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.NoContent) {
                description = DELETE_SESSION_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.deleteSession(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = UserAuditActionType.MANAGEMENT_DELETE_SESSION,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val userSessionId = call.validatePathParameter(UserApiPaths.SESSION_ID) { userSessionId ->
            userSessionId.toUserSessionIdOrThrow()
        }

        val result = managementDeleteSessionUseCase(
            userSessionId = userSessionId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun RouteConfig.deleteAllUserSessionsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = DELETE_ALL_USER_SESSIONS_ROUTE_SUMMARY
        operationId = DELETE_ALL_USER_SESSIONS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.SESSION)
        description = getFormattedDescription(
            description = DELETE_ALL_USER_SESSIONS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request {
            pathParameter<String>(UserApiPaths.USER_ID) {
                description = DELETE_ALL_USER_SESSIONS_ROUTE_PATH_USER_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.NoContent) {
                description = DELETE_ALL_USER_SESSIONS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.deleteAllUserSessions(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = UserAuditActionType.MANAGEMENT_DELETE_ALL_USER_SESSIONS,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val userId = call.validatePathParameter(UserApiPaths.USER_ID) { userId ->
            userId.toUserIdOrThrow()
        }

        val result = managementDeleteAllSessionsUseCase(
            userId = userId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun logErrorToAudit(
        authenticatedRequestContext: AuthenticatedRequestContext,
        actionType: AuditActionType,
        error: AppError
    ) {
        val errorData = auditErrorConverter.convert(error)
        val metadata = authenticatedRequestContext.clientInfo.toAuditMetadata() + errorData.metadata

        auditLogger.log(
            actorId = authenticatedRequestContext.userId.asHexDashString(),
            actorType = AuditActorType.USER,
            actorUserRole = authenticatedRequestContext.userRole.serialName,
            action = actionType,
            resource = UserAuditResourceType.SESSION,
            status = AuditStatus.DENIED,
            metadata = metadata
        )
    }

    companion object {
        const val GET_SESSIONS_ROUTE_SUMMARY = "List user sessions (management)"
        const val GET_SESSIONS_ROUTE_DESCRIPTION =
            "Returns a paginated list of user sessions. Row visibility and masking follow session permissions."
        const val GET_SESSIONS_ROUTE_OPERATION_ID = "getManagementUserSessions"
        const val GET_SESSIONS_ROUTE_RESPONSE_OK_DESCRIPTION = "Paged user sessions"

        const val GET_SESSION_ROUTE_SUMMARY = "Get user session (management)"
        const val GET_SESSION_ROUTE_DESCRIPTION = "Returns a single user session by user id and session id."
        const val GET_SESSION_ROUTE_OPERATION_ID = "getManagementUserSession"
        const val GET_SESSION_ROUTE_PATH_USER_ID_DESCRIPTION = "User id (UUID string, hex with dashes)"
        const val GET_SESSION_ROUTE_PATH_SESSION_ID_DESCRIPTION = "Session id (UUID string, hex with dashes)"
        const val GET_SESSION_ROUTE_RESPONSE_OK_DESCRIPTION = "User session"

        const val DELETE_SESSION_ROUTE_SUMMARY = "Delete user session (management)"
        const val DELETE_SESSION_ROUTE_DESCRIPTION = "Force-terminates the specified session for the specified user."
        const val DELETE_SESSION_ROUTE_OPERATION_ID = "deleteManagementUserSession"
        const val DELETE_SESSION_ROUTE_PATH_USER_ID_DESCRIPTION = "User id (UUID string, hex with dashes)"
        const val DELETE_SESSION_ROUTE_PATH_SESSION_ID_DESCRIPTION = "Session id (UUID string, hex with dashes)"
        const val DELETE_SESSION_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION = "Session terminated; no response body."

        const val DELETE_ALL_USER_SESSIONS_ROUTE_SUMMARY = "Delete all user sessions (management)"
        const val DELETE_ALL_USER_SESSIONS_ROUTE_DESCRIPTION = "Force-terminates all sessions for the specified user."
        const val DELETE_ALL_USER_SESSIONS_ROUTE_OPERATION_ID = "deleteManagementAllUserSessions"
        const val DELETE_ALL_USER_SESSIONS_ROUTE_PATH_USER_ID_DESCRIPTION = "User id (UUID string, hex with dashes)"
        const val DELETE_ALL_USER_SESSIONS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION =
            "All sessions terminated; no response body."
    }
}