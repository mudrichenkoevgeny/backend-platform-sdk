package io.github.mudrichenkoevgeny.backend.core.security.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProviderImpl
import javax.inject.Singleton

/**
 * Dagger bindings for security settings access.
 *
 * Binds [SecuritySettingsProvider] to [SecuritySettingsProviderImpl].
 */
@Module
public interface SecuritySettingsProviderModule {
    @Binds
    @Singleton
    public fun bindSecuritySettingsProvider(
        securitySettingsProviderImpl: SecuritySettingsProviderImpl
    ): SecuritySettingsProvider
}
