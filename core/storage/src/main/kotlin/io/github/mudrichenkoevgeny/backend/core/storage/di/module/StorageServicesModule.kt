package io.github.mudrichenkoevgeny.backend.core.storage.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.storage.imgproxy.ImgproxyUrlBuilder
import io.github.mudrichenkoevgeny.backend.core.storage.imgproxy.ImgproxyUrlBuilderImpl
import io.github.mudrichenkoevgeny.backend.core.storage.service.S3StorageService
import io.github.mudrichenkoevgeny.backend.core.storage.service.StorageService
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProviderImpl
import javax.inject.Singleton

/**
 * Dagger module providing storage services and settings provider bindings.
 *
 * Binds [S3StorageService] to [StorageService], [ImgproxyUrlBuilderImpl] to [ImgproxyUrlBuilder],
 * and [StorageSettingsProviderImpl] to [StorageSettingsProvider].
 */
@Module
public interface StorageServicesModule {
    @Binds
    @Singleton
    public fun bindStorageSettingsProvider(impl: StorageSettingsProviderImpl): StorageSettingsProvider

    @Binds
    @Singleton
    public fun bindStorageService(impl: S3StorageService): StorageService

    @Binds
    @Singleton
    public fun bindImgproxyUrlBuilder(impl: ImgproxyUrlBuilderImpl): ImgproxyUrlBuilder
}
