package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProviderImpl
import javax.inject.Singleton

@Module
interface AuthSettingsProviderModule {

    @Binds
    @Singleton
    fun bindAuthSettingsProvider(authSettingsProviderImpl: AuthSettingsProviderImpl): AuthSettingsProvider
}