package io.github.mudrichenkoevgeny.backend.core.security.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProviderImpl
import javax.inject.Singleton

@Module
interface SecuritySettingsProviderModule {

    @Binds
    @Singleton
    fun bindSecuritySettingsProvider(securitySettingsProviderImpl: SecuritySettingsProviderImpl): SecuritySettingsProvider
}