package io.github.mudrichenkoevgeny.backend.feature.user.route

import io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.AuthRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.configuration.UserConfigurationRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.security.UserSecurityRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.session.SessionRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.user.UserRouter
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.basic
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class UserFeatureRouterTest {

    @Test
    fun `register delegates to all subrouters`() {
        val authRouter = mockk<AuthRouter>(relaxed = true)
        val userRouter = mockk<UserRouter>(relaxed = true)
        val sessionRouter = mockk<SessionRouter>(relaxed = true)
        val userSecurityRouter = mockk<UserSecurityRouter>(relaxed = true)
        val userConfigurationRouter = mockk<UserConfigurationRouter>(relaxed = true)

        val router = UserRouter(
            authRouter = authRouter,
            userRouter = userRouter,
            sessionRouter = sessionRouter,
            userSecurityRouter = userSecurityRouter,
            userConfigurationRouter = userConfigurationRouter
        )

        testApplication {
            application {
                install(Authentication) {
                    basic(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
                        validate { null }
                    }
                }
                routing {
                    router.register(this)
                }
            }
        }

        verify(exactly = 1) { authRouter.register(any()) }
        verify(exactly = 1) { userConfigurationRouter.register(any()) }
        verify(exactly = 1) { userRouter.register(any()) }
        verify(exactly = 1) { sessionRouter.register(any()) }
        verify(exactly = 1) { userSecurityRouter.register(any()) }
    }
}

