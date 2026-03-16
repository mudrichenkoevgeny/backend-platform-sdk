package io.github.mudrichenkoevgeny.backend.core.common.routing

import io.ktor.server.routing.Route

/**
 * Abstraction for feature‑specific routers that register their endpoints
 * on the provided Ktor [Route] tree.
 *
 * Typical usage is to inject implementations and call [register] from a central
 * routing module to keep feature routes isolated and testable.
 */
interface BaseRouter {

    /**
     * Registers all routes belonging to this router under the given [route].
     */
    fun register(route: Route)
}