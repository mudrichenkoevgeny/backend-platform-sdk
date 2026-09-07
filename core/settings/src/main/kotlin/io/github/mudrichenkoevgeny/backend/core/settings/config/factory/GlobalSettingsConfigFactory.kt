package io.github.mudrichenkoevgeny.backend.core.settings.config.factory

import io.github.mudrichenkoevgeny.backend.core.settings.config.model.GlobalSettingsConfig

/**
 * Creates a [GlobalSettingsConfig] instance from the current runtime environment.
 *
 * The factory exists to keep configuration parsing separate from DI wiring and to make the
 * configuration creation testable.
 */
interface GlobalSettingsConfigFactory {
    /**
     * Reads configuration values and returns a new immutable [GlobalSettingsConfig].
     */
    fun create(): GlobalSettingsConfig
}