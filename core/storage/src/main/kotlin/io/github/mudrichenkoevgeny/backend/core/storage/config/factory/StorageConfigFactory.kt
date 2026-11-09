package io.github.mudrichenkoevgeny.backend.core.storage.config.factory

import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig

/**
 * Factory that builds [StorageConfig] from environment or other configuration source.
 */
public interface StorageConfigFactory {
    /**
     * Creates the storage module configuration.
     *
     * @return [StorageConfig] with the selected backend type and backend-specific settings.
     */
    public fun create(): StorageConfig
}
