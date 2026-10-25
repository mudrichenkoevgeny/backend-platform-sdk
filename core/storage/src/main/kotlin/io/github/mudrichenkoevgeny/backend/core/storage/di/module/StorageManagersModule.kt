package io.github.mudrichenkoevgeny.backend.core.storage.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManagerImpl
import javax.inject.Singleton

/**
 * Binds managers coordinating core storage components.
 *
 * Managers are used by use cases and routes to coordinate repositories and services.
 */
@Module
interface StorageManagersModule {

    @Binds
    @Singleton
    fun bindStorageManager(storageManagerImpl: StorageManagerImpl): StorageManager
}