package io.github.mudrichenkoevgeny.backend.core.events.config.factory

import io.github.mudrichenkoevgeny.backend.core.events.config.model.EventsConfig

/**
 * Factory that builds [EventsConfig] from environment or other configuration source.
 */
public interface EventsConfigFactory {
    /**
     * Creates the events module configuration.
     *
     * @return [EventsConfig] with transport type and Kafka parameters.
     */
    public fun create(): EventsConfig
}
