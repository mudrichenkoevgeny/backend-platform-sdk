package io.github.mudrichenkoevgeny.backend.sample.di

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.AppInfo
import io.github.mudrichenkoevgeny.backend.sample.SampleAppInfo
import io.github.mudrichenkoevgeny.backend.sample.lifecycle.AppShutdownHook
import io.github.mudrichenkoevgeny.backend.sample.lifecycle.AppShutdownHookImpl
import io.github.mudrichenkoevgeny.backend.sample.lifecycle.RuntimeShutdownHookRegistrar
import io.github.mudrichenkoevgeny.backend.sample.lifecycle.ShutdownHookRegistrar
import javax.inject.Singleton

/**
 * Sample app-specific Dagger bindings.
 *
 * Provides:
 * - [AppInfo] as [SampleAppInfo]
 * - [AppShutdownHook] as [AppShutdownHookImpl]
 * - [ShutdownHookRegistrar] as [RuntimeShutdownHookRegistrar]
 */
@Module
interface AppModule {
    @Binds
    @Singleton
    fun bindAppMetadata(impl: SampleAppInfo): AppInfo

    @Binds
    @Singleton
    fun bindsAppShutdownHook(appShutdownHookImpl: AppShutdownHookImpl): AppShutdownHook

    @Binds
    @Singleton
    fun bindShutdownHookRegistrar(impl: RuntimeShutdownHookRegistrar): ShutdownHookRegistrar
}