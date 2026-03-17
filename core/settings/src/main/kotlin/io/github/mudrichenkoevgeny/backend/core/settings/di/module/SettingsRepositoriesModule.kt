package io.github.mudrichenkoevgeny.backend.core.settings.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.settings.database.repository.SystemSettingRepository
import io.github.mudrichenkoevgeny.backend.core.settings.database.repository.SystemSettingRepositoryImpl
import javax.inject.Singleton

/**
 * Dagger bindings for settings repositories.
 *
 * Binds [SystemSettingRepository] to [SystemSettingRepositoryImpl].
 */
@Module
interface SettingsRepositoriesModule {

    @Binds
    @Singleton
    fun bindSystemSettingRepository(systemSettingRepositoryImpl: SystemSettingRepositoryImpl): SystemSettingRepository
}