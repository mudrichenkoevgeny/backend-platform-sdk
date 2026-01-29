package io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver

interface PathResolver {
    fun getResolvedPaths(): ResolvedPaths
}