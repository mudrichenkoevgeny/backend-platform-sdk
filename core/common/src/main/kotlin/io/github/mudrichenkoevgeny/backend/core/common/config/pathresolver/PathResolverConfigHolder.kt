package io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver

/**
 * Holds mutable [PathResolverConfig] that can be set by the host application.
 *
 * The SDK does not depend on any specific DI container here, so the configuration can be
 * provided during bootstrap and replaced later if paths need to change without restarting
 * the process. When no config is set explicitly, a default [PathResolverConfig] is used.
 */
object PathResolverConfigHolder {
    private var config: PathResolverConfig? = null

    fun set(config: PathResolverConfig) {
        this.config = config
    }

    fun get(): PathResolverConfig = config ?: PathResolverConfig()
}