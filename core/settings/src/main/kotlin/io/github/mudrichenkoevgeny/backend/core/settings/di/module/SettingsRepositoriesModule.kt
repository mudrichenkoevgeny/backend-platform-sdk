package io.github.mudrichenkoevgeny.backend.core.settings.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.settings.database.repository.SystemSettingsRepository
import io.github.mudrichenkoevgeny.backend.core.settings.database.repository.SystemSettingsRepositoryImpl
import javax.inject.Singleton

/**
 * Dagger bindings for settings repositories.
 *
 * Binds [SystemSettingsRepository] to [SystemSettingsRepositoryImpl].
 */
@Module
public interface SettingsRepositoriesModule {
    @Binds
    @Singleton
    public fun bindSystemSettingRepository(
        systemSettingRepositoryImpl: SystemSettingsRepositoryImpl
    ): SystemSettingsRepository
}
