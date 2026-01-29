package io.github.mudrichenkoevgeny.backend.core.common.config.common.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.CommonConfig

interface CommonConfigFactory {
    fun create(): CommonConfig
}