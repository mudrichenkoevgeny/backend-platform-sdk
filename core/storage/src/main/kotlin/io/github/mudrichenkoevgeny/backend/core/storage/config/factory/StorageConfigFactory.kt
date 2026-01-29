package io.github.mudrichenkoevgeny.backend.core.storage.config.factory

import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig

interface StorageConfigFactory {
    fun create(): StorageConfig
}