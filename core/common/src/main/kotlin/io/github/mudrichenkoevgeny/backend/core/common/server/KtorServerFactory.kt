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

/**
 * Factory for creating a pre‑configured Ktor [EmbeddedServer] used across services.
 *
 * Responsibilities:
 * - Configure a shared application logger.
 * - Wire the provided application module as the Ktor module.
 * - Expose two connectors:
 *   - main API connector (`ktorServerHost` : `ktorServerPort`)
 *   - management/monitoring connector (`ktorServerHost` : `ktorManagementPort`)
 */
object KtorServer {
    private const val KTOR_DEFAULT_LOGGER = "io.ktor.server.Application"

    /**
     * Creates a Netty‑based [EmbeddedServer] using ports from [commonConfig] and the given [applicationModule].
     *
     * @param commonConfig configuration with host and ports for main and management connectors
     * @param applicationModule Ktor application module to install into the server environment
     */
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