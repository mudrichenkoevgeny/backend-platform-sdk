package io.github.mudrichenkoevgeny.backend.core.common.di

import io.github.mudrichenkoevgeny.backend.core.common.di.module.BaseAppConfigModule
import io.github.mudrichenkoevgeny.backend.core.common.di.module.AppErrorParserModule
import io.github.mudrichenkoevgeny.backend.core.common.di.module.AppLoggerModule
import io.github.mudrichenkoevgeny.backend.core.common.di.module.CoroutineScopeModule
import io.github.mudrichenkoevgeny.backend.core.common.di.module.EnvModule
import io.github.mudrichenkoevgeny.backend.core.common.di.module.SwaggerModule
import dagger.Module

@Module(
    includes = [
        CoroutineScopeModule::class,
        EnvModule::class,
        BaseAppConfigModule::class,
        AppErrorParserModule::class,
        AppLoggerModule::class,
        SwaggerModule::class
    ]
)
interface CommonModules