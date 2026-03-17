package io.github.mudrichenkoevgeny.backend.core.storage.di.module

import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig
import io.github.mudrichenkoevgeny.backend.core.storage.model.StorageType
import io.github.mudrichenkoevgeny.backend.core.storage.service.LocalStorageService
import io.github.mudrichenkoevgeny.backend.core.storage.service.S3StorageService
import io.github.mudrichenkoevgeny.backend.core.storage.service.StorageService
import dagger.Module
import dagger.Provides
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Dagger module that provides a single [StorageService] implementation based on [StorageConfig.storageType].
 *
 * Uses providers to avoid constructing both implementations eagerly.
 */
@Module
class StorageServicesModule {

    @Provides
    @Singleton
    fun provideStorageService(
        storageConfig: StorageConfig,
        localStorage: Provider<LocalStorageService>,
        s3Storage: Provider<S3StorageService>
    ): StorageService {
        return when (storageConfig.storageType) {
            StorageType.LOCAL -> localStorage.get()
            StorageType.S3 -> s3Storage.get()
        }
    }
}