package io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.login.OpenLoginRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.refreshtoken.OpenRefreshTokenRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.registration.OpenRegistrationRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.resetpassword.OpenResetPasswordRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.settings.OpenAuthSettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.unlock.OpenUnlockRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Root public authentication router for the user feature, aggregating all non-authenticated flows.
 *
 * This router orchestrates the registration of:
 * 1. Session token renewal ([OpenRefreshTokenRouter]).
 * 2. Multichannel login and MFA verification ([OpenLoginRouter]).
 * 3. User registration and confirmation flows ([OpenRegistrationRouter]).
 * 4. Password recovery and reset mechanisms ([OpenResetPasswordRouter]).
 * 5. Publicly accessible authentication settings and metadata ([OpenAuthSettingsRouter]).
 * 6. Self-service account unlock flows ([OpenUnlockRouter]).
 */
@Singleton
class OpenAuthRouter @Inject constructor(
    private val openRefreshTokenRouter: OpenRefreshTokenRouter,
    private val openLoginRouter: OpenLoginRouter,
    private val openRegistrationRouter: OpenRegistrationRouter,
    private val openResetPasswordRouter: OpenResetPasswordRouter,
    private val openAuthSettingsRouter: OpenAuthSettingsRouter,
    private val openUnlockRouter: OpenUnlockRouter
) : BaseRouter {
    override fun register(route: Route) {
        openRefreshTokenRouter.register(route)
        openLoginRouter.register(route)
        openRegistrationRouter.register(route)
        openResetPasswordRouter.register(route)
        openAuthSettingsRouter.register(route)
        openUnlockRouter.register(route)
    }
}