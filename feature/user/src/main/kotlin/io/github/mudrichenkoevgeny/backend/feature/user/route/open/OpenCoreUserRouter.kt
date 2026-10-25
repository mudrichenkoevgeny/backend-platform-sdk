package io.github.mudrichenkoevgeny.backend.feature.user.route.open

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.OpenAuthRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.configuration.OpenUserConfigurationRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.identifier.OpenIdentifierRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.security.OpenSecuritySettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.session.OpenSessionRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.user.OpenUserRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.user.security.OpenUserSecurityRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.settings.OpenGlobalSettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.storage.OpenStorageRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.storage.OpenStorageSettingsRouter
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
 * 1. [OpenStorageRouter] — Public storage file operations and presigned upload URLs.
 * 2. [OpenAuthRouter] — Unauthenticated entry points (Login, Register, Refresh, Reset).
 * 3. [OpenUserConfigurationRouter] — System-wide user feature metadata and settings.
 * 4. [OpenUserRouter] — Personal profile management and account lifecycle.
 * 5. [OpenUserSecurityRouter] — MFA (TOTP) setup and recovery code management.
 * 6. [OpenIdentifierRouter] — Management of linked identities (Email, Phone, OAuth).
 * 7. [OpenSessionRouter] — Active session introspection and termination.
 * 8. [OpenSecuritySettingsRouter] — Public security and password policy settings.
 * 9. [OpenGlobalSettingsRouter] — Public global configuration and deployment options.
 * 10. [OpenStorageSettingsRouter] — Public storage upload limits and configuration.
 */
@Singleton
class OpenCoreUserRouter @Inject constructor(
    private val openStorageRouter: OpenStorageRouter,
    private val openAuthRouter: OpenAuthRouter,
    private val openUserConfigurationRouter: OpenUserConfigurationRouter,
    private val openUserRouter: OpenUserRouter,
    private val openUserSecurityRouter: OpenUserSecurityRouter,
    private val openIdentifierRouter: OpenIdentifierRouter,
    private val openSessionRouter: OpenSessionRouter,
    private val openSecuritySettingsRouter: OpenSecuritySettingsRouter,
    private val openGlobalSettingsRouter: OpenGlobalSettingsRouter,
    private val openStorageSettingsRouter: OpenStorageSettingsRouter
) : BaseRouter {
    override fun register(route: Route) {
        openStorageRouter.register(route)
        openAuthRouter.register(route)
        openUserConfigurationRouter.register(route)
        openUserRouter.register(route)
        openUserSecurityRouter.register(route)
        openIdentifierRouter.register(route)
        openSessionRouter.register(route)
        openSecuritySettingsRouter.register(route)
        openGlobalSettingsRouter.register(route)
        openStorageSettingsRouter.register(route)
    }
}