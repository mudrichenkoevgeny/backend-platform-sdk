package io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByEmailUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByTotpRecoveryCodeUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByTotpUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.verifytotp.VerifyTotpPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.data.toAuthDataPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.login.LoginByEmailRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.management.auth.login.SelfManagementLoginRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management HTTP routes for authentication, including multifactor verification flows.
 *
 * Registered routes:
 * 1. [SelfManagementLoginRoutes.LOGIN_BY_EMAIL] — handles primary authentication via [LoginByEmailUseCase].
 * 2. [SelfManagementLoginRoutes.LOGIN_BY_TOTP] — handles secondary verification via [LoginByTotpUseCase].
 * 3. [SelfManagementLoginRoutes.LOGIN_BY_TOTP_RECOVERY_CODE] — handles account recovery via [LoginByTotpRecoveryCodeUseCase].
 */
@Singleton
class SelfManagementLoginRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val loginByEmailUseCase: LoginByEmailUseCase,
    private val loginByTotpUseCase: LoginByTotpUseCase,
    private val loginByTotpRecoveryCodeUseCase: LoginByTotpRecoveryCodeUseCase
) : BaseRouter {

    override fun register(route: Route) {
        registerLoginByEmailRoute(route)
        registerLoginByTotpRoute(route)
        registerLoginByTotpRecoveryCodeRoute(route)
    }

    private fun registerLoginByEmailRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = SelfManagementLoginRoutes.LOGIN_BY_EMAIL,
            builder = { loginByEmailDocs(allowedRoles, allowedAccountStatuses) },
            body = { loginByEmail() }
        )
    }

    private fun registerLoginByTotpRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = SelfManagementLoginRoutes.LOGIN_BY_TOTP,
            builder = { loginByTotpDocs(allowedRoles, allowedAccountStatuses) },
            body = { loginByTotp() }
        )
    }

    private fun registerLoginByTotpRecoveryCodeRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.post(
            path = SelfManagementLoginRoutes.LOGIN_BY_TOTP_RECOVERY_CODE,
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
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.AUTH)
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
            password = request.password,requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) { authData ->
            authData.toAuthDataPayload()
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
            code(HttpStatusCode.OK) { description = LOGIN_ROUTE_RESPONSE_OK_DESCRIPTION }
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
            code(HttpStatusCode.OK) { description = LOGIN_ROUTE_RESPONSE_OK_DESCRIPTION }
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
        const val LOGIN_BY_EMAIL_ROUTE_SUMMARY = "Management login by email"
        const val LOGIN_BY_EMAIL_ROUTE_DESCRIPTION = "Authenticates a management account using email and password."
        const val LOGIN_BY_EMAIL_ROUTE_OPERATION_ID = "managementLoginByEmail"

        const val LOGIN_BY_TOTP_ROUTE_SUMMARY = "Management login by TOTP"
        const val LOGIN_BY_TOTP_ROUTE_DESCRIPTION = "Finalizes the management login process by providing a TOTP code."
        const val LOGIN_BY_TOTP_ROUTE_OPERATION_ID = "managementLoginByTotp"

        const val LOGIN_BY_TOTP_RECOVERY_CODE_ROUTE_SUMMARY = "Management login by recovery code"
        const val LOGIN_BY_TOTP_RECOVERY_CODE_ROUTE_DESCRIPTION = "Finalizes the management login process by providing a recovery code."
        const val LOGIN_BY_TOTP_RECOVERY_CODE_ROUTE_OPERATION_ID = "managementLoginByTotpRecoveryCode"

        const val LOGIN_ROUTE_RESPONSE_OK_DESCRIPTION = "Success. User authenticated."
    }
}