package io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.registration

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.registration.RegistrationByEmailUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.registration.SendRegistrationConfirmationToEmailUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.otpconfirmation.toOtpConfirmationPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.otpconfirmation.OtpConfirmationPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.data.toAuthDataPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.auth.data.AuthDataPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.registration.RegistrationByEmailRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.confirmation.SendConfirmationToEmailRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.open.auth.registration.OpenRegistrationRoutes
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
 * 1. [OpenRegistrationRoutes.REGISTRATION_BY_EMAIL] — creates a new user account using email, password, and a valid OTP via [RegistrationByEmailUseCase].
 * 2. [OpenRegistrationRoutes.SEND_REGISTRATION_CONFIRMATION_TO_EMAIL] — triggers the delivery of a registration verification code to the specified email via [SendRegistrationConfirmationToEmailUseCase].
 */
@Singleton
class OpenRegistrationRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val registrationByEmailUseCase: RegistrationByEmailUseCase,
    private val sendRegistrationConfirmationToEmailUseCase: SendRegistrationConfirmationToEmailUseCase
) : BaseRouter {
    override fun register(route: Route) {
        registerRegistrationByEmailRoute(route)
        registerSendRegistrationConfirmationToEmailRoute(route)
    }

    private fun registerRegistrationByEmailRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = OpenRegistrationRoutes.REGISTRATION_BY_EMAIL,
            builder = { registrationByEmailDocs(allowedRoles, allowedAccountStatuses) },
            body = { registrationByEmail() }
        )
    }

    private fun registerSendRegistrationConfirmationToEmailRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = OpenRegistrationRoutes.SEND_REGISTRATION_CONFIRMATION_TO_EMAIL,
            builder = { sendRegistrationConfirmationToEmailDocs(allowedRoles, allowedAccountStatuses) },
            body = { sendRegistrationConfirmationToEmail() }
        )
    }

    private fun RouteConfig.registrationByEmailDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = REGISTRATION_BY_EMAIL_ROUTE_SUMMARY
        operationId = REGISTRATION_BY_EMAIL_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = REGISTRATION_BY_EMAIL_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = true
        )
        request { body<RegistrationByEmailRequest>() }
        response {
            code(HttpStatusCode.OK) {
                body<AuthDataPayload>()
                description = REGISTRATION_BY_EMAIL_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.registrationByEmail() {
        val request = call.validateRequest<RegistrationByEmailRequest>()

        val result = registrationByEmailUseCase(
            email = request.email,
            password = request.password,
            confirmationCode = request.confirmationCode,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) { authData ->
            authData.toAuthDataPayload()
        }
    }

    private fun RouteConfig.sendRegistrationConfirmationToEmailDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = SEND_REGISTRATION_CONFIRMATION_TO_EMAIL_ROUTE_SUMMARY
        operationId = SEND_REGISTRATION_CONFIRMATION_TO_EMAIL_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = SEND_REGISTRATION_CONFIRMATION_TO_EMAIL_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = true
        )
        request { body<SendConfirmationToEmailRequest>() }
        response {
            code(HttpStatusCode.OK) {
                body<OtpConfirmationPayload>()
                description = SEND_REGISTRATION_CONFIRMATION_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.sendRegistrationConfirmationToEmail() {
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
        const val REGISTRATION_BY_EMAIL_ROUTE_SUMMARY = "Register user by email"
        const val REGISTRATION_BY_EMAIL_ROUTE_DESCRIPTION = "Registers a new user using email and password " +
                "after confirmation code validation."
        const val REGISTRATION_BY_EMAIL_ROUTE_OPERATION_ID = "openRegistrationByEmail"
        const val REGISTRATION_BY_EMAIL_ROUTE_RESPONSE_OK_DESCRIPTION = "Success. User registered."

        const val SEND_REGISTRATION_CONFIRMATION_TO_EMAIL_ROUTE_SUMMARY = "Send registration confirmation code to email"
        const val SEND_REGISTRATION_CONFIRMATION_TO_EMAIL_ROUTE_DESCRIPTION = "Sends a verification code to the email for registration purposes."
        const val SEND_REGISTRATION_CONFIRMATION_TO_EMAIL_ROUTE_OPERATION_ID = "openSendRegistrationConfirmationToEmail"
        const val SEND_REGISTRATION_CONFIRMATION_ROUTE_RESPONSE_OK_DESCRIPTION = "Success. Verification code sent."
    }
}