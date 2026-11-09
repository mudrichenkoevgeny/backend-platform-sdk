package io.github.mudrichenkoevgeny.backend.core.settings.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsServiceImpl
import javax.inject.Singleton

/**
 * Dagger bindings for settings services.
 *
 * Binds [SystemSettingsService] to [SystemSettingsServiceImpl].
 */
@Module
public interface SettingsServicesModule {
    @Binds
    @Singleton
    public fun bindSystemSettingsService(systemSettingsServiceImpl: SystemSettingsServiceImpl): SystemSettingsService
}
