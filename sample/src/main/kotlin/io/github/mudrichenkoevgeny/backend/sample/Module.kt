package io.github.mudrichenkoevgeny.backend.sample

import io.github.mudrichenkoevgeny.backend.core.common.application.httpconfiguration.configureHTTP
import io.github.mudrichenkoevgeny.backend.core.common.application.ratelimit.configureGlobalRateLimit
import io.github.mudrichenkoevgeny.backend.core.common.application.serialization.configureSerialization
import io.github.mudrichenkoevgeny.backend.core.common.application.statuspages.configureStatusPages
import io.github.mudrichenkoevgeny.backend.core.common.application.websockets.configureWebSockets
import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppEnvironment
import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppInstanceMode
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.route.setupSwaggerEndpoints
import io.github.mudrichenkoevgeny.backend.core.common.routing.onPort
import io.github.mudrichenkoevgeny.backend.core.observability.application.configureObservability
import io.github.mudrichenkoevgeny.backend.core.observability.metrics.route.installMetricsEndpoint
import io.github.mudrichenkoevgeny.backend.sample.di.AppComponent
import io.github.mudrichenkoevgeny.shared.foundation.feature.auditapi.domain.permissions.AuditPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.securityapi.domain.permission.SecurityPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.settingsapi.domain.permission.SettingsPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.AuthSettingsPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.IdentifierPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.SessionPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.routing.routing
import kotlinx.coroutines.launch

/**
 * Ktor application module for the sample application.
 *
 * Wires SDK capabilities into a single Ktor [Application]:
 * - HTTP defaults (CORS, headers, etc.) via [configureHTTP]
 * - JSON serialization via [configureSerialization]
 * - observability (OpenTelemetry + metrics) via [configureObservability] and [installMetricsEndpoint]
 * - status pages (error mapping) via [configureStatusPages]
 * - global rate limiting via [configureGlobalRateLimit]
 * - WebSockets via [configureWebSockets]
 * - Swagger setup via [setupSwaggerEndpoints] for non-PROD environments
 *
 * On [ApplicationStarted], runs health checks and seeds default data/settings via the corresponding
 * injected use cases.
 */
fun Application.module(
    appComponent: AppComponent
) {
    val commonConfig = appComponent.commonConfig()
    val telemetryProvider = appComponent.telemetryProvider()
    val appLogger = appComponent.appLogger()

    configureHTTP(
        environment = commonConfig.environment,
        allowedOrigins = commonConfig.allowedOrigins
    )

    this.monitor.subscribe(ApplicationStarted) {
        if (commonConfig.instanceMode != AppInstanceMode.PUBLIC) {
            appComponent.userScheduledJobs().start()

            launch {
                appComponent.healthCheckerManager().checkNonCriticalHealth()
            }

            launch {
                appComponent.seedAdminAccountsUseCase()(
                    permissionCodesForUserCreation = setOf(SettingsPermissionCode.GLOBAL_SETTINGS_UPDATE)
                            + setOf(SecurityPermissionCode.SECURITY_SETTINGS_UPDATE)
                            + setOf(AuthSettingsPermissionCode.AUTH_SETTINGS_UPDATE)
                            + AuditPermissionCode.ALL + UserPermissionCode.ALL + IdentifierPermissionCode.ALL +
                            SessionPermissionCode.ALL
                )
                appComponent.seedGlobalSettingsUseCase()()
                appComponent.seedSecuritySettingsUseCase()()
                appComponent.seedAuthSettingsUseCase()()
            }
        }
    }

    appComponent.authenticationProvider().configureAuthentication(this)

    configureSerialization()
    configureObservability(telemetryProvider, appLogger)
    configureStatusPages(
        appErrorParser = appComponent.appErrorParser(),
        appLogger = appLogger
    )
    configureGlobalRateLimit(
        rateLimit = commonConfig.rateLimit,
        rateLimitPeriodSeconds = commonConfig.rateLimitPeriodSeconds
    )
    configureWebSockets()

    appComponent.swaggerInitializer().initialize(this)

    routing {
        onPort(commonConfig.ktorManagementPort) {
            installMetricsEndpoint(telemetryProvider)
        }

        if (commonConfig.environment != AppEnvironment.PROD) {
            setupSwaggerEndpoints()
        }

        appComponent.authenticatedWebSocketRouter().register(this)

        when (commonConfig.instanceMode) {
            AppInstanceMode.PUBLIC -> {
                appComponent.openSecuritySettingsRouter().register(this)
                appComponent.openGlobalSettingsSettingsRouter().register(this)
                appComponent.openCoreUserRouter().register(this)
            }
            AppInstanceMode.MANAGEMENT -> {
                appComponent.managementAuditRouter().register(this)
                appComponent.managementSecuritySettingsRouter().register(this)
                appComponent.managementGlobalSettingsRouter().register(this)
                appComponent.managementCoreUserRouter().register(this)
            }
            AppInstanceMode.FULL -> {
                appComponent.managementAuditRouter().register(this)
                appComponent.openSecuritySettingsRouter().register(this)
                appComponent.managementSecuritySettingsRouter().register(this)
                appComponent.openGlobalSettingsSettingsRouter().register(this)
                appComponent.managementGlobalSettingsRouter().register(this)
                appComponent.openCoreUserRouter().register(this)
                appComponent.managementCoreUserRouter().register(this)
            }
        }
    }
}