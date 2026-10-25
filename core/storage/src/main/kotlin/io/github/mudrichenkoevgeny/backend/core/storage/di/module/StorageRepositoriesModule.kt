package io.github.mudrichenkoevgeny.backend.core.storage.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.storage.database.repository.StorageFilesRepository
import io.github.mudrichenkoevgeny.backend.core.storage.database.repository.StorageFilesRepositoryImpl
import javax.inject.Singleton

/**
 * Dagger module providing bindings for storage database repositories.
 *
 * Binds [StorageFilesRepositoryImpl] to [StorageFilesRepository].
 */
@Module
interface StorageRepositoriesModule {

    /**
     * Binds the Exposed repository implementation for storage files metadata.
     */
    @Binds
    @Singleton
    fun bindStorageFilesRepository(impl: StorageFilesRepositoryImpl): StorageFilesRepository
}