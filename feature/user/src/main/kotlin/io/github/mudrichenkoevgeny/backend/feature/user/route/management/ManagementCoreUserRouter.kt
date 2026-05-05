package io.github.mudrichenkoevgeny.backend.feature.user.route.management

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.ManagementAuthRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.configuration.ManagementUserConfigurationRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.identifier.ManagementIdentifierRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.identifier.SelfManagementIdentifierRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.session.ManagementSessionRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.session.SelfManagementSessionRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.user.ManagementUserRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.user.SelfManagementUserRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.user.security.ManagementUserSecurityRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.user.security.SelfManagementUserSecurityRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Root management router for the user feature, aggregating all administrative and self-service sub-routers.
 *
 * This router orchestrates the registration of:
 * 1. Authentication management ([ManagementAuthRouter]).
 * 2. Administrative and self-service identifier management ([ManagementIdentifierRouter], [SelfManagementIdentifierRouter]).
 * 3. Administrative and self-service session control ([ManagementSessionRouter], [SelfManagementSessionRouter]).
 * 4. Administrative and self-service user profile operations ([ManagementUserRouter], [SelfManagementUserRouter]).
 * 5. Administrative and self-service security/MFA settings ([ManagementUserSecurityRouter], [SelfManagementUserSecurityRouter]).
 * 6. Global user feature configurations ([ManagementUserConfigurationRouter]).
 */
@Singleton
class ManagementCoreUserRouter @Inject constructor(
    private val managementAuthRouter: ManagementAuthRouter,
    private val managementIdentifierRouter: ManagementIdentifierRouter,
    private val selfManagementIdentifierRouter: SelfManagementIdentifierRouter,
    private val managementSessionRouter: ManagementSessionRouter,
    private val selfManagementSessionRouter: SelfManagementSessionRouter,
    private val managementUserRouter: ManagementUserRouter,
    private val selfManagementUserRouter: SelfManagementUserRouter,
    private val managementUserSecurityRouter: ManagementUserSecurityRouter,
    private val selfManagementUserSecurityRouter: SelfManagementUserSecurityRouter,
    private val managementUserConfigurationRouter: ManagementUserConfigurationRouter
) : BaseRouter {
    override fun register(route: Route) {
        managementAuthRouter.register(route)
        managementIdentifierRouter.register(route)
        selfManagementIdentifierRouter.register(route)
        managementSessionRouter.register(route)
        selfManagementSessionRouter.register(route)
        managementUserRouter.register(route)
        selfManagementUserRouter.register(route)
        managementUserSecurityRouter.register(route)
        selfManagementUserSecurityRouter.register(route)
        managementUserConfigurationRouter.register(route)
    }
}