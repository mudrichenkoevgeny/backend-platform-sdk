package io.github.mudrichenkoevgeny.backend.core.settings.config.factory

import io.github.mudrichenkoevgeny.backend.core.settings.config.model.SettingsConfig

interface SettingsConfigFactory {
    fun create(): SettingsConfig
}