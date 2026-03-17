package io.github.mudrichenkoevgeny.backend.core.settings.config.factory

import io.github.mudrichenkoevgeny.backend.core.settings.config.model.SettingsConfig

/**
 * Creates a [SettingsConfig] instance from the current runtime environment.
 *
 * The factory exists to keep configuration parsing separate from DI wiring and to make the
 * configuration creation testable.
 */
interface SettingsConfigFactory {
    /**
     * Reads configuration values and returns a new immutable [SettingsConfig].
     */
    fun create(): SettingsConfig
}