package io.github.mudrichenkoevgeny.backend.feature.user.di

import dagger.Module
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserAuthInfrastructureModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserConfigModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserManagersModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserRepositoriesModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserServicesModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.AuthSettingsProviderModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.EmailParserModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserAuditErrorParserModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserExternalAuthVerifierModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserScheduledJobsModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserWebSocketModule

/**
 * Aggregates all Dagger modules required by the user feature.
 *
 * Host applications should include this module (or its submodules) in their application component
 * to install configuration, repositories, managers, services, audit logging, and WebSocket wiring.
 */
@Module(
    includes = [
        UserConfigModule::class,
        UserAuthInfrastructureModule::class,
        UserExternalAuthVerifierModule::class,
        UserRepositoriesModule::class,
        UserManagersModule::class,
        UserServicesModule::class,
        AuthSettingsProviderModule::class,
        UserWebSocketModule::class,
        EmailParserModule::class,
        UserAuditErrorParserModule::class,
        UserScheduledJobsModule::class
    ]
)
interface UserModules