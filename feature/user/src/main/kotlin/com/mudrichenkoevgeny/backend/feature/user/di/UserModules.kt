package io.github.mudrichenkoevgeny.backend.feature.user.di

import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserAuditModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserAuthInfrastructureModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserConfigModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserManagersModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserRepositoriesModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserServicesModule
import io.github.mudrichenkoevgeny.backend.feature.user.di.module.UserTablesModule
import dagger.Module

@Module(
    includes = [
        UserConfigModule::class,
        UserAuthInfrastructureModule::class,
        UserRepositoriesModule::class,
        UserManagersModule::class,
        UserServicesModule::class,
        UserAuditModule::class,
        UserTablesModule::class
    ]
)
interface UserModules