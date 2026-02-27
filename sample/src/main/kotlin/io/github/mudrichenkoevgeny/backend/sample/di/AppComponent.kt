package io.github.mudrichenkoevgeny.backend.sample.di

import dagger.Component
import io.github.mudrichenkoevgeny.backend.core.audit.di.AuditModules
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.CommonConfig
import io.github.mudrichenkoevgeny.backend.core.common.di.CommonModules
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.initializer.SwaggerInitializer
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheckerManager
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.crosscutting.di.CrosscuttingModules
import io.github.mudrichenkoevgeny.backend.core.database.di.DatabaseModules
import io.github.mudrichenkoevgeny.backend.core.events.di.EventsModules
import io.github.mudrichenkoevgeny.backend.core.observability.di.ObservabilityModules
import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider
import io.github.mudrichenkoevgeny.backend.core.security.di.SecurityModules
import io.github.mudrichenkoevgeny.backend.core.security.route.SecurityFeatureRouter
import io.github.mudrichenkoevgeny.backend.core.security.settings.usecase.SeedSecuritySettingsUseCase
import io.github.mudrichenkoevgeny.backend.core.settings.di.SettingsModules
import io.github.mudrichenkoevgeny.backend.core.settings.global.usecase.SeedGlobalSettingsUseCase
import io.github.mudrichenkoevgeny.backend.core.settings.route.SettingsFeatureRouter
import io.github.mudrichenkoevgeny.backend.core.storage.di.StorageModules
import io.github.mudrichenkoevgeny.backend.feature.user.di.UserModules
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.router.AuthenticatedWebSocketRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserFeatureRouter
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.settings.SeedAuthSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.system.SeedAdminAccountsUseCase
import io.github.mudrichenkoevgeny.backend.sample.appbootstrap.AppBootstrap
import io.github.mudrichenkoevgeny.backend.sample.lifecycle.AppShutdownHook
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CommonModules::class,
        ObservabilityModules::class,
        DatabaseModules::class,
        SettingsModules::class,
        SecurityModules::class,
        AuditModules::class,
        EventsModules::class,
        StorageModules::class,
        CrosscuttingModules::class,
        UserModules::class,
        AppModule::class
    ]
)
interface AppComponent {
    // common
    fun commonConfig(): CommonConfig
    fun appLogger(): AppLogger
    fun appErrorParser(): AppErrorParser
    fun healthCheckerManager(): HealthCheckerManager

    // observability
    fun telemetryProvider(): TelemetryProvider
    fun swaggerInitializer(): SwaggerInitializer

    // settings
    fun seedGlobalSettingsUseCase(): SeedGlobalSettingsUseCase
    fun settingsFeatureRouter(): SettingsFeatureRouter

    // security
    fun seedSecuritySettingsUseCase(): SeedSecuritySettingsUseCase
    fun securityFeatureRouter(): SecurityFeatureRouter

    // user
    fun authenticationProvider(): AuthenticationProvider
    fun seedAdminAccountsUseCase(): SeedAdminAccountsUseCase
    fun seedAuthSettingsUseCase(): SeedAuthSettingsUseCase
    fun userFeatureRouter(): UserFeatureRouter
    fun authenticatedWebSocketRouter(): AuthenticatedWebSocketRouter

    // app
    fun appBootstrap(): AppBootstrap
    fun appShutdownHook(): AppShutdownHook
}