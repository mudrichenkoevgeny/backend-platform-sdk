package io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByEmailUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByExternalAuthProviderUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByPhoneUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByTotpRecoveryCodeUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByTotpUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.SendLoginConfirmationToPhoneUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.otpconfirmation.toOtpConfirmationPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.verifytotp.VerifyTotpPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.data.toAuthDataPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiFields
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.login.LoginByEmailRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.login.LoginByExternalAuthProviderRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.login.LoginByPhoneRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.confirmation.SendConfirmationToPhoneRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.open.auth.login.OpenLoginRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Public authentication routes for user login and MFA verification.
 *
 * Registered routes:
 * 1. [OpenLoginRoutes.LOGIN_BY_EMAIL] — authenticates via email/password using [LoginByEmailUseCase].
 * 2. [OpenLoginRoutes.LOGIN_BY_PHONE] — authenticates via phone/OTP using [LoginByPhoneUseCase].
 * 3. [OpenLoginRoutes.LOGIN_BY_EXTERNAL_AUTH_PROVIDER] — authenticates via OAuth/External tokens using [LoginByExternalAuthProviderUseCase].
 * 4. [OpenLoginRoutes.SEND_LOGIN_CONFIRMATION_TO_PHONE] — triggers OTP delivery to phone via [SendLoginConfirmationToPhoneUseCase].
 * 5. [OpenLoginRoutes.LOGIN_BY_TOTP] — completes MFA login with a TOTP code using [LoginByTotpUseCase].
 * 6. [OpenLoginRoutes.LOGIN_BY_TOTP_RECOVERY_CODE] — completes MFA login with a backup code using [LoginByTotpRecoveryCodeUseCase].
 */
@Singleton
class OpenLoginRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val loginByEmailUseCase: LoginByEmailUseCase,
    private val loginByPhoneUseCase: LoginByPhoneUseCase,
    private val loginByExternalAuthProviderUseCase: LoginByExternalAuthProviderUseCase,
    private val sendLoginConfirmationToPhoneUseCase: SendLoginConfirmationToPhoneUseCase,
    private val loginByTotpUseCase: LoginByTotpUseCase,
    private val loginByTotpRecoveryCodeUseCase: LoginByTotpRecoveryCodeUseCase
) : BaseRouter {

    override fun register(route: Route) {
        registerLoginByEmailRoute(route)
        registerLoginByPhoneRoute(route)
        registerLoginByExternalAuthProviderRoute(route)
        registerSendLoginConfirmationToPhoneRoute(route)
        registerLoginByTotpRoute(route)
        registerLoginByTotpRecoveryCodeRoute(route)
    }

    private fun registerLoginByEmailRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = OpenLoginRoutes.LOGIN_BY_EMAIL,
            builder = { loginByEmailDocs(allowedRoles, allowedAccountStatuses) },
            body = { loginByEmail() }
        )
    }

    private fun registerLoginByPhoneRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = OpenLoginRoutes.LOGIN_BY_PHONE,
            builder = { loginByPhoneDocs(allowedRoles, allowedAccountStatuses) },
            body = { loginByPhone() }
        )
    }

    private fun registerLoginByExternalAuthProviderRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = OpenLoginRoutes.LOGIN_BY_EXTERNAL_AUTH_PROVIDER,
            builder = { loginByExternalAuthProviderDocs(allowedRoles, allowedAccountStatuses) },
            body = { loginByExternalAuthProvider() }
        )
    }

    private fun registerSendLoginConfirmationToPhoneRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = OpenLoginRoutes.SEND_LOGIN_CONFIRMATION_TO_PHONE,
            builder = { sendLoginConfirmationToPhoneDocs(allowedRoles, allowedAccountStatuses) },
            body = { sendLoginConfirmationToPhone() }
        )
    }

    private fun registerLoginByTotpRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = OpenLoginRoutes.LOGIN_BY_TOTP,
            builder = { loginByTotpDocs(allowedRoles, allowedAccountStatuses) },
            body = { loginByTotp() }
        )
    }

    private fun registerLoginByTotpRecoveryCodeRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = OpenLoginRoutes.LOGIN_BY_TOTP_RECOVERY_CODE,
            builder = { loginByTotpRecoveryCodeDocs(allowedRoles, allowedAccountStatuses) },
            body = { loginByTotpRecoveryCode() }
        )
    }

    private fun RouteConfig.loginByEmailDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = LOGIN_BY_EMAIL_ROUTE_SUMMARY
        operationId = LOGIN_BY_EMAIL_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = LOGIN_BY_EMAIL_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )
        request { body<LoginByEmailRequest>() }
        response {
            code(HttpStatusCode.OK) { description = LOGIN_ROUTE_RESPONSE_OK_DESCRIPTION }
        }
    }

    private suspend fun RoutingContext.loginByEmail() {
        val request = call.validateRequest<LoginByEmailRequest>()

        val result = loginByEmailUseCase(
            email = request.email,
            password = request.password,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) { authData ->
            authData.toAuthDataPayload()
        }
    }

    private fun RouteConfig.loginByPhoneDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = LOGIN_BY_PHONE_ROUTE_SUMMARY
        operationId = LOGIN_BY_PHONE_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = LOGIN_BY_PHONE_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )
        request { body<LoginByPhoneRequest>() }
        response {
            code(HttpStatusCode.OK) { description = LOGIN_ROUTE_RESPONSE_OK_DESCRIPTION }
        }
    }

    private suspend fun RoutingContext.loginByPhone() {
        val request = call.validateRequest<LoginByPhoneRequest>()

        val result = loginByPhoneUseCase(
            phoneNumber = request.phoneNumber,
            confirmationCode = request.confirmationCode,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) { authData ->
            authData.toAuthDataPayload()
        }
    }

    private fun RouteConfig.loginByExternalAuthProviderDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = LOGIN_BY_EXTERNAL_AUTH_PROVIDER_ROUTE_SUMMARY
        operationId = LOGIN_BY_EXTERNAL_AUTH_PROVIDER_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = LOGIN_BY_EXTERNAL_AUTH_PROVIDER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )
        request { body<LoginByExternalAuthProviderRequest>() }
        response {
            code(HttpStatusCode.OK) { description = LOGIN_ROUTE_RESPONSE_OK_DESCRIPTION }
        }
    }

    private suspend fun RoutingContext.loginByExternalAuthProvider() {
        val request = call.validateRequest<LoginByExternalAuthProviderRequest>()

        val authProvider = UserAuthProvider.fromValueOrNull(request.authProvider)
            ?: throw RequestHandlingException(
                CommonError.InvalidFieldValue(
                    fieldName = UserApiFields.AUTH_PROVIDER
                )
            )

        val result = loginByExternalAuthProviderUseCase(
            authProvider = authProvider,
            token = request.token,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) { authData ->
            authData.toAuthDataPayload()
        }
    }

    private fun RouteConfig.sendLoginConfirmationToPhoneDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = SEND_LOGIN_CONFIRMATION_TO_PHONE_ROUTE_SUMMARY
        operationId = SEND_LOGIN_CONFIRMATION_TO_PHONE_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = SEND_LOGIN_CONFIRMATION_TO_PHONE_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )
        request { body<SendConfirmationToPhoneRequest>() }
        response {
            code(HttpStatusCode.OK) {
                description = SEND_LOGIN_CONFIRMATION_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.sendLoginConfirmationToPhone() {
        val request = call.validateRequest<SendConfirmationToPhoneRequest>()
        val result = sendLoginConfirmationToPhoneUseCase(
            phoneNumber = request.phoneNumber,
            requestContext = call.getRequestContext()
        )
        call.respondResult(result, appLogger, appErrorParser) { otpConfirmation ->
            otpConfirmation.toOtpConfirmationPayload()
        }
    }

    private fun RouteConfig.loginByTotpDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = LOGIN_BY_TOTP_ROUTE_SUMMARY
        operationId = LOGIN_BY_TOTP_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = LOGIN_BY_TOTP_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )
        request { body<VerifyTotpPayload>() }
        response {
            code(HttpStatusCode.OK) { description =
                LOGIN_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.loginByTotp() {
        val request = call.validateRequest<VerifyTotpPayload>()

        val result = loginByTotpUseCase(
            mfaToken = request.mfaToken,
            code = request.code,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) { authData ->
            authData.toAuthDataPayload()
        }
    }

    private fun RouteConfig.loginByTotpRecoveryCodeDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = LOGIN_BY_TOTP_RECOVERY_CODE_ROUTE_SUMMARY
        operationId = LOGIN_BY_TOTP_RECOVERY_CODE_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = LOGIN_BY_TOTP_RECOVERY_CODE_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )
        request { body<VerifyTotpPayload>() }
        response {
            code(HttpStatusCode.OK) { description =
                LOGIN_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.loginByTotpRecoveryCode() {
        val request = call.validateRequest<VerifyTotpPayload>()

        val result = loginByTotpRecoveryCodeUseCase(
            mfaToken = request.mfaToken,
            code = request.code,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) { authData ->
            authData.toAuthDataPayload()
        }
    }

    companion object {
        const val LOGIN_BY_EMAIL_ROUTE_SUMMARY = "Login by email"
        const val LOGIN_BY_EMAIL_ROUTE_DESCRIPTION = "Authenticates a user using email and password."
        const val LOGIN_BY_EMAIL_ROUTE_OPERATION_ID = "loginByEmail"

        const val LOGIN_BY_PHONE_ROUTE_SUMMARY = "Login by phone"
        const val LOGIN_BY_PHONE_ROUTE_DESCRIPTION = "Authenticates a user using phone number and confirmation code."
        const val LOGIN_BY_PHONE_ROUTE_OPERATION_ID = "loginByPhone"

        const val LOGIN_BY_EXTERNAL_AUTH_PROVIDER_ROUTE_SUMMARY = "Login via external auth provider"
        const val LOGIN_BY_EXTERNAL_AUTH_PROVIDER_ROUTE_DESCRIPTION = "Authenticates a user using an external " +
                "authentication provider token."
        const val LOGIN_BY_EXTERNAL_AUTH_PROVIDER_ROUTE_OPERATION_ID = "loginByExternalAuthProvider"

        const val LOGIN_BY_TOTP_ROUTE_SUMMARY = "Login by TOTP"
        const val LOGIN_BY_TOTP_ROUTE_DESCRIPTION = "Finalizes the login process by providing a TOTP code."
        const val LOGIN_BY_TOTP_ROUTE_OPERATION_ID = "loginByTotp"

        const val LOGIN_BY_TOTP_RECOVERY_CODE_ROUTE_SUMMARY = "Login by totp recovery code"
        const val LOGIN_BY_TOTP_RECOVERY_CODE_ROUTE_DESCRIPTION = "Finalizes the login process by providing a recovery code."
        const val LOGIN_BY_TOTP_RECOVERY_CODE_ROUTE_OPERATION_ID = "loginByTotpRecoveryCode"

        const val LOGIN_ROUTE_RESPONSE_OK_DESCRIPTION = "Success. User authenticated."

        const val SEND_LOGIN_CONFIRMATION_TO_PHONE_ROUTE_SUMMARY = "Send login confirmation code to phone"
        const val SEND_LOGIN_CONFIRMATION_TO_PHONE_ROUTE_DESCRIPTION = "Sends a verification code to the phone number for login/registration purposes."
        const val SEND_LOGIN_CONFIRMATION_TO_PHONE_ROUTE_OPERATION_ID = "sendLoginConfirmationToPhone"
        const val SEND_LOGIN_CONFIRMATION_ROUTE_RESPONSE_OK_DESCRIPTION = "Success. Verification code sent."
    }
}