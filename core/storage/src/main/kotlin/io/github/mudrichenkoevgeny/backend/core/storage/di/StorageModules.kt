package io.github.mudrichenkoevgeny.backend.core.storage.di

import io.github.mudrichenkoevgeny.backend.core.storage.di.module.StorageConfigModule
import io.github.mudrichenkoevgeny.backend.core.storage.di.module.StorageServicesModule
import dagger.Module

@Module(
    includes = [
        StorageConfigModule::class,
        StorageServicesModule::class
    ]
)
interface StorageModules