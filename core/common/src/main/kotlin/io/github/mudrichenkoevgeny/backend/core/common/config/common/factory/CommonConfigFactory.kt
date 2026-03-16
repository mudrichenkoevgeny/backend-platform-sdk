package io.github.mudrichenkoevgeny.backend.core.common.config.common.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.CommonConfig

/**
 * Factory for building immutable [CommonConfig] instances.
 */
interface CommonConfigFactory {

    /**
     * Creates a new snapshot of the current common configuration.
     */
    fun create(): CommonConfig
}