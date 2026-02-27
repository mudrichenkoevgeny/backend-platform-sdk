package io.github.mudrichenkoevgeny.backend.sample

import io.github.mudrichenkoevgeny.backend.core.common.application.httpconfiguration.configureHTTP
import io.github.mudrichenkoevgeny.backend.core.common.application.ratelimit.configureGlobalRateLimit
import io.github.mudrichenkoevgeny.backend.core.common.application.serialization.configureSerialization
import io.github.mudrichenkoevgeny.backend.core.common.application.statuspages.configureStatusPages
import io.github.mudrichenkoevgeny.backend.core.common.application.websockets.configureWebSockets
import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppEnvironment
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.route.setupSwaggerEndpoints
import io.github.mudrichenkoevgeny.backend.core.common.routing.onPort
import io.github.mudrichenkoevgeny.backend.core.observability.application.configureObservability
import io.github.mudrichenkoevgeny.backend.core.observability.metrics.route.installMetricsEndpoint
import io.github.mudrichenkoevgeny.backend.sample.di.AppComponent
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.routing.routing
import kotlinx.coroutines.launch

fun Application.module(
    appComponent: AppComponent
) {
    val commonConfig = appComponent.commonConfig()
    val telemetryProvider = appComponent.telemetryProvider()
    val appLogger = appComponent.appLogger()

    this.monitor.subscribe(ApplicationStarted) {
        launch {
            appComponent.healthCheckerManager().checkNonCriticalHealth()
        }

        launch {
            appComponent.seedAdminAccountsUseCase().execute()
            appComponent.seedGlobalSettingsUseCase().execute()
            appComponent.seedSecuritySettingsUseCase().execute()
            appComponent.seedAuthSettingsUseCase().execute()
        }
    }

    appComponent.authenticationProvider().configureAuthentication(this)

    configureSerialization()
    configureObservability(telemetryProvider, appLogger)
    configureStatusPages(
        appErrorParser = appComponent.appErrorParser(),
        appLogger = appLogger
    )
    configureHTTP(
        environment = commonConfig.environment,
        allowedOrigins = commonConfig.allowedOrigins
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

        appComponent.settingsFeatureRouter().register(this)
        appComponent.securityFeatureRouter().register(this)
        appComponent.userFeatureRouter().register(this)
        appComponent.authenticatedWebSocketRouter().register(this)
    }
}