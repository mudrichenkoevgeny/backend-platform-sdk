package io.github.mudrichenkoevgeny.backend.sample.di

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.AppInfo
import io.github.mudrichenkoevgeny.backend.sample.SampleAppInfo
import io.github.mudrichenkoevgeny.backend.sample.lifecycle.AppShutdownHook
import io.github.mudrichenkoevgeny.backend.sample.lifecycle.AppShutdownHookImpl
import javax.inject.Singleton

@Module
interface AppModule {
    @Binds
    @Singleton
    fun bindAppMetadata(impl: SampleAppInfo): AppInfo

    @Binds
    @Singleton
    fun bindsAppShutdownHook(appShutdownHookImpl: AppShutdownHookImpl): AppShutdownHook
}