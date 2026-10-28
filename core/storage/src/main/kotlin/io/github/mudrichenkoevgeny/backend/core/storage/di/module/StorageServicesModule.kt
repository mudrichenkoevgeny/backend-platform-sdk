package io.github.mudrichenkoevgeny.backend.core.storage.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.storage.service.S3StorageService
import io.github.mudrichenkoevgeny.backend.core.storage.service.StorageService
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProviderImpl
import javax.inject.Singleton

/**
 * Dagger module providing storage services and settings provider bindings.
 *
 * Binds [S3StorageService] to [StorageService] and [StorageSettingsProviderImpl] to [StorageSettingsProvider].
 */
@Module
interface StorageServicesModule {

    @Binds
    @Singleton
    fun bindStorageSettingsProvider(impl: StorageSettingsProviderImpl): StorageSettingsProvider

    @Binds
    @Singleton
    fun bindStorageService(impl: S3StorageService): StorageService
}
