package com.mudrichenkoevgeny.backend.feature.user.route.auth.resetpassword

import com.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import com.mudrichenkoevgeny.backend.core.common.routing.respondResult
import com.mudrichenkoevgeny.backend.core.common.validation.validateRequest
import com.mudrichenkoevgeny.backend.feature.user.mapper.toResponse
import com.mudrichenkoevgeny.backend.feature.user.network.request.context.getRequestContext
import com.mudrichenkoevgeny.backend.feature.user.network.request.auth.password.ResetPasswordRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.auth.password.SendResetPasswordConfirmationRequest
import com.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import com.mudrichenkoevgeny.backend.feature.user.route.auth.AuthRoutes
import com.mudrichenkoevgeny.backend.feature.user.usecase.auth.password.ResetPasswordUseCase
import com.mudrichenkoevgeny.backend.feature.user.usecase.auth.password.SendResetPasswordConfirmationUseCase
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

object ResetPasswordRoutes {
    const val RESET_PASSWORD = "${AuthRoutes.BASE_AUTH_ROUTE}/reset-password"
    const val SEND_RESET_PASSWORD_CONFIRMATION = "$RESET_PASSWORD/send-confirmation"
}

@Singleton
class ResetPasswordRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val sendResetPasswordConfirmationUseCase: SendResetPasswordConfirmationUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : BaseRouter {
    override fun register(route: Route) {
        route.post(
            path = ResetPasswordRoutes.SEND_RESET_PASSWORD_CONFIRMATION,
            builder = { sendResetPasswordConfirmationDocs() },
            body = { sendResetPasswordConfirmation() }
        )

        route.post(
            path = ResetPasswordRoutes.RESET_PASSWORD,
            builder = { resetPasswordDocs() },
            body = { resetPassword() }
        )
    }

    private fun RouteConfig.sendResetPasswordConfirmationDocs() {
        summary = SEND_RESET_PASSWORD_CONFIRMATION_SUMMARY
        description = SEND_RESET_PASSWORD_CONFIRMATION_DESCRIPTION
        operationId = SEND_RESET_PASSWORD_CONFIRMATION_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        request {
            body<SendResetPasswordConfirmationRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = SEND_RESET_PASSWORD_CONFIRMATION_RESPONSE_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.sendResetPasswordConfirmation() {
        val request = call.validateRequest<SendResetPasswordConfirmationRequest>()

        val result = sendResetPasswordConfirmationUseCase.execute(
            email = request.email,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) {
                sendConfirmation -> sendConfirmation.toResponse()
        }
    }

    private fun RouteConfig.resetPasswordDocs() {
        summary = RESET_PASSWORD_SUMMARY
        description = RESET_PASSWORD_DESCRIPTION
        operationId = RESET_PASSWORD_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        request {
            body<ResetPasswordRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = RESET_PASSWORD_RESPONSE_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.resetPassword() {
        val request = call.validateRequest<ResetPasswordRequest>()

        val result = resetPasswordUseCase.execute(
            email = request.email,
            confirmationCode = request.confirmationCode,
            newPassword = request.newPassword,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    companion object {
        const val SEND_RESET_PASSWORD_CONFIRMATION_SUMMARY = "Send reset password confirmation"
        const val SEND_RESET_PASSWORD_CONFIRMATION_DESCRIPTION =
            "Sends a confirmation code to the user's email for reset password."
        const val SEND_RESET_PASSWORD_CONFIRMATION_OPERATION_ID = "sendResetPasswordConfirmation"
        const val SEND_RESET_PASSWORD_CONFIRMATION_RESPONSE_DESCRIPTION = "Confirmation sent successfully."

        const val RESET_PASSWORD_SUMMARY = "Reset password"
        const val RESET_PASSWORD_DESCRIPTION = "Resets the user's password using a confirmation code."
        const val RESET_PASSWORD_OPERATION_ID = "resetPassword"
        const val RESET_PASSWORD_RESPONSE_DESCRIPTION = "Reset password successfully."
    }
}