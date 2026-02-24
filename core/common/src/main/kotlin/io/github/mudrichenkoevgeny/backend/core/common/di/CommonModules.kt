package io.github.mudrichenkoevgeny.backend.core.common.di

import io.github.mudrichenkoevgeny.backend.core.common.di.module.CommonConfigModule
import io.github.mudrichenkoevgeny.backend.core.common.di.module.AppErrorParserModule
import io.github.mudrichenkoevgeny.backend.core.common.di.module.AppLoggerModule
import io.github.mudrichenkoevgeny.backend.core.common.di.module.CoroutineScopeModule
import io.github.mudrichenkoevgeny.backend.core.common.di.module.EnvModule
import io.github.mudrichenkoevgeny.backend.core.common.di.module.SwaggerModule
import io.github.mudrichenkoevgeny.backend.core.common.di.module.WebSocketModule
import dagger.Module

@Module(
    includes = [
        CoroutineScopeModule::class,
        EnvModule::class,
        CommonConfigModule::class,
        AppErrorParserModule::class,
        AppLoggerModule::class,
        SwaggerModule::class,
        WebSocketModule::class
    ]
)
interface CommonModules