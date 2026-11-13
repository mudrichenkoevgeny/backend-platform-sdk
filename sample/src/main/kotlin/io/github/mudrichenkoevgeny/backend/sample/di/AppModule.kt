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
 * Binds:
 * - [AppInfo] via [SampleAppInfo]
 * - [AppShutdownHook] via [AppShutdownHookImpl]
 * - [ShutdownHookRegistrar] via [RuntimeShutdownHookRegistrar]
 */
@Module
public interface AppModule {
    @Binds
    @Singleton
    public fun bindAppMetadata(impl: SampleAppInfo): AppInfo

    @Binds
    @Singleton
    public fun bindAppShutdownHook(appShutdownHookImpl: AppShutdownHookImpl): AppShutdownHook

    @Binds
    @Singleton
    public fun bindShutdownHookRegistrar(impl: RuntimeShutdownHookRegistrar): ShutdownHookRegistrar
}
