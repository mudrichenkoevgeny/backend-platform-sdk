package io.github.mudrichenkoevgeny.backend.feature.user.config.factory

import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig

/**
 * Builds [UserConfig] for the host application.
 *
 * Implementations typically read environment variables and secret files, validate required values,
 * and assemble provider-specific config objects.
 */
interface UserConfigFactory {
    /**
     * Creates a fully initialized [UserConfig].
     *
     * @return created configuration
     */
    fun create(): UserConfig
}