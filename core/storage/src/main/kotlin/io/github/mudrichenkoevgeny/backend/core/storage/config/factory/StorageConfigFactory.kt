package io.github.mudrichenkoevgeny.backend.core.storage.config.factory

import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig

/**
 * Factory that builds [StorageConfig] from environment or other configuration source.
 */
interface StorageConfigFactory {

    /**
     * Creates the storage module configuration.
     *
     * @return [StorageConfig] with the selected backend type and backend-specific settings.
     */
    fun create(): StorageConfig
}