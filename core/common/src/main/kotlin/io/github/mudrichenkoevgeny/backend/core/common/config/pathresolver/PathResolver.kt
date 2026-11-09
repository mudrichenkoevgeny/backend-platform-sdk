package io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver

/**
 * Resolves filesystem paths required by the configuration subsystem.
 *
 * Implementations can use different strategies (working directory, environment variables, etc.)
 * but must always return absolute [ResolvedPaths].
 */
public interface PathResolver {
    /**
     * Returns current resolved paths for configuration files and secrets.
     */
    public fun getResolvedPaths(): ResolvedPaths
}
