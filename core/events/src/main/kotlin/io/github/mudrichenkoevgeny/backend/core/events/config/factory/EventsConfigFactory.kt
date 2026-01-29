package io.github.mudrichenkoevgeny.backend.core.events.config.factory

import io.github.mudrichenkoevgeny.backend.core.events.config.model.EventsConfig

interface EventsConfigFactory {
    fun create(): EventsConfig
}