package io.github.mudrichenkoevgeny.backend.core.storage.di

import io.github.mudrichenkoevgeny.backend.core.storage.di.module.StorageConfigModule
import io.github.mudrichenkoevgeny.backend.core.storage.di.module.StorageServicesModule
import io.github.mudrichenkoevgeny.backend.core.storage.service.StorageService
import dagger.Module

/**
 * Aggregate Dagger module for the storage feature.
 *
 * Includes [StorageConfigModule] (config factory and config) and [StorageServicesModule] (selects [StorageService] implementation based on configuration).
 */
@Module(
    includes = [
        StorageConfigModule::class,
        StorageServicesModule::class
    ]
)
interface StorageModules