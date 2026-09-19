package io.github.mudrichenkoevgeny.backend.core.common.route

import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.CommonConfig
import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheckerManager
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.onPort
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
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
class ManagementHealthRouter @Inject constructor(
    private val commonConfig: CommonConfig,
    private val healthCheckerManager: HealthCheckerManager
) : BaseRouter {

    companion object {
        /** Base URL segment for all health endpoints. */
        const val BASE_PATH = "/health"

        /** Liveness check endpoint path. */
        const val LIVE_PATH = "/live"

        /** Readiness check endpoint path. */
        const val READY_PATH = "/ready"
    }

    /**
     * Registers the health check routes on the management port.
     *
     * @param route top-level [Route] where management health routes will be installed
     */
    override fun register(route: Route) {
        route.onPort(commonConfig.ktorManagementPort) {
            route(BASE_PATH) {
                get(
                    path = LIVE_PATH,
                    builder = {
                        hidden = true
                    },
                    body = {
                        call.respond(HttpStatusCode.OK)
                    }
                )
                get(
                    path = READY_PATH,
                    builder = {
                        hidden = true
                    },
                    body = {
                        val result = healthCheckerManager.checkCriticalHealth()
                        if (result is AppSystemResult.Success) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.ServiceUnavailable)
                        }
                    }
                )
            }
        }
    }
}