package io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.login.OpenLoginRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.refreshtoken.OpenRefreshTokenRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.register.OpenRegisterRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.resetpassword.OpenResetPasswordRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.settings.OpenAuthSettingsRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Root public authentication router for the user feature, aggregating all non-authenticated flows.
 *
 * This router orchestrates the registration of:
 * 1. Session token renewal ([OpenRefreshTokenRouter]).
 * 2. Multichannel login and MFA verification ([OpenLoginRouter]).
 * 3. User registration and confirmation flows ([OpenRegisterRouter]).
 * 4. Password recovery and reset mechanisms ([OpenResetPasswordRouter]).
 * 5. Publicly accessible authentication settings and metadata ([OpenAuthSettingsRouter]).
 */
@Singleton
class OpenAuthRouter @Inject constructor(
    private val openRefreshTokenRouter: OpenRefreshTokenRouter,
    private val openLoginRouter: OpenLoginRouter,
    private val openRegisterRouter: OpenRegisterRouter,
    private val openResetPasswordRouter: OpenResetPasswordRouter,
    private val openAuthSettingsRouter: OpenAuthSettingsRouter
) : BaseRouter {
    override fun register(route: Route) {
        openRefreshTokenRouter.register(route)
        openLoginRouter.register(route)
        openRegisterRouter.register(route)
        openResetPasswordRouter.register(route)
        openAuthSettingsRouter.register(route)
    }
}