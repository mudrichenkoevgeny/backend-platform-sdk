package io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver

object PathResolverConfigHolder {
    private var config: PathResolverConfig? = null

    fun set(config: PathResolverConfig) {
        this.config = config
    }

    fun get(): PathResolverConfig = config ?: PathResolverConfig()
}