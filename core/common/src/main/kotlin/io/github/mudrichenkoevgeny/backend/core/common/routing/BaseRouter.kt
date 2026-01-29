package io.github.mudrichenkoevgeny.backend.core.common.routing

import io.ktor.server.routing.Route

interface BaseRouter {
    fun register(route: Route)
}