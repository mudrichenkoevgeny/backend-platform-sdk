package com.mudrichenkoevgeny.backend.feature.user.route.confirmation

import com.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import com.mudrichenkoevgeny.backend.core.common.routing.respondResult
import com.mudrichenkoevgeny.backend.core.common.validation.validateRequest
import com.mudrichenkoevgeny.backend.feature.user.mapper.toResponse
import com.mudrichenkoevgeny.backend.feature.user.network.request.confirmation.SendConfirmationToEmailRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.confirmation.SendConfirmationToPhoneRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.context.getRequestContext
import com.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import com.mudrichenkoevgeny.backend.feature.user.usecase.confirmation.SendConfirmationToEmailUseCase
import com.mudrichenkoevgeny.backend.feature.user.usecase.confirmation.SendConfirmationToPhoneUseCase
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

object ConfirmationRoutes {
    const val BASE_CONFIRMATION_ROUTE = "/confirmation"
    const val SEND_CONFIRMATION_TO_EMAIL = "$BASE_CONFIRMATION_ROUTE/send-to-email"
    const val SEND_CONFIRMATION_TO_PHONE = "$BASE_CONFIRMATION_ROUTE/send-to-phone"
}

@Singleton
class ConfirmationRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val sendConfirmationToEmailUseCase: SendConfirmationToEmailUseCase,
    private val sendConfirmationToPhoneUseCase: SendConfirmationToPhoneUseCase
) : BaseRouter {
    override fun register(route: Route) {
        route.post(
            path = ConfirmationRoutes.SEND_CONFIRMATION_TO_EMAIL,
            builder = { sendConfirmationToEmailDocs() },
            body = { sendConfirmationToEmail() }
        )

        route.post(
            path = ConfirmationRoutes.SEND_CONFIRMATION_TO_PHONE,
            builder = { sendConfirmationToPhoneDocs() },
            body = { sendConfirmationToPhone() }
        )
    }

    private fun RouteConfig.sendConfirmationToEmailDocs() {
        summary = SEND_CONFIRMATION_TO_EMAIL_ROUTE_SUMMARY
        description = SEND_CONFIRMATION_TO_EMAIL_ROUTE_DESCRIPTION
        operationId = SEND_CONFIRMATION_TO_EMAIL_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.CONFIRMATION)
        request {
            body<SendConfirmationToEmailRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = SEND_CONFIRMATION_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.sendConfirmationToEmail() {
        val request = call.validateRequest<SendConfirmationToEmailRequest>()

        val result = sendConfirmationToEmailUseCase.execute(
            email = request.email,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) {
            sendConfirmation -> sendConfirmation.toResponse()
        }
    }

    private fun RouteConfig.sendConfirmationToPhoneDocs() {
        summary = SEND_CONFIRMATION_TO_PHONE_ROUTE_SUMMARY
        description = SEND_CONFIRMATION_TO_PHONE_ROUTE_DESCRIPTION
        operationId = SEND_CONFIRMATION_TO_PHONE_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.CONFIRMATION)
        request {
            body<SendConfirmationToPhoneRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = SEND_CONFIRMATION_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.sendConfirmationToPhone() {
        val request = call.validateRequest<SendConfirmationToPhoneRequest>()

        val result = sendConfirmationToPhoneUseCase.execute(
            phoneNumber = request.phoneNumber,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) {
            sendConfirmation -> sendConfirmation.toResponse()
        }
    }

    companion object {
        const val SEND_CONFIRMATION_TO_EMAIL_ROUTE_SUMMARY = "Send confirmation code via email"
        const val SEND_CONFIRMATION_TO_EMAIL_ROUTE_DESCRIPTION = "Initiates email verification process. " +
                "Generates a code and sends it to the specified address."
        const val SEND_CONFIRMATION_TO_EMAIL_ROUTE_OPERATION_ID = "sendConfirmationToEmail"

        const val SEND_CONFIRMATION_TO_PHONE_ROUTE_SUMMARY = "Send confirmation code via phone"
        const val SEND_CONFIRMATION_TO_PHONE_ROUTE_DESCRIPTION = "Initiates phone verification process. " +
                "Generates a code and sends it to the specified phone number."
        const val SEND_CONFIRMATION_TO_PHONE_ROUTE_OPERATION_ID = "sendConfirmationToPhone"

        const val SEND_CONFIRMATION_ROUTE_RESPONSE_OK_DESCRIPTION = "Success. Verification code sent."
    }
}