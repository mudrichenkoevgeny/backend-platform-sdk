package io.github.mudrichenkoevgeny.backend.sample.lifecycle

import io.ktor.server.engine.EmbeddedServer

interface AppShutdownHook {
    fun register(server: EmbeddedServer<*, *>)
}