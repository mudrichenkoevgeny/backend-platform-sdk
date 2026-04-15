package io.github.mudrichenkoevgeny.backend.feature.user.route.security

import io.github.mudrichenkoevgeny.backend.feature.user.route.open.identifier.PasswordRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.open.identifier.OpenIdentifierRouter
import io.ktor.server.routing.Route
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class UserSecurityRouterTest {

    @Test
    fun `register delegates to password and user-identifiers security routers`() {
        val passwordRouter = mockk<PasswordRouter>(relaxed = true)
        val userOpenIdentifierRouter = mockk<OpenIdentifierRouter>(relaxed = true)
        val route = mockk<Route>(relaxed = true)

        val router = UserSecurityRouter(
            passwordRouter = passwordRouter,
            identifiersRouter = userOpenIdentifierRouter
        )

        router.register(route)

        verify(exactly = 1) { passwordRouter.register(route) }
        verify(exactly = 1) { userOpenIdentifierRouter.register(route) }
    }
}

