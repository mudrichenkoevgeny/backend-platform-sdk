package io.github.mudrichenkoevgeny.backend.feature.user.config.factory

import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig

interface UserConfigFactory {
    fun create(): UserConfig
}