package io.github.mudrichenkoevgeny.backend.core.storage.di.module

import dagger.Binds
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig
import io.github.mudrichenkoevgeny.backend.core.storage.model.StorageType
import io.github.mudrichenkoevgeny.backend.core.storage.service.LocalStorageService
import io.github.mudrichenkoevgeny.backend.core.storage.service.S3StorageService
import io.github.mudrichenkoevgeny.backend.core.storage.service.StorageService
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProviderImpl
import dagger.Module
import dagger.Provides
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Dagger module providing storage services and settings provider bindings.
 *
 * Binds [StorageService] based on [StorageConfig.storageType] and [StorageSettingsProviderImpl] to [StorageSettingsProvider].
 */
@Module
abstract class StorageServicesModule {

    @Binds
    @Singleton
    abstract fun bindStorageSettingsProvider(impl: StorageSettingsProviderImpl): StorageSettingsProvider

    companion object {

        /**
         * Provides the active [StorageService] implementation based on [StorageConfig.storageType].
         */
        @Provides
        @Singleton
        fun provideStorageService(
            storageConfig: StorageConfig,
            localStorage: Provider<LocalStorageService>,
            s3Storage: Provider<S3StorageService>
        ): StorageService {
            val storageType = storageConfig.storageType
            return when (storageType) {
                StorageType.LOCAL -> localStorage.get()
                StorageType.S3 -> s3Storage.get()
            }
        }
    }
}