package io.github.mudrichenkoevgeny.backend.feature.user.route.open.session

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validatePathParameter
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.query.parseSelfSessionsListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session.DeleteAllOtherSessionsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session.DeleteSessionUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session.GetSessionUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session.GetSessionsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session.LogoutUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session.ReauthenticateSessionUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.mapper.pagedresult.mapItems
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.verifytotp.VerifyTotpPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.toUserSessionIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.session.toDeletedSessionsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.session.toUserSessionPrivatePayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.session.toUserSessionSummaryPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiPaths
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.session.DeletedSessionsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.session.UserSessionPrivatePayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.session.UserSessionSummaryPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.open.user.session.OpenSessionRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Router managing user session lifecycles and security trust levels.
 *
 * This component provides authenticated endpoints for session introspection and
 * management. It supports listing active sessions, terminating specific or all
 * other sessions, and performing MFA-based re-authentication to elevate session
 * trust (step-up authentication).
 *
 * Registered routes:
 * 1. [OpenSessionRoutes.GET_SESSIONS] — Paginated list of active user sessions via [GetSessionsUseCase].
 * 2. [OpenSessionRoutes.GET_SESSION] — Individual session details via [GetSessionUseCase].
 * 3. [OpenSessionRoutes.LOGOUT] — Termination of the current session via [LogoutUseCase].
 * 4. [OpenSessionRoutes.DELETE_SESSION] — Termination of a specific session via [DeleteSessionUseCase].
 * 5. [OpenSessionRoutes.DELETE_ALL_OTHER_SESSIONS] — Bulk termination via [DeleteAllOtherSessionsUseCase].
 * 6. [OpenSessionRoutes.REAUTHENTICATE_SESSION] — MFA verification via [ReauthenticateSessionUseCase].
 */
@Singleton
class OpenSessionRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val getSessionsUseCase: GetSessionsUseCase,
    private val getSessionUseCase: GetSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val deleteAllOtherSessionsUseCase: DeleteAllOtherSessionsUseCase,
    private val reauthenticateSessionUseCase: ReauthenticateSessionUseCase
) : BaseRouter {
    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerGetSessionsRoute(this)
            registerGetSessionRoute(this)
            registerLogoutRoute(this)
            registerDeleteSessionRoute(this)
            registerDeleteAllOtherSessionsRoute(this)
            registerReauthenticateSessionRoute(this)
        }
    }

    private fun registerLogoutRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = OpenSessionRoutes.LOGOUT,
            builder = { logoutDocs(allowedRoles, allowedAccountStatuses) },
            body = { logout(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerGetSessionsRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.get(
            path = OpenSessionRoutes.GET_SESSIONS,
            builder = { getSessionsDocs(allowedRoles, allowedAccountStatuses) },
            body = { getSessions(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerGetSessionRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.get(
            path = OpenSessionRoutes.GET_SESSION,
            builder = { getSessionDocs(allowedRoles, allowedAccountStatuses) },
            body = { getSession(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerDeleteSessionRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.delete(
            path = OpenSessionRoutes.DELETE_SESSION,
            builder = { deleteSessionDocs(allowedRoles, allowedAccountStatuses) },
            body = { deleteSession(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerDeleteAllOtherSessionsRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.delete(
            path = OpenSessionRoutes.DELETE_ALL_OTHER_SESSIONS,
            builder = { deleteAllOtherSessionsDocs(allowedRoles, allowedAccountStatuses) },
            body = { deleteAllOtherSessions(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerReauthenticateSessionRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.post(
            path = OpenSessionRoutes.REAUTHENTICATE_SESSION,
            builder = { reauthenticateSessionDocs(allowedRoles, allowedAccountStatuses) },
            body = { reauthenticateSession(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.logoutDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = LOGOUT_ROUTE_SUMMARY
        operationId = LOGOUT_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + UserSwaggerTags.SESSION)

        description = getFormattedDescription(
            description = LOGOUT_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = false
        )

        response {
            code(HttpStatusCode.OK) {
                description = LOGOUT_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.logout(
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
                actionType = UserAuditActionType.LOGOUT,
                resourceId = authenticatedRequestContext.sessionId.asHexDashString(),
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = logoutUseCase(
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun RouteConfig.getSessionsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_SESSIONS_ROUTE_SUMMARY
        operationId = GET_SESSIONS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + UserSwaggerTags.SESSION)

        description = getFormattedDescription(
            description = GET_SESSIONS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = false
        )

        response {
            code(HttpStatusCode.OK) {
                body<PagedResult<UserSessionSummaryPayload>>()
                description = GET_SESSIONS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getSessions(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val queryParams = call.parseSelfSessionsListQueryParams()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = getSessionsUseCase(
            authenticatedRequestContext = authenticatedRequestContext,
            pageParams = queryParams.listing.pageParams,
            sortBy = queryParams.listing.sortBy,
            sortOrder = queryParams.listing.sortOrder,
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
            operationSystemVersions = queryParams.operationSystemVersions
        )

        call.respondResult(result, appLogger, appErrorParser) { pagedSessions ->
            pagedSessions.mapItems { userSessionSummary -> userSessionSummary.toUserSessionSummaryPayload() }
        }
    }

    private fun RouteConfig.getSessionDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_SESSION_ROUTE_SUMMARY
        operationId = GET_SESSION_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + UserSwaggerTags.SESSION)

        description = getFormattedDescription(
            description = GET_SESSION_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = false
        )

        request {
            pathParameter<String>(UserApiPaths.SESSION_ID) {
                description = GET_SESSION_ROUTE_PATH_PARAMETER_ID_DESCRIPTION
            }
        }

        response {
            code(HttpStatusCode.OK) {
                body<UserSessionPrivatePayload>()
                description = GET_SESSION_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getSession(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val sessionId = call.validatePathParameter(UserApiPaths.SESSION_ID) { id ->
            id.toUserSessionIdOrThrow()
        }

        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = getSessionUseCase(
            sessionId = sessionId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { userSessionPrivate ->
            userSessionPrivate.toUserSessionPrivatePayload()
        }
    }

    private fun RouteConfig.deleteSessionDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = DELETE_SESSION_ROUTE_SUMMARY
        operationId = DELETE_SESSION_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + UserSwaggerTags.SESSION)

        description = getFormattedDescription(
            description = DELETE_SESSION_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = false
        )

        request {
            pathParameter<String>(UserApiPaths.SESSION_ID) {
                description = DELETE_SESSION_ROUTE_PATH_PARAMETER_ID_DESCRIPTION
            }
        }

        response {
            code(HttpStatusCode.OK) {
                description = DELETE_SESSION_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.deleteSession(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val sessionId = call.validatePathParameter(UserApiPaths.SESSION_ID) { id ->
            id.toUserSessionIdOrThrow()
        }

        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = UserAuditActionType.SELF_DELETE_SESSION,
                resourceId = sessionId.asHexDashString(),
                error = authorizeResult.error,
                extraMetadata = setOf(
                    AuditEventMetadata(
                        key = UserAuditMetadataKey.USER_ID,
                        value = authenticatedRequestContext.userId.asHexDashString()
                    )
                )
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = deleteSessionUseCase(
            sessionId = sessionId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun RouteConfig.deleteAllOtherSessionsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = DELETE_ALL_OTHER_SESSIONS_ROUTE_SUMMARY
        operationId = DELETE_ALL_OTHER_SESSIONS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + UserSwaggerTags.SESSION)

        description = getFormattedDescription(
            description = DELETE_ALL_OTHER_SESSIONS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = false
        )

        response {
            code(HttpStatusCode.OK) {
                body<DeletedSessionsPayload>()
                description = DELETE_ALL_OTHER_SESSIONS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.deleteAllOtherSessions(
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
                actionType = UserAuditActionType.SELF_DELETE_OTHER_SESSIONS,
                resourceId = null,
                error = authorizeResult.error,
                extraMetadata = setOf(
                    AuditEventMetadata(
                        key = UserAuditMetadataKey.USER_ID,
                        value = authenticatedRequestContext.userId.asHexDashString()
                    )
                )
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = deleteAllOtherSessionsUseCase(
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { deletedSessions ->
            deletedSessions.toDeletedSessionsPayload()
        }
    }

    private fun RouteConfig.reauthenticateSessionDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = REAUTHENTICATE_SESSION_ROUTE_SUMMARY
        operationId = REAUTHENTICATE_SESSION_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + UserSwaggerTags.SESSION)

        description = getFormattedDescription(
            description = REAUTHENTICATE_SESSION_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = false
        )

        request {
            body<VerifyTotpPayload>()
        }

        response {
            code(HttpStatusCode.OK) {
                description = REAUTHENTICATE_SESSION_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.reauthenticateSession(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val payload = call.receive<VerifyTotpPayload>()
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = UserAuditActionType.REAUTHENTICATE_SESSION,
                resourceId = authenticatedRequestContext.sessionId.asHexDashString(),
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = reauthenticateSessionUseCase(
            authenticatedRequestContext = authenticatedRequestContext,
            mfaToken = payload.mfaToken,
            code = payload.code
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun logErrorToAudit(
        authenticatedRequestContext: AuthenticatedRequestContext,
        actionType: AuditActionType,
        resourceId: String?,
        error: AppError,
        extraMetadata: Set<AuditEventMetadata> = emptySet()
    ) {
        val errorData = auditErrorConverter.convert(error)
        val metadata = authenticatedRequestContext.clientInfo.toAuditMetadata() + errorData.metadata + extraMetadata

        auditLogger.log(
            actorId = authenticatedRequestContext.userId.asHexDashString(),
            actorType = AuditActorType.USER,
            actorUserRole = authenticatedRequestContext.userRole.value,
            action = actionType,
            resource = UserAuditResourceType.SESSION,
            resourceId = resourceId,
            status = AuditStatus.DENIED,
            metadata = metadata
        )
    }

    companion object {
        const val LOGOUT_ROUTE_SUMMARY = "Logout current user"
        const val LOGOUT_ROUTE_DESCRIPTION = "Terminates the current active session."
        const val LOGOUT_ROUTE_OPERATION_ID = "openLogout"
        const val LOGOUT_ROUTE_RESPONSE_OK_DESCRIPTION = "Logged out successfully"

        const val GET_SESSIONS_ROUTE_SUMMARY = "List active user sessions"
        const val GET_SESSIONS_ROUTE_DESCRIPTION = "Retrieves a paginated list of active sessions."
        const val GET_SESSIONS_ROUTE_OPERATION_ID = "openGetSessions"
        const val GET_SESSIONS_ROUTE_RESPONSE_OK_DESCRIPTION = "Active sessions retrieved successfully"

        const val GET_SESSION_ROUTE_SUMMARY = "Get active user session"
        const val GET_SESSION_ROUTE_DESCRIPTION = "Retrieves details of a specific active session."
        const val GET_SESSION_ROUTE_OPERATION_ID = "openGetSession"
        const val GET_SESSION_ROUTE_PATH_PARAMETER_ID_DESCRIPTION = "The ID of the session to retrieve."
        const val GET_SESSION_ROUTE_RESPONSE_OK_DESCRIPTION = "Session details retrieved successfully"

        const val DELETE_SESSION_ROUTE_SUMMARY = "Delete active user session"
        const val DELETE_SESSION_ROUTE_DESCRIPTION = "Terminates a specific active session by ID."
        const val DELETE_SESSION_ROUTE_OPERATION_ID = "openDeleteSession"
        const val DELETE_SESSION_ROUTE_PATH_PARAMETER_ID_DESCRIPTION = "The ID of the session to delete."
        const val DELETE_SESSION_ROUTE_RESPONSE_OK_DESCRIPTION = "Session deleted successfully"

        const val DELETE_ALL_OTHER_SESSIONS_ROUTE_SUMMARY = "Delete all other active user sessions"
        const val DELETE_ALL_OTHER_SESSIONS_ROUTE_DESCRIPTION = "Terminates all active sessions except the current one."
        const val DELETE_ALL_OTHER_SESSIONS_ROUTE_OPERATION_ID = "openDeleteAllOtherSessions"
        const val DELETE_ALL_OTHER_SESSIONS_ROUTE_RESPONSE_OK_DESCRIPTION = "All other sessions deleted successfully"

        const val REAUTHENTICATE_SESSION_ROUTE_SUMMARY = "Reauthenticate session with TOTP"
        const val REAUTHENTICATE_SESSION_ROUTE_DESCRIPTION = "Verifies TOTP code to confirm session security."
        const val REAUTHENTICATE_SESSION_ROUTE_OPERATION_ID = "openReauthenticateSession"
        const val REAUTHENTICATE_SESSION_ROUTE_RESPONSE_OK_DESCRIPTION = "Session reauthenticated successfully"
    }
}
