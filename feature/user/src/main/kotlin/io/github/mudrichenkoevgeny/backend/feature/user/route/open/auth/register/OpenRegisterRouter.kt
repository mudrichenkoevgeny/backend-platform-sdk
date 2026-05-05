package io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.register

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.register.RegisterByEmailUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.register.SendRegistrationConfirmationToEmailUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.otpconfirmation.toOtpConfirmationPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.data.toAuthDataPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.register.RegisterByEmailRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.confirmation.SendConfirmationToEmailRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.open.auth.register.OpenRegisterRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Public authentication routes for new user registration.
 *
 * Registered routes:
 * 1. [OpenRegisterRoutes.REGISTER_BY_EMAIL] — creates a new user account using email, password, and a valid OTP via [RegisterByEmailUseCase].
 * 2. [OpenRegisterRoutes.SEND_REGISTER_CONFIRMATION_TO_EMAIL] — triggers the delivery of a registration verification code to the specified email via [SendRegistrationConfirmationToEmailUseCase].
 */
@Singleton
class OpenRegisterRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val registerByEmailUseCase: RegisterByEmailUseCase,
    private val sendRegistrationConfirmationToEmailUseCase: SendRegistrationConfirmationToEmailUseCase
) : BaseRouter {
    override fun register(route: Route) {
        registerRegisterByEmailRoute(route)
        registerSendRegisterConfirmationToEmailRoute(route)
    }

    private fun registerRegisterByEmailRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = OpenRegisterRoutes.REGISTER_BY_EMAIL,
            builder = { registerByEmailDocs(allowedRoles, allowedAccountStatuses) },
            body = { registerByEmail() }
        )
    }

    private fun registerSendRegisterConfirmationToEmailRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = OpenRegisterRoutes.SEND_REGISTER_CONFIRMATION_TO_EMAIL,
            builder = { sendRegisterConfirmationToEmailDocs(allowedRoles, allowedAccountStatuses) },
            body = { sendRegisterConfirmationToEmail() }
        )
    }

    private fun RouteConfig.registerByEmailDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = REGISTER_BY_EMAIL_ROUTE_SUMMARY
        operationId = REGISTER_BY_EMAIL_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = REGISTER_BY_EMAIL_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )
        request { body<RegisterByEmailRequest>() }
        response {
            code(HttpStatusCode.OK) {
                description = REGISTER_BY_EMAIL_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.registerByEmail() {
        val request = call.validateRequest<RegisterByEmailRequest>()

        val result = registerByEmailUseCase(
            email = request.email,
            password = request.password,
            confirmationCode = request.confirmationCode,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) { authData ->
            authData.toAuthDataPayload()
        }
    }

    private fun RouteConfig.sendRegisterConfirmationToEmailDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = SEND_REGISTER_CONFIRMATION_TO_EMAIL_ROUTE_SUMMARY
        operationId = SEND_REGISTER_CONFIRMATION_TO_EMAIL_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = SEND_REGISTER_CONFIRMATION_TO_EMAIL_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )
        request { body<SendConfirmationToEmailRequest>() }
        response {
            code(HttpStatusCode.OK) {
                description = SEND_REGISTER_CONFIRMATION_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.sendRegisterConfirmationToEmail() {
        val request = call.validateRequest<SendConfirmationToEmailRequest>()

        val result = sendRegistrationConfirmationToEmailUseCase(
            email = request.email,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) { otpConfirmation ->
            otpConfirmation.toOtpConfirmationPayload()
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