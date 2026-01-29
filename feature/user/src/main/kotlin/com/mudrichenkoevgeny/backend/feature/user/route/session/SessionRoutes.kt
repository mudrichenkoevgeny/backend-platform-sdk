package io.github.mudrichenkoevgeny.backend.feature.user.route.session

import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.model.toUserSessionIdOrThrow
import io.github.mudrichenkoevgeny.backend.core.common.network.constants.ApiFields
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.validation.validatePathParameter
import io.github.mudrichenkoevgeny.backend.feature.user.mapper.toResponse
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.session.DeleteAllOtherSessionsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.session.DeleteSessionUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.session.GetSessionsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.session.LogoutFromCurrentSessionUseCase
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

object SessionRoutes {
    const val BASE_SESSION_ROUTE = "/session"
    const val LOGOUT_ROUTE = "$BASE_SESSION_ROUTE/logout"
    const val GET_SESSIONS = BASE_SESSION_ROUTE
    const val DELETE_SESSION = "$BASE_SESSION_ROUTE/{id}"
    const val DELETE_ALL_OTHER_SESSIONS = "$BASE_SESSION_ROUTE/delete-others"
}

@Singleton
class SessionRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val getSessionsUseCase: GetSessionsUseCase,
    private val logoutFromCurrentSessionUseCase: LogoutFromCurrentSessionUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    private val deleteAllOtherSessionsUseCase: DeleteAllOtherSessionsUseCase
) : BaseRouter {
    override fun register(route: Route) {
        route.get(
            path = SessionRoutes.GET_SESSIONS,
            builder = { getSessionsDocs() },
            body = { getSessions() }
        )

        route.post(
            path = SessionRoutes.LOGOUT_ROUTE,
            builder = { logoutFromCurrentSessionDocs() },
            body = { logoutFromCurrentSession() }
        )

        route.delete(
            path = SessionRoutes.DELETE_SESSION,
            builder = { deleteSessionDocs() },
            body = { deleteSession() }
        )

        route.delete(
            path = SessionRoutes.DELETE_ALL_OTHER_SESSIONS,
            builder = { deleteAllOtherSessionsDocs() },
            body = { deleteAllOtherSessions() }
        )
    }

    private fun RouteConfig.getSessionsDocs() {
        summary = GET_SESSIONS_ROUTE_SUMMARY
        description = GET_SESSIONS_ROUTE_DESCRIPTION
        operationId = GET_SESSIONS_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.SESSION)
        response {
            code(HttpStatusCode.OK) {
                description = GET_SESSIONS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getSessions() {
        val result = getSessionsUseCase.execute(
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) {
            userSessions -> userSessions.map { userSession -> userSession.toResponse() }
        }
    }

    private fun RouteConfig.logoutFromCurrentSessionDocs() {
        summary = LOGOUT_FROM_CURRENT_SESSION_ROUTE_SUMMARY
        description = LOGOUT_FROM_CURRENT_SESSION_ROUTE_DESCRIPTION
        operationId = LOGOUT_FROM_CURRENT_SESSION_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        response {
            code(HttpStatusCode.NoContent) {
                description = LOGOUT_FROM_CURRENT_SESSION_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.logoutFromCurrentSession() {
        val result = logoutFromCurrentSessionUseCase.execute(
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun RouteConfig.deleteSessionDocs() {
        summary = DELETE_SESSION_ROUTE_SUMMARY
        description = DELETE_SESSION_ROUTE_DESCRIPTION
        operationId = DELETE_SESSION_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.SESSION)
        request {
            pathParameter<String>(ApiFields.ID) {
                description = DELETE_SESSION_ROUTE_PATH_PARAMETER_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = DELETE_SESSION_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.deleteSession() {
        val sessionId = call.validatePathParameter(ApiFields.ID) { id ->
            id.toUserSessionIdOrThrow()
        }

        val result = deleteSessionUseCase.execute(
            sessionId = sessionId,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun RouteConfig.deleteAllOtherSessionsDocs() {
        summary = DELETE_ALL_OTHER_SESSIONS_ROUTE_SUMMARY
        description = DELETE_ALL_OTHER_SESSIONS_ROUTE_DESCRIPTION
        operationId = DELETE_ALL_OTHER_SESSIONS_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.SESSION)
        response {
            code(HttpStatusCode.OK) {
                description = DELETE_ALL_OTHER_SESSIONS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.deleteAllOtherSessions() {
        val requestContext = call.getRequestContext()

        val result = deleteAllOtherSessionsUseCase.execute(
            requestContext = requestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    companion object {
        const val GET_SESSIONS_ROUTE_SUMMARY = "Get all active sessions"
        const val GET_SESSIONS_ROUTE_DESCRIPTION =
            "Returns a list of all active sessions for the current user, including device info and login method."
        const val GET_SESSIONS_ROUTE_OPERATION_ID = "getSessions"
        const val GET_SESSIONS_ROUTE_RESPONSE_OK_DESCRIPTION = "List of active sessions"

        const val LOGOUT_FROM_CURRENT_SESSION_ROUTE_SUMMARY = "Logout from current session"
        const val LOGOUT_FROM_CURRENT_SESSION_ROUTE_DESCRIPTION = "Terminates the current user session " +
                "and invalidates the associated authentication tokens."
        const val LOGOUT_FROM_CURRENT_SESSION_ROUTE_OPERATION_ID = "logoutFromCurrentSession"
        const val LOGOUT_FROM_CURRENT_SESSION_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION = "Success. Current session " +
                "has been terminated."

        const val DELETE_SESSION_ROUTE_SUMMARY = "Delete a specific session"
        const val DELETE_SESSION_ROUTE_DESCRIPTION =
            "Deletes a session by its ID. The current session cannot be deleted using this endpoint."
        const val DELETE_SESSION_ROUTE_OPERATION_ID = "deleteSession"
        const val DELETE_SESSION_ROUTE_PATH_PARAMETER_ID_DESCRIPTION = "ID of the session to delete"
        const val DELETE_SESSION_ROUTE_RESPONSE_OK_DESCRIPTION = "Session deleted successfully"

        private const val DELETE_ALL_OTHER_SESSIONS_ROUTE_SUMMARY = "Logout from all other sessions"
        private const val DELETE_ALL_OTHER_SESSIONS_ROUTE_DESCRIPTION = "Revokes all user sessions except for the current one."
        private const val DELETE_ALL_OTHER_SESSIONS_ROUTE_OPERATION_ID = "deleteAllOtherSessions"
        private const val DELETE_ALL_OTHER_SESSIONS_ROUTE_RESPONSE_OK_DESCRIPTION =
            "Success. All other sessions have been deleted."
    }
}