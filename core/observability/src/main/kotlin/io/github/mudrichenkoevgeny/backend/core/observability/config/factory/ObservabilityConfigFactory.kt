package io.github.mudrichenkoevgeny.backend.core.observability.config.factory

import io.github.mudrichenkoevgeny.backend.core.observability.config.model.ObservabilityConfig

interface ObservabilityConfigFactory {
    fun create(): ObservabilityConfig
}