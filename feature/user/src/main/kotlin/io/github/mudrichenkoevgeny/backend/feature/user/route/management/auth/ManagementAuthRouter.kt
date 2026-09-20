package io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.login.SelfManagementLoginRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.refreshtoken.SelfManagementRefreshTokenRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.resetpassword.SelfManagementResetPasswordRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.settings.ManagementAuthSettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.unlock.SelfManagementUnlockRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Root management HTTP router for authentication and security configuration.
 *
 * Orchestrates the registration of specialized sub-routers:
 * 1. [ManagementAuthSettingsRouter] — handles authentication policy management.
 * 2. [SelfManagementLoginRouter] — handles multifactor authentication flows.
 * 3. [SelfManagementRefreshTokenRouter] — handles session token renewal.
 * 4. [SelfManagementResetPasswordRouter] — handles account recovery and password resets.
 * 5. [SelfManagementUnlockRouter] — handles account unlock flows.
 */
@Singleton
class ManagementAuthRouter @Inject constructor(
    private val managementAuthSettingsRouter: ManagementAuthSettingsRouter,
    private val selfManagementLoginRouter: SelfManagementLoginRouter,
    private val selfManagementRefreshTokenRouter: SelfManagementRefreshTokenRouter,
    private val selfManagementResetPasswordRouter: SelfManagementResetPasswordRouter,
    private val selfManagementUnlockRouter: SelfManagementUnlockRouter
) : BaseRouter {
    override fun register(route: Route) {
        managementAuthSettingsRouter.register(route)
        selfManagementLoginRouter.register(route)
        selfManagementRefreshTokenRouter.register(route)
        selfManagementResetPasswordRouter.register(route)
        selfManagementUnlockRouter.register(route)
    }
}
