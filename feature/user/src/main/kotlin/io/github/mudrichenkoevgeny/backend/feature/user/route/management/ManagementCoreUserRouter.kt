package io.github.mudrichenkoevgeny.backend.feature.user.route.management

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.audit.ManagementAuditRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.ManagementAuthRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.settings.ManagementAuthSettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.configuration.ManagementUserConfigurationRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.identifier.ManagementIdentifierRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.identifier.SelfManagementIdentifierRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.security.ManagementSecuritySettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.session.ManagementSessionRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.session.SelfManagementSessionRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.settings.ManagementGlobalSettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.storage.ManagementStorageRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.storage.ManagementStorageSettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.storage.SelfManagementStorageRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.user.ManagementUserRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.user.SelfManagementUserRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.user.security.ManagementUserSecurityRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.user.security.SelfManagementUserSecurityRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.settings.OpenGlobalSettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.security.OpenSecuritySettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.settings.OpenAuthSettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.configuration.OpenUserConfigurationRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.storage.OpenStorageSettingsRouter
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Root management router for the user feature, aggregating all administrative and self-service sub-routers,
 * as well as open configuration routers required by unauthenticated clients on the management connector.
 *
 * This router orchestrates the registration of:
 * 1. Authentication management ([ManagementAuthRouter]).
 * 2. Management auth settings ([ManagementAuthSettingsRouter]).
 * 3. Administrative and self-service identifier management ([ManagementIdentifierRouter], [SelfManagementIdentifierRouter]).
 * 4. Administrative and self-service session control ([ManagementSessionRouter], [SelfManagementSessionRouter]).
 * 5. Administrative and self-service user profile operations ([ManagementUserRouter], [SelfManagementUserRouter]).
 * 6. Administrative and self-service security/MFA settings ([ManagementUserSecurityRouter], [SelfManagementUserSecurityRouter]).
 * 7. Global user feature configurations ([ManagementUserConfigurationRouter]).
 * 8. System & policy settings ([ManagementGlobalSettingsRouter], [ManagementSecuritySettingsRouter], [ManagementStorageSettingsRouter]).
 * 9. Audit trail management ([ManagementAuditRouter]).
 * 10. Self-service and administrative storage file management ([SelfManagementStorageRouter], [ManagementStorageRouter]).
 * 11. Unauthenticated open settings ([OpenGlobalSettingsRouter], [OpenSecuritySettingsRouter], [OpenStorageSettingsRouter], [OpenAuthSettingsRouter], [OpenUserConfigurationRouter]).
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
    private val managementUserConfigurationRouter: ManagementUserConfigurationRouter,
    private val managementGlobalSettingsRouter: ManagementGlobalSettingsRouter,
    private val managementSecuritySettingsRouter: ManagementSecuritySettingsRouter,
    private val managementStorageSettingsRouter: ManagementStorageSettingsRouter,
    private val managementAuditRouter: ManagementAuditRouter,
    private val selfManagementStorageRouter: SelfManagementStorageRouter,
    private val managementStorageRouter: ManagementStorageRouter,
    private val openGlobalSettingsRouter: OpenGlobalSettingsRouter,
    private val openSecuritySettingsRouter: OpenSecuritySettingsRouter,
    private val openStorageSettingsRouter: OpenStorageSettingsRouter,
    private val openAuthSettingsRouter: OpenAuthSettingsRouter,
    private val openUserConfigurationRouter: OpenUserConfigurationRouter
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
        managementGlobalSettingsRouter.register(route)
        managementSecuritySettingsRouter.register(route)
        managementStorageSettingsRouter.register(route)
        managementAuditRouter.register(route)
        selfManagementStorageRouter.register(route)
        managementStorageRouter.register(route)
        openGlobalSettingsRouter.register(route)
        openSecuritySettingsRouter.register(route)
        openStorageSettingsRouter.register(route)
        openAuthSettingsRouter.register(route)
        openUserConfigurationRouter.register(route)
    }
}
