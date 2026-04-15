package io.github.mudrichenkoevgeny.backend.feature.user.route.management.session

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.pagination.mapItems
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validatePathParameter
import io.github.mudrichenkoevgeny.backend.feature.user.audit.toDeniedUserAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.network.query.parseSessionsListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session.ManagementDeleteAllUserSessionsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session.ManagementDeleteUserSessionUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session.ManagementGetUserSessionUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session.ManagementGetUserSessionsUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
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
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagementSessionRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val managementGetUserSessionsUseCase: ManagementGetUserSessionsUseCase,
    private val managementGetUserSessionUseCase: ManagementGetUserSessionUseCase,
    private val managementDeleteUserSessionUseCase: ManagementDeleteUserSessionUseCase,
    private val managementDeleteAllUserSessionsUseCase: ManagementDeleteAllUserSessionsUseCase
) : BaseRouter {
    override fun register(route: Route) {
        route.get(
            path = ManagementSessionRoutes.GET_USER_SESSIONS,
            builder = { getSessionsDocs() },
            body = { getSessions() }
        )
        route.get(
            path = ManagementSessionRoutes.GET_USER_SESSION,
            builder = { getSessionDocs() },
            body = { getSession() }
        )
        route.delete(
            path = ManagementSessionRoutes.DELETE_USER_SESSION,
            builder = { deleteSessionDocs() },
            body = { deleteSession() }
        )
        route.delete(
            path = ManagementSessionRoutes.DELETE_ALL_USER_SESSIONS,
            builder = { deleteAllUserSessionsDocs() },
            body = { deleteAllUserSessions() }
        )
    }

    private fun RouteConfig.getSessionsDocs() {
        summary = GET_SESSIONS_ROUTE_SUMMARY
        description = GET_SESSIONS_ROUTE_DESCRIPTION
        operationId = GET_SESSIONS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.SESSION)
        response {
            code(HttpStatusCode.OK) {
                description = GET_SESSIONS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getSessions() {
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
        val queryParams = call.parseSessionsListQueryParams()

        val result = managementGetUserSessionsUseCase(
            authenticatedRequestContext = authenticatedRequestContext,
            pageParams = queryParams.listing.pageParams,
            sortBy = queryParams.listing.sortBy,
            sortOrder = queryParams.listing.sortOrder,
            userIds = queryParams.userIds,
            identifiers = queryParams.identifiers,
            identifierIds = queryParams.identifierIds,
            identifierAuthProviders = queryParams.identifierAuthProviders,
            revokedValues = queryParams.revokedValues,
            clientTypes = queryParams.clientTypes,
            userAgents = queryParams.userAgents,
            ipAddresses = queryParams.ipAddresses,
            languages = queryParams.languages,
            deviceIds = queryParams.deviceIds,
            deviceNames = queryParams.deviceNames,
            appVersions = queryParams.appVersions,
            operationSystemVersions = queryParams.operationSystemVersions
        )

        call.respondResult(result, appLogger, appErrorParser) { paged ->
            paged.mapItems { it.toUserSessionPayload() }
        }
    }

    private fun RouteConfig.getSessionDocs() {
        summary = GET_SESSION_ROUTE_SUMMARY
        description = GET_SESSION_ROUTE_DESCRIPTION
        operationId = GET_SESSION_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.SESSION)
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

    private suspend fun RoutingContext.getSession() {
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
        val userId = call.validatePathParameter(UserApiPaths.USER_ID) { userId ->
            userId.toUserIdOrThrow()
        }
        val userSessionId = call.validatePathParameter(UserApiPaths.SESSION_ID) { userSessionId ->
            userSessionId.toUserSessionIdOrThrow()
        }

        val result = managementGetUserSessionUseCase(
            userId = userId,
            userSessionId = userSessionId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { session ->
            session.toUserSessionPayload()
        }
    }

    private fun RouteConfig.deleteSessionDocs() {
        summary = DELETE_SESSION_ROUTE_SUMMARY
        description = DELETE_SESSION_ROUTE_DESCRIPTION
        operationId = DELETE_SESSION_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.SESSION)
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

    private suspend fun RoutingContext.deleteSession() {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN),
            requiredAccountStatus = setOf(UserAccountStatus.ACTIVE)
        )

        if (authorizeResult is AppResult.Error) {
            val metadata: Set<AuditEventMetadata> = authorizeResult.error.toDeniedUserAuditEventMetadata() +
                    authenticatedRequestContext.clientInfo.toAuditMetadata()
            auditLogger.log(
                actorId = authenticatedRequestContext.userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = authenticatedRequestContext.userRole.serialName,
                action = UserAuditActionType.MANAGEMENT_DELETE_USER_SESSION,
                resource = UserAuditResourceType.USER_SESSION,
                status = AuditStatus.DENIED,
                metadata = metadata
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val userId = call.validatePathParameter(UserApiPaths.USER_ID) { userId ->
            userId.toUserIdOrThrow()
        }
        val userSessionId = call.validatePathParameter(UserApiPaths.SESSION_ID) { userSessionId ->
            userSessionId.toUserSessionIdOrThrow()
        }

        val result = managementDeleteUserSessionUseCase(
            userId = userId,
            userSessionId = userSessionId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun RouteConfig.deleteAllUserSessionsDocs() {
        summary = DELETE_ALL_USER_SESSIONS_ROUTE_SUMMARY
        description = DELETE_ALL_USER_SESSIONS_ROUTE_DESCRIPTION
        operationId = DELETE_ALL_USER_SESSIONS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.SESSION)
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

    private suspend fun RoutingContext.deleteAllUserSessions() {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN),
            requiredAccountStatus = setOf(UserAccountStatus.ACTIVE)
        )

        if (authorizeResult is AppResult.Error) {
            val metadata: Set<AuditEventMetadata> = authorizeResult.error.toDeniedUserAuditEventMetadata() +
                    authenticatedRequestContext.clientInfo.toAuditMetadata()
            auditLogger.log(
                actorId = authenticatedRequestContext.userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = authenticatedRequestContext.userRole.serialName,
                action = UserAuditActionType.MANAGEMENT_DELETE_ALL_USER_SESSIONS,
                resource = UserAuditResourceType.USER_SESSION,
                status = AuditStatus.DENIED,
                metadata = metadata
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val userId = call.validatePathParameter(UserApiPaths.USER_ID) { userId ->
            userId.toUserIdOrThrow()
        }

        val result = managementDeleteAllUserSessionsUseCase(
            userId = userId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
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