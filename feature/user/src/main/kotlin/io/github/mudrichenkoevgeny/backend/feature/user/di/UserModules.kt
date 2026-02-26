package io.github.mudrichenkoevgeny.backend.feature.user.di

import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserAuditModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserAuthInfrastructureModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserConfigModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserManagersModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserRepositoriesModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserServicesModule
import dagger.Module
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.AuthSettingsProviderModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserExternalAuthVerifierModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserWebSocketModule

@Module(
    includes = [
        UserConfigModule::class,
        UserAuthInfrastructureModule::class,
        UserExternalAuthVerifierModule::class,
        UserRepositoriesModule::class,
        UserManagersModule::class,
        UserServicesModule::class,
        UserAuditModule::class,
        AuthSettingsProviderModule::class,
        UserWebSocketModule::class
    ]
)
interface UserModules