package io.github.mudrichenkoevgeny.backend.core.common.config.swagger.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.model.SwaggerConfig

/**
 * Factory for producing [SwaggerConfig] instances.
 */
public interface SwaggerConfigFactory {
    /**
     * Creates a new snapshot of the current Swagger configuration.
     */
    public fun create(): SwaggerConfig
}
