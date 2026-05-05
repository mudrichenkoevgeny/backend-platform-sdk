package io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.login.SelfManagementLoginRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.refreshtoken.SelfManagementRefreshTokenRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.resetpassword.SelfManagementResetPasswordRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.settings.ManagementAuthSettingsRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Root management HTTP router for authentication and security configuration.
 *
 * Orchestrates the registration of specialized sub-routers:
 * 1. [SelfManagementLoginRouter] — handles multifactor authentication flows.
 * 2. [SelfManagementRefreshTokenRouter] — handles session token renewal.
 * 3. [SelfManagementResetPasswordRouter] — handles account recovery and password resets.
 * 4. [ManagementAuthSettingsRouter] — handles authentication policy management.
 */
@Singleton
class ManagementAuthRouter @Inject constructor(
    private val selfManagementLoginRouter: SelfManagementLoginRouter,
    private val selfManagementRefreshTokenRouter: SelfManagementRefreshTokenRouter,
    private val selfManagementResetPasswordRouter: SelfManagementResetPasswordRouter,
    private val managementAuthSettingsRouter: ManagementAuthSettingsRouter
) : BaseRouter {
    override fun register(route: Route) {
        selfManagementLoginRouter.register(route)
        selfManagementRefreshTokenRouter.register(route)
        selfManagementResetPasswordRouter.register(route)
        managementAuthSettingsRouter.register(route)
    }
}