package io.github.mudrichenkoevgeny.backend.core.storage.di.module

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.storage.config.factory.StorageConfigFactory
import io.github.mudrichenkoevgeny.backend.core.storage.config.factory.StorageConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig
import javax.inject.Singleton

/**
 * Dagger module that provides [StorageConfigFactory] (backed by [StorageConfigFactoryImpl]) and [StorageConfig] (created by the factory).
 */
@Module
public class StorageConfigModule {
    @Provides
    @Singleton
    public fun provideStorageConfigFactory(envReader: EnvReader): StorageConfigFactory = StorageConfigFactoryImpl(
        envReader = envReader
    )

    @Provides
    @Singleton
    public fun provideStorageConfig(storageConfigFactory: StorageConfigFactory): StorageConfig =
        storageConfigFactory.create()
}
