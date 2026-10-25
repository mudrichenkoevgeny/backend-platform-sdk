package io.github.mudrichenkoevgeny.backend.core.storage.di

import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.storage.di.module.StorageAppErrorParserModule
import io.github.mudrichenkoevgeny.backend.core.storage.di.module.StorageConfigModule
import io.github.mudrichenkoevgeny.backend.core.storage.di.module.StorageRepositoriesModule
import io.github.mudrichenkoevgeny.backend.core.storage.di.module.StorageManagersModule
import io.github.mudrichenkoevgeny.backend.core.storage.di.module.StorageServicesModule
import io.github.mudrichenkoevgeny.backend.core.storage.service.StorageService

/**
 * Aggregate Dagger module for the storage core feature.
 *
 * Includes the following submodules:
 * - [StorageConfigModule]: Provides configuration factory and storage configuration settings.
 * - [StorageServicesModule]: Binds [StorageService] implementations based on config.
 * - [StorageRepositoriesModule]: Binds data access repositories for storage files and settings.
 * - [StorageManagersModule]: Binds storage management coordinators.
 * - [StorageAppErrorParserModule]: Binds storage error parser into error parser set.
 */
@Module(
    includes = [
        StorageConfigModule::class,
        StorageServicesModule::class,
        StorageRepositoriesModule::class,
        StorageManagersModule::class,
        StorageAppErrorParserModule::class
    ]
)
interface StorageModules