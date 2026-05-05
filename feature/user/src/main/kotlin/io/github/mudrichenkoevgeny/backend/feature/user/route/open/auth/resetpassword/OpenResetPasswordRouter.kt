package io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.resetpassword

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.resetpassword.ResetPasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.resetpassword.SendResetPasswordConfirmationUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.otpconfirmation.toOtpConfirmationPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.password.ResetPasswordRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.password.SendResetPasswordConfirmationRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.open.auth.resetpassword.OpenResetPasswordRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Public authentication routes for password recovery and reset.
 *
 * Registered routes:
 * 1. [OpenResetPasswordRoutes.SEND_RESET_EMAIL_PASSWORD_CONFIRMATION] — triggers a verification code delivery to the user's email via [SendResetPasswordConfirmationUseCase].
 * 2. [OpenResetPasswordRoutes.RESET_EMAIL_PASSWORD] — updates the account password using a valid confirmation code via [ResetPasswordUseCase].
 */
@Singleton
class OpenResetPasswordRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val sendResetPasswordConfirmationUseCase: SendResetPasswordConfirmationUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : BaseRouter {
    override fun register(route: Route) {
        registerSendResetPasswordConfirmationRoute(route)
        registerResetPasswordRoute(route)
    }

    private fun registerSendResetPasswordConfirmationRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = OpenResetPasswordRoutes.SEND_RESET_EMAIL_PASSWORD_CONFIRMATION,
            builder = { sendResetPasswordConfirmationDocs(allowedRoles, allowedAccountStatuses) },
            body = { sendResetPasswordConfirmation() }
        )
    }

    private fun registerResetPasswordRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = OpenResetPasswordRoutes.RESET_EMAIL_PASSWORD,
            builder = { resetPasswordDocs(allowedRoles, allowedAccountStatuses) },
            body = { resetPassword() }
        )
    }

    private fun RouteConfig.sendResetPasswordConfirmationDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = SEND_RESET_PASSWORD_CONFIRMATION_SUMMARY
        operationId = SEND_RESET_PASSWORD_CONFIRMATION_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)

        description = getFormattedDescription(
            description = SEND_RESET_PASSWORD_CONFIRMATION_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )

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

        val result = sendResetPasswordConfirmationUseCase(
            email = request.email,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) { otpConfirmation ->
            otpConfirmation.toOtpConfirmationPayload()
        }
    }

    private fun RouteConfig.resetPasswordDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = RESET_PASSWORD_SUMMARY
        operationId = RESET_PASSWORD_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)

        description = getFormattedDescription(
            description = RESET_PASSWORD_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )

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

        val result = resetPasswordUseCase(
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