package io.github.mudrichenkoevgeny.backend.core.security.application.iprestriction

import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.CommonConfig
import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppInstanceMode
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.core.security.validator.iprestriction.IpRestrictionPolicyValidator
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.request.path

/**
 * Configures the IP restriction plugin on the Ktor [Application].
 *
 * Intercepts incoming requests and checks the client's IP address against the configured
 * [SecuritySettingsProvider] IP restriction policy corresponding to the request's contour
 * (determined by [AppInstanceMode], management port, or URL path).
 *
 * @param commonConfig Common runtime configuration providing instance mode and ports.
 * @param securitySettingsProvider Provider for open and management IP restriction policies.
 * @param validator Validator for evaluating IP addresses against restriction policies.
 */
fun Application.configureIpRestriction(
    commonConfig: CommonConfig,
    securitySettingsProvider: SecuritySettingsProvider,
    validator: IpRestrictionPolicyValidator
) {
    val ipRestrictionPlugin = createApplicationPlugin("IpRestrictionPlugin") {
        onCall { call ->
            val isManagementContour = when (commonConfig.instanceMode) {
                AppInstanceMode.PUBLIC -> false
                AppInstanceMode.MANAGEMENT -> true
                AppInstanceMode.FULL -> {
                    call.request.local.localPort == commonConfig.ktorManagementPort ||
                        call.request.path().startsWith("/management")
                }
            }

            val policy = if (isManagementContour) {
                securitySettingsProvider.getManagementIpRestrictionPolicy()
            } else {
                securitySettingsProvider.getOpenIpRestrictionPolicy()
            }

            val clientIp = call.request.local.remoteHost
            if (!validator.isIpAllowed(clientIp, policy)) {
                throw RequestHandlingException(SecurityError.IpNotAllowed())
            }
        }
    }

    install(ipRestrictionPlugin)
}
