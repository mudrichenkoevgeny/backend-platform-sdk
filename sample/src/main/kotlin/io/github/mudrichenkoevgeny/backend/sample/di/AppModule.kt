package io.github.mudrichenkoevgeny.backend.sample.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.AppInfo
import io.github.mudrichenkoevgeny.backend.sample.SampleAppInfo
import io.github.mudrichenkoevgeny.backend.sample.lifecycle.AppShutdownHook
import io.github.mudrichenkoevgeny.backend.sample.lifecycle.AppShutdownHookImpl
import io.github.mudrichenkoevgeny.backend.sample.lifecycle.RuntimeShutdownHookRegistrar
import io.github.mudrichenkoevgeny.backend.sample.lifecycle.ShutdownHookRegistrar
import java.time.Clock
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
public interface AppModule {
    @Binds
    @Singleton
    public fun bindAppMetadata(impl: SampleAppInfo): AppInfo

    @Binds
    @Singleton
    public fun bindsAppShutdownHook(appShutdownHookImpl: AppShutdownHookImpl): AppShutdownHook

    @Binds
    @Singleton
    public fun bindShutdownHookRegistrar(impl: RuntimeShutdownHookRegistrar): ShutdownHookRegistrar

    public companion object {
        @Provides
        @Singleton
        public fun provideClock(): Clock = Clock.systemUTC()
    }
}
