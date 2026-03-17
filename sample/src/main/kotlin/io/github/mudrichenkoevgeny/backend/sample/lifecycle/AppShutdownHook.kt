package io.github.mudrichenkoevgeny.backend.sample.lifecycle

import io.ktor.server.engine.EmbeddedServer

/**
 * Installs graceful shutdown handling for the running application.
 *
 * Implementations are expected to register a JVM shutdown hook and stop the provided [EmbeddedServer]
 * while also releasing infrastructure resources (database, redis, background jobs, etc.).
 */
interface AppShutdownHook {
    /**
     * Registers a shutdown hook for the given [server].
     */
    fun register(server: EmbeddedServer<*, *>)
}