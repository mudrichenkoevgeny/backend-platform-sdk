package com.mudrichenkoevgeny.backend.feature.user.di

import com.mudrichenkoevgeny.backend.feature.user.di.module.UserAuditModule
import com.mudrichenkoevgeny.backend.feature.user.di.module.UserAuthInfrastructureModule
import com.mudrichenkoevgeny.backend.feature.user.di.module.UserConfigModule
import com.mudrichenkoevgeny.backend.feature.user.di.module.UserManagersModule
import com.mudrichenkoevgeny.backend.feature.user.di.module.UserRepositoriesModule
import com.mudrichenkoevgeny.backend.feature.user.di.module.UserServicesModule
import com.mudrichenkoevgeny.backend.feature.user.di.module.UserTablesModule
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