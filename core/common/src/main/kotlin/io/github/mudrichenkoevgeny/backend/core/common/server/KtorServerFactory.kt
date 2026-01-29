package io.github.mudrichenkoevgeny.backend.core.common.server

import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.CommonConfig
import io.ktor.server.application.Application
import io.ktor.server.application.serverConfig
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.logging.KtorSimpleLogger

object KtorServer {
    private const val KTOR_DEFAULT_LOGGER = "io.ktor.server.Application"

    fun create(
        commonConfig: CommonConfig,
        applicationModule: Application.() -> Unit
    ): EmbeddedServer<*, *> {
        val environment = applicationEnvironment {
            log = KtorSimpleLogger(KTOR_DEFAULT_LOGGER)
        }

        val rootConfig = serverConfig(environment) {
            module(applicationModule)
        }

        return embeddedServer(Netty, rootConfig) {
            connectors.add(EngineConnectorBuilder().apply {
                host = commonConfig.ktorServerHost
                port = commonConfig.ktorServerPort
            })
            connectors.add(EngineConnectorBuilder().apply {
                host = commonConfig.ktorServerHost
                port = commonConfig.ktorManagementPort
            })
        }
    }
}