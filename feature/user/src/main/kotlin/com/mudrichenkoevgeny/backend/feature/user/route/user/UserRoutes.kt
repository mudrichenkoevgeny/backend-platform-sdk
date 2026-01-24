package com.mudrichenkoevgeny.backend.feature.user.route.user

import com.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.common.model.toUserIdOrThrow
import com.mudrichenkoevgeny.backend.core.common.network.constants.ApiFields
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import com.mudrichenkoevgeny.backend.core.common.routing.respondResult
import com.mudrichenkoevgeny.backend.core.common.validation.validatePathParameter
import com.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import com.mudrichenkoevgeny.backend.feature.user.mapper.user.toUserResponse
import com.mudrichenkoevgeny.backend.feature.user.network.request.context.getRequestContext
import com.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import com.mudrichenkoevgeny.backend.feature.user.usecase.user.DeleteUserUseCase
import com.mudrichenkoevgeny.backend.feature.user.usecase.user.GetUserUseCase
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

object UserRoutes {
    const val BASE_USER_ROUTE = "/user"
    const val GET_USER = BASE_USER_ROUTE
    const val DELETE_USER = "$BASE_USER_ROUTE/{id}"
}

@Singleton
class UserRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val getUserUseCase: GetUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase
) : BaseRouter {
    override fun register(route: Route) {
        route.get(
            path = UserRoutes.GET_USER,
            builder = { getUserDocs() },
            body = { getUser() }
        )

        route.delete(
            path = UserRoutes.DELETE_USER,
            builder = { deleteUserDocs() },
            body = { deleteUser() }
        )
    }

    private fun RouteConfig.getUserDocs() {
        summary = GET_USER_ROUTE_SUMMARY
        description = GET_USER_ROUTE_DESCRIPTION
        operationId = GET_USER_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.USER)
        response {
            code(HttpStatusCode.OK) {
                description = GET_USER_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getUser() {
        val requestContext = call.getRequestContext()

        val result = getUserUseCase.execute(
            userId = requestContext.userId
                ?: return call.respondResult(
                    AppResult.Error(UserError.InvalidAccessToken()),
                    appLogger,
                    appErrorParser
                ),
            requestContext = requestContext
        )

        call.respondResult(result, appLogger, appErrorParser) {
            userData -> userData.toUserResponse()
        }
    }

    private fun RouteConfig.deleteUserDocs() {
        summary = DELETE_USER_ROUTE_SUMMARY
        description = DELETE_USER_ROUTE_DESCRIPTION
        operationId = DELETE_USER_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.USER)
        request {
            pathParameter<String>(ApiFields.ID) {
                description = DELETE_USER_ROUTE_PATH_PARAMETER_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = DELETE_USER_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.deleteUser() {
        val userId = call.validatePathParameter(ApiFields.ID) { id ->
            id.toUserIdOrThrow()
        }

        val result = deleteUserUseCase.execute(
            userId = userId,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    companion object {
        const val GET_USER_ROUTE_SUMMARY = "Get current user"
        const val GET_USER_ROUTE_DESCRIPTION = "Returns information about the currently authenticated user."
        const val GET_USER_ROUTE_OPERATION_ID = "getUser"
        const val GET_USER_ROUTE_RESPONSE_OK_DESCRIPTION = "User data"

        const val DELETE_USER_ROUTE_SUMMARY = "Delete current user"
        const val DELETE_USER_ROUTE_DESCRIPTION = "Deletes the currently authenticated user account."
        const val DELETE_USER_ROUTE_OPERATION_ID = "deleteUser"
        const val DELETE_USER_ROUTE_PATH_PARAMETER_ID_DESCRIPTION = "ID of the user to delete"
        const val DELETE_USER_ROUTE_RESPONSE_OK_DESCRIPTION = "User deleted successfully"
    }
}