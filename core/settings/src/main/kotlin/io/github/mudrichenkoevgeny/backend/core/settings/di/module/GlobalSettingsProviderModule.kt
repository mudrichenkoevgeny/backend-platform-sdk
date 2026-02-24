package io.github.mudrichenkoevgeny.backend.core.settings.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProviderImpl
import javax.inject.Singleton

@Module
interface GlobalSettingsProviderModule {

    @Binds
    @Singleton
    fun bindGlobalSettingsProvider(globalSettingsProviderImpl: GlobalSettingsProviderImpl): GlobalSettingsProvider
}