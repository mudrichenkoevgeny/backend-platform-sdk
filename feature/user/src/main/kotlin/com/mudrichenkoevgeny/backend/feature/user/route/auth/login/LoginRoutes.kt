package com.mudrichenkoevgeny.backend.feature.user.route.auth.login

import com.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import com.mudrichenkoevgeny.backend.core.common.routing.respondResult
import com.mudrichenkoevgeny.backend.core.common.validation.validateRequest
import com.mudrichenkoevgeny.backend.feature.user.mapper.toResponse
import com.mudrichenkoevgeny.backend.feature.user.network.request.auth.login.LoginByEmailRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.auth.login.LoginByExternalAuthProviderRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.auth.login.LoginByPhoneRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.confirmation.SendConfirmationToPhoneRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.context.getRequestContext
import com.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import com.mudrichenkoevgeny.backend.feature.user.route.auth.AuthRoutes
import com.mudrichenkoevgeny.backend.feature.user.usecase.auth.login.LoginByEmailUseCase
import com.mudrichenkoevgeny.backend.feature.user.usecase.auth.login.LoginByExternalAuthProviderUseCase
import com.mudrichenkoevgeny.backend.feature.user.usecase.auth.login.LoginByPhoneUseCase
import com.mudrichenkoevgeny.backend.feature.user.usecase.auth.login.SendLoginConfirmationToPhoneUseCase
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

object LoginRoutes {
    const val BASE_LOGIN_ROUTE = "${AuthRoutes.BASE_AUTH_ROUTE}/login"
    const val LOGIN_BY_EMAIL = "$BASE_LOGIN_ROUTE/email"
    const val LOGIN_BY_PHONE = "$BASE_LOGIN_ROUTE/phone"
    const val LOGIN_BY_EXTERNAL_AUTH_PROVIDER = "$BASE_LOGIN_ROUTE/external-auth-provider"
    const val BASE_LOGIN_CONFIRMATION_ROUTE = "${LOGIN_BY_PHONE}/confirmation"
    const val SEND_LOGIN_CONFIRMATION_TO_PHONE = "$BASE_LOGIN_CONFIRMATION_ROUTE/send-to-phone"
}

@Singleton
class LoginRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val loginByEmailUseCase: LoginByEmailUseCase,
    private val loginByPhoneUseCase: LoginByPhoneUseCase,
    private val loginByExternalAuthProviderUseCase: LoginByExternalAuthProviderUseCase,
    private val sendLoginConfirmationToPhoneUseCase: SendLoginConfirmationToPhoneUseCase
) : BaseRouter {
    override fun register(route: Route) {
        route.post(
            path = LoginRoutes.LOGIN_BY_EMAIL,
            builder = { loginByEmailDocs() },
            body = { loginByEmail() }
        )

        route.post(
            path = LoginRoutes.LOGIN_BY_PHONE,
            builder = { loginByPhoneDocs() },
            body = { loginByPhone() }
        )

        route.post(
            path = LoginRoutes.LOGIN_BY_EXTERNAL_AUTH_PROVIDER,
            builder = { loginByExternalAuthProviderDocs() },
            body = { loginByExternalAuthProvider() }
        )

        route.post(
            path = LoginRoutes.SEND_LOGIN_CONFIRMATION_TO_PHONE,
            builder = { sendLoginConfirmationToPhoneDocs() },
            body = { sendLoginConfirmationToPhone() }
        )
    }

    private fun RouteConfig.loginByEmailDocs() {
        summary = LOGIN_BY_EMAIL_ROUTE_SUMMARY
        description = LOGIN_BY_EMAIL_ROUTE_DESCRIPTION
        operationId = LOGIN_BY_EMAIL_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        request {
            body<LoginByEmailRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = LOGIN_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.loginByEmail() {
        val request = call.validateRequest<LoginByEmailRequest>()

        val result = loginByEmailUseCase.execute(
            email = request.email,
            password = request.password,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) {
            authData -> authData.toResponse()
        }
    }

    private fun RouteConfig.loginByPhoneDocs() {
        summary = LOGIN_BY_PHONE_ROUTE_SUMMARY
        description = LOGIN_BY_PHONE_ROUTE_DESCRIPTION
        operationId = LOGIN_BY_PHONE_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        request {
            body<LoginByPhoneRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = LOGIN_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.loginByPhone() {
        val request = call.validateRequest<LoginByPhoneRequest>()

        val result = loginByPhoneUseCase.execute(
            phoneNumber = request.phoneNumber,
            confirmationCode = request.confirmationCode,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) {
            authData -> authData.toResponse()
        }
    }

    private fun RouteConfig.loginByExternalAuthProviderDocs() {
        summary = LOGIN_BY_EXTERNAL_AUTH_PROVIDER_ROUTE_SUMMARY
        description = LOGIN_BY_EXTERNAL_AUTH_PROVIDER_ROUTE_DESCRIPTION
        operationId = LOGIN_BY_EXTERNAL_AUTH_PROVIDER_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        request {
            body<LoginByExternalAuthProviderRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = LOGIN_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.loginByExternalAuthProvider() {
        val request = call.validateRequest<LoginByExternalAuthProviderRequest>()

        val result = loginByExternalAuthProviderUseCase.execute(
            authProviderKey = request.authProvider,
            token = request.token,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) {
            authData -> authData.toResponse()
        }
    }

    private fun RouteConfig.sendLoginConfirmationToPhoneDocs() {
        summary = SEND_LOGIN_CONFIRMATION_TO_PHONE_ROUTE_SUMMARY
        description = SEND_LOGIN_CONFIRMATION_TO_PHONE_ROUTE_DESCRIPTION
        operationId = SEND_LOGIN_CONFIRMATION_TO_PHONE_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.AUTH)
        request {
            body<SendConfirmationToPhoneRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = SEND_LOGIN_CONFIRMATION_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.sendLoginConfirmationToPhone() {
        val request = call.validateRequest<SendConfirmationToPhoneRequest>()

        val result = sendLoginConfirmationToPhoneUseCase.execute(
            phoneNumber = request.phoneNumber,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) {
            sendConfirmation -> sendConfirmation.toResponse()
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

        const val LOGIN_ROUTE_RESPONSE_OK_DESCRIPTION = "Success. User authenticated."

        const val SEND_LOGIN_CONFIRMATION_TO_PHONE_ROUTE_SUMMARY = "Send login confirmation code to phone"
        const val SEND_LOGIN_CONFIRMATION_TO_PHONE_ROUTE_DESCRIPTION = "Sends a verification code to the phone number for login/registration purposes."
        const val SEND_LOGIN_CONFIRMATION_TO_PHONE_ROUTE_OPERATION_ID = "sendLoginConfirmationToPhone"
        const val SEND_LOGIN_CONFIRMATION_ROUTE_RESPONSE_OK_DESCRIPTION = "Success. Verification code sent."
    }
}