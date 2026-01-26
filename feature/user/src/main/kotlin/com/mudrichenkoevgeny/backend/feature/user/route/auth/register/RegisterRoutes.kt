package com.mudrichenkoevgeny.backend.feature.user.route.auth.register

import com.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import com.mudrichenkoevgeny.backend.core.common.routing.respondResult
import com.mudrichenkoevgeny.backend.core.common.validation.validateRequest
import com.mudrichenkoevgeny.backend.feature.user.mapper.toResponse
import com.mudrichenkoevgeny.backend.feature.user.network.request.auth.register.RegisterByEmailRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.confirmation.SendConfirmationToEmailRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.context.getRequestContext
import com.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import com.mudrichenkoevgeny.backend.feature.user.route.auth.AuthRoutes
import com.mudrichenkoevgeny.backend.feature.user.usecase.auth.register.RegisterByEmailUseCase
import com.mudrichenkoevgeny.backend.feature.user.usecase.auth.register.SendRegistrationConfirmationToEmailUseCase
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

object RegisterRoutes {
    const val BASE_REGISTER_ROUTE = "${AuthRoutes.BASE_AUTH_ROUTE}/register"
    const val REGISTER_BY_EMAIL = "$BASE_REGISTER_ROUTE/email"
    const val BASE_REGISTER_CONFIRMATION_ROUTE = "${REGISTER_BY_EMAIL}/confirmation"
    const val SEND_REGISTER_CONFIRMATION_TO_EMAIL = "$BASE_REGISTER_CONFIRMATION_ROUTE/send-to-email"
}

@Singleton
class RegisterRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val registerByEmailUseCase: RegisterByEmailUseCase,
    private val sendRegistrationConfirmationToEmailUseCase: SendRegistrationConfirmationToEmailUseCase
) : BaseRouter {
    override fun register(route: Route) {
        route.post(
            path = RegisterRoutes.REGISTER_BY_EMAIL,
            builder = { registerByEmailDocs() },
            body = { registerByEmail() }
        )

        route.post(
            path = RegisterRoutes.SEND_REGISTER_CONFIRMATION_TO_EMAIL,
            builder = { sendRegisterConfirmationToEmailDocs() },
            body = { sendRegisterConfirmationToEmail() }
        )
    }

    private fun RouteConfig.registerByEmailDocs() {
        summary = REGISTER_BY_EMAIL_ROUTE_SUMMARY
        description = REGISTER_BY_EMAIL_ROUTE_DESCRIPTION
        operationId = REGISTER_BY_EMAIL_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        request {
            body<RegisterByEmailRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = REGISTER_BY_EMAIL_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.registerByEmail() {
        val request = call.validateRequest<RegisterByEmailRequest>()

        val result = registerByEmailUseCase.execute(
            email = request.email,
            password = request.password,
            confirmationCode = request.confirmationCode,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun RouteConfig.sendRegisterConfirmationToEmailDocs() {
        summary = SEND_REGISTER_CONFIRMATION_TO_EMAIL_ROUTE_SUMMARY
        description = SEND_REGISTER_CONFIRMATION_TO_EMAIL_ROUTE_DESCRIPTION
        operationId = SEND_REGISTER_CONFIRMATION_TO_EMAIL_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        request {
            body<SendConfirmationToEmailRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = SEND_REGISTER_CONFIRMATION_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.sendRegisterConfirmationToEmail() {
        val request = call.validateRequest<SendConfirmationToEmailRequest>()

        val result = sendRegistrationConfirmationToEmailUseCase.execute(
            email = request.email,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) {
                sendConfirmation -> sendConfirmation.toResponse()
        }
    }

    companion object {
        const val REGISTER_BY_EMAIL_ROUTE_SUMMARY = "Register user by email"
        const val REGISTER_BY_EMAIL_ROUTE_DESCRIPTION = "Registers a new user using email and password " +
                "after confirmation code validation."
        const val REGISTER_BY_EMAIL_ROUTE_OPERATION_ID = "registerByEmail"
        const val REGISTER_BY_EMAIL_ROUTE_RESPONSE_OK_DESCRIPTION = "Success. User registered."

        const val SEND_REGISTER_CONFIRMATION_TO_EMAIL_ROUTE_SUMMARY = "Send registration confirmation code to email"
        const val SEND_REGISTER_CONFIRMATION_TO_EMAIL_ROUTE_DESCRIPTION = "Sends a verification code to the email for registration purposes."
        const val SEND_REGISTER_CONFIRMATION_TO_EMAIL_ROUTE_OPERATION_ID = "sendRegisterConfirmationToEmail"
        const val SEND_REGISTER_CONFIRMATION_ROUTE_RESPONSE_OK_DESCRIPTION = "Success. Verification code sent."
    }
}