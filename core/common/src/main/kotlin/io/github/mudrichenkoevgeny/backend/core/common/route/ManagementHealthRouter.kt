package io.github.mudrichenkoevgeny.backend.core.common.route

import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.CommonConfig
import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheckerManager
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.onPort
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Health check router providing infrastructure probes on the management connector.
 *
 * Exposes Liveness and Readiness endpoints intended for orchestrators and reverse proxies (e.g. Kubernetes, Nginx):
 * - [LIVE_PATH] (`/health/live`): Indicates that the Ktor process is up and running. Returns HTTP 200 OK.
 * - [READY_PATH] (`/health/ready`): Evaluates critical infrastructure via [HealthCheckerManager.checkCriticalHealth].
 *   Returns HTTP 200 OK when critical services (database, Redis, etc.) are available, or HTTP 503 Service Unavailable otherwise.
 *
 * Routes are restricted to [CommonConfig.ktorManagementPort] using [onPort].
 */
@Singleton
public class ManagementHealthRouter @Inject constructor(
    private val commonConfig: CommonConfig,
    private val healthCheckerManager: HealthCheckerManager
) : BaseRouter {
    /**
     * Registers the health check routes on the management port.
     *
     * @param route top-level [Route] where management health routes will be installed
     */
    override fun register(route: Route) {
        route.onPort(commonConfig.ktorManagementPort) {
            route(
                path = BASE_PATH,
                build = {
                    get(
                        path = LIVE_PATH,
                        builder = { configureOpenApiHidden() },
                        body = { handleLiveness() }
                    )
                    get(
                        path = READY_PATH,
                        builder = { configureOpenApiHidden() },
                        body = { handleReadiness() }
                    )
                }
            )
        }
    }

    private fun RouteConfig.configureOpenApiHidden() {
        hidden = true
    }

    private suspend fun RoutingContext.handleLiveness() {
        call.respond(HttpStatusCode.OK)
    }

    private suspend fun RoutingContext.handleReadiness() {
        val result = healthCheckerManager.checkCriticalHealth()
        if (result is AppSystemResult.Success) {
            call.respond(HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.ServiceUnavailable)
        }
    }

    public companion object {
        /** Base URL segment for all health endpoints. */
        public const val BASE_PATH: String = "/health"

        /** Liveness check endpoint path. */
        public const val LIVE_PATH: String = "/live"

        /** Readiness check endpoint path. */
        public const val READY_PATH: String = "/ready"
    }
}
