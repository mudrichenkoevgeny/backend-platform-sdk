package io.github.mudrichenkoevgeny.backend.feature.user.route.open

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.OpenAuthRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.configuration.OpenUserConfigurationRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.identifier.OpenIdentifierRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.session.OpenSessionRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.user.OpenUserRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.user.security.OpenUserSecurityRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Root aggregator for all public and user-facing authentication and profile routes.
 *
 * This router serves as the central entry point for the user feature's web API,
 * orchestrating the registration of specialized sub-routers. It covers the entire
 * user lifecycle from unauthenticated flows (login, registration) to authenticated
 * profile management, security configurations, and session control.
 *
 * Registered sub-routers:
 * 1. [OpenAuthRouter] — Unauthenticated entry points (Login, Register, Refresh, Reset).
 * 2. [OpenUserConfigurationRouter] — System-wide user feature metadata and settings.
 * 3. [OpenUserRouter] — Personal profile management and account lifecycle.
 * 4. [OpenUserSecurityRouter] — MFA (TOTP) setup and recovery code management.
 * 5. [OpenIdentifierRouter] — Management of linked identities (Email, Phone, OAuth).
 * 6. [OpenSessionRouter] — Active session introspection and termination.
 */
@Singleton
class OpenCoreUserRouter @Inject constructor(
    private val openAuthRouter: OpenAuthRouter,
    private val openUserConfigurationRouter: OpenUserConfigurationRouter,
    private val openUserRouter: OpenUserRouter,
    private val openUserSecurityRouter: OpenUserSecurityRouter,
    private val openIdentifierRouter: OpenIdentifierRouter,
    private val openSessionRouter: OpenSessionRouter
) : BaseRouter {
    override fun register(route: Route) {
        openAuthRouter.register(route)
        openUserConfigurationRouter.register(route)
        openUserRouter.register(route)
        openUserSecurityRouter.register(route)
        openIdentifierRouter.register(route)
        openSessionRouter.register(route)
    }
}