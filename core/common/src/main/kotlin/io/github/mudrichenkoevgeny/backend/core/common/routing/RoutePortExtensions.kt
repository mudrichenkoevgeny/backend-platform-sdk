package io.github.mudrichenkoevgeny.backend.core.common.routing

import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext

/**
 * Restricts a nested route to be resolved only for requests that were received on the given [port].
 *
 * This is useful when the same application hosts multiple connectors (e.g. public API port and
 * management/health port) and certain routes must be available only on one of them.
 *
 * Usage:
 * ```kotlin
 * routing {
 *     onPort(8080) {
 *         get("/public") { ... }
 *     }
 *     onPort(8081) {
 *         get("/health") { ... }
 *     }
 * }
 * ```
 */
fun Route.onPort(port: Int, build: Route.() -> Unit): Route {
    val routeWithPort = createChild(object : RouteSelector() {
        override suspend fun evaluate(
            context: RoutingResolveContext,
            segmentIndex: Int
        ): RouteSelectorEvaluation {
            return if (context.call.request.local.localPort == port) {
                RouteSelectorEvaluation.Constant
            } else {
                RouteSelectorEvaluation.Failed
            }
        }
    })
    routeWithPort.build()
    return routeWithPort
}