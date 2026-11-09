package io.github.mudrichenkoevgeny.backend.sample.di

import dagger.BindsInstance
import dagger.Component
import io.github.mudrichenkoevgeny.backend.core.audit.di.AuditModules
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.CommonConfig
import io.github.mudrichenkoevgeny.backend.core.common.di.CommonModules
import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BackgroundScope
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.initializer.SwaggerInitializer
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheckerManager
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.route.ManagementHealthRouter
import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig
import io.github.mudrichenkoevgeny.backend.core.database.di.DatabaseModules
import io.github.mudrichenkoevgeny.backend.core.database.migrator.DatabaseMigrator
import io.github.mudrichenkoevgeny.backend.core.events.di.EventsModules
import io.github.mudrichenkoevgeny.backend.core.observability.di.ObservabilityModules
import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider
import io.github.mudrichenkoevgeny.backend.core.security.di.SecurityModules
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.core.security.usecase.system.settings.SeedSecuritySettingsUseCase
import io.github.mudrichenkoevgeny.backend.core.security.validator.iprestriction.IpRestrictionPolicyValidator
import io.github.mudrichenkoevgeny.backend.core.settings.di.SettingsModules
import io.github.mudrichenkoevgeny.backend.core.settings.usecase.system.globalsettings.SeedGlobalSettingsUseCase
import io.github.mudrichenkoevgeny.backend.core.storage.di.StorageModules
import io.github.mudrichenkoevgeny.backend.core.storage.scheduled.StorageScheduledJobs
import io.github.mudrichenkoevgeny.backend.core.storage.usecase.system.storagesettings.SeedStorageSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.di.UserModules
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.router.ManagementWebSocketRouter
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.router.OpenWebSocketRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.ManagementCoreUserRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.audit.ManagementAuditRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.security.ManagementSecuritySettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.settings.ManagementGlobalSettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.storage.ManagementStorageRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.storage.ManagementStorageSettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.management.storage.SelfManagementStorageRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.OpenCoreUserRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.security.OpenSecuritySettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.settings.OpenGlobalSettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.storage.OpenStorageRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.storage.OpenStorageSettingsRouter
import io.github.mudrichenkoevgeny.backend.feature.user.scheduled.UserScheduledJobs
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.system.adminaccounts.SeedAdminAccountsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.system.auth.settings.SeedAuthSettingsUseCase
import io.github.mudrichenkoevgeny.backend.sample.appbootstrap.AppBootstrap
import io.github.mudrichenkoevgeny.backend.sample.lifecycle.AppShutdownHook
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton
import javax.sql.DataSource

/**
 * Dagger application component for the sample host app.
 *
 * This component aggregates all SDK modules and exposes entry points (routers, providers, use
 * cases) needed by the sample application to start the server and register routes.
 */
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
        UserModules::class,
        AuditParsersModule::class,
        AppModule::class
    ]
)
public interface AppComponent {
    @Component.Factory
    public interface Factory {
        /**
         * Creates the application component.
         *
         * @param backgroundScope application-wide background CoroutineScope
         * that will be used by core/feature modules (audit, websockets, etc.).
         */
        public fun create(
            @BindsInstance @BackgroundScope backgroundScope: CoroutineScope
        ): AppComponent
    }

    // --- Common ---
    public fun commonConfig(): CommonConfig

    public fun appLogger(): AppLogger

    public fun appErrorParser(): AppErrorParser

    public fun healthCheckerManager(): HealthCheckerManager

    public fun managementHealthRouter(): ManagementHealthRouter

    // --- Observability ---
    public fun telemetryProvider(): TelemetryProvider

    public fun swaggerInitializer(): SwaggerInitializer

    // --- Database ---
    public fun databaseMigrator(): DatabaseMigrator

    public fun databaseConfig(): DatabaseConfig

    public fun dataSource(): DataSource

    // --- Audit ---
    public fun managementAuditRouter(): ManagementAuditRouter

    // --- Settings ---
    public fun seedGlobalSettingsUseCase(): SeedGlobalSettingsUseCase

    public fun openGlobalSettingsSettingsRouter(): OpenGlobalSettingsRouter

    public fun managementGlobalSettingsRouter(): ManagementGlobalSettingsRouter

    // --- Security ---
    public fun securitySettingsProvider(): SecuritySettingsProvider

    public fun seedSecuritySettingsUseCase(): SeedSecuritySettingsUseCase

    public fun ipRestrictionPolicyValidator(): IpRestrictionPolicyValidator

    public fun openSecuritySettingsRouter(): OpenSecuritySettingsRouter

    public fun managementSecuritySettingsRouter(): ManagementSecuritySettingsRouter

    // --- Storage ---
    public fun seedStorageSettingsUseCase(): SeedStorageSettingsUseCase

    public fun openStorageSettingsRouter(): OpenStorageSettingsRouter

    public fun managementStorageSettingsRouter(): ManagementStorageSettingsRouter

    public fun openStorageRouter(): OpenStorageRouter

    public fun selfManagementStorageRouter(): SelfManagementStorageRouter

    public fun managementStorageRouter(): ManagementStorageRouter

    public fun storageScheduledJobs(): StorageScheduledJobs

    // --- User ---
    public fun authenticationProvider(): AuthenticationProvider

    public fun seedAdminAccountsUseCase(): SeedAdminAccountsUseCase

    public fun seedAuthSettingsUseCase(): SeedAuthSettingsUseCase

    public fun openCoreUserRouter(): OpenCoreUserRouter

    public fun managementCoreUserRouter(): ManagementCoreUserRouter

    public fun openWebSocketRouter(): OpenWebSocketRouter

    public fun managementWebSocketRouter(): ManagementWebSocketRouter

    public fun userScheduledJobs(): UserScheduledJobs

    // --- App ---
    public fun appBootstrap(): AppBootstrap

    public fun appShutdownHook(): AppShutdownHook
}
