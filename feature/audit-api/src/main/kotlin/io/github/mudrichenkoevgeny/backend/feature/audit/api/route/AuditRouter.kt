package io.github.mudrichenkoevgeny.backend.feature.audit.api.route

import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.audit.api.route.management.ManagementAuditRouter
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates and registers all HTTP routes provided by the audit API module.
 */
@Singleton
class AuditRouter @Inject constructor(
    private val managementAuditRouter: ManagementAuditRouter
) : BaseRouter {
    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            managementAuditRouter.register(this)
        }
    }
}
