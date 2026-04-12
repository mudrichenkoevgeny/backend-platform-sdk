package io.github.mudrichenkoevgeny.backend.core.common.di

import io.github.mudrichenkoevgeny.backend.core.common.di.module.AppErrorParserModule
import io.github.mudrichenkoevgeny.backend.core.common.di.module.AppLoggerModule
import io.github.mudrichenkoevgeny.backend.core.common.di.module.CommonConfigModule
import io.github.mudrichenkoevgeny.backend.core.common.di.module.EnvModule
import io.github.mudrichenkoevgeny.backend.core.common.di.module.SwaggerModule
import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BackgroundScope
import dagger.Module

/**
 * Aggregates core common modules that are typically used by backend applications.
 *
 * This module does not create application-level primitives (such as coroutine scopes) by itself.
 * Instead, it expects them (e.g. [BackgroundScope])
 * to be provided by the application component and wires library functionality on top of them.
 */
@Module(
    includes = [
        EnvModule::class,
        CommonConfigModule::class,
        AppErrorParserModule::class,
        AppLoggerModule::class,
        SwaggerModule::class
    ]
)
interface CommonModules