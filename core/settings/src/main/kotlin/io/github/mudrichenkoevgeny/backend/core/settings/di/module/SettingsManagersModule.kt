package io.github.mudrichenkoevgeny.backend.core.settings.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.settings.manager.SystemSettingsManager
import io.github.mudrichenkoevgeny.backend.core.settings.manager.SystemSettingsManagerImpl
import javax.inject.Singleton

/**
 * Dagger bindings for settings managers.
 *
 * Binds [SystemSettingsManager] to [SystemSettingsManagerImpl].
 */
@Module
interface SettingsManagersModule {

    @Binds
    @Singleton
    fun bindSystemSettingsManager(systemSettingsManagerImpl: SystemSettingsManagerImpl): SystemSettingsManager
}