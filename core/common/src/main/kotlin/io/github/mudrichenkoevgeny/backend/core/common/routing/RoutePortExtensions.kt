package io.github.mudrichenkoevgeny.backend.core.common.routing

import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext

fun Route.onPort(port: Int, build: Route.() -> Unit): Route {
    val routeWithPort = createChild(object : RouteSelector() {
        override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
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