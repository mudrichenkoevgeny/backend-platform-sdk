package io.github.mudrichenkoevgeny.backend.feature.user.network.application

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.installTestAuth
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder

fun ApplicationTestBuilder.setupManagementTestEnvironment(router: BaseRouter? = null): String {
    val signedToken = JWT.create()
        .withSubject("00000000-0000-0000-0000-000000000001")
        .sign(Algorithm.HMAC256("test-secret"))

    application {
        install(ContentNegotiation) {
            json(FoundationJson)
        }
        installTestAuth()

        router?.let { router ->
            routing {
                router.register(this)
            }
        }
    }

    client.config {
        install(ClientContentNegotiation) {
            json(FoundationJson)
        }
    }

    return signedToken
}

fun ApplicationTestBuilder.setupOpenTestEnvironment(router: BaseRouter? = null) {
    application {
        install(ContentNegotiation) {
            json(FoundationJson)
        }

        router?.let { router ->
            routing {
                router.register(this)
            }
        }
    }

    client.config {
        install(ClientContentNegotiation) {
            json(FoundationJson)
        }
    }
}