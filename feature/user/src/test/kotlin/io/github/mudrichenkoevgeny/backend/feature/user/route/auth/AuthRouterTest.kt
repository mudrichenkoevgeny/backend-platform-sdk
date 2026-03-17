package io.github.mudrichenkoevgeny.backend.feature.user.route.auth

import io.github.mudrichenkoevgeny.backend.feature.user.route.auth.login.LoginRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.auth.refreshtoken.RefreshTokenRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.auth.register.RegisterRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.auth.resetpassword.ResetPasswordRouter
import io.github.mudrichenkoevgeny.backend.feature.user.route.auth.settings.AuthSettingsRouter
import io.ktor.server.routing.Route
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class AuthRouterTest {

    @Test
    fun `register delegates to all auth subrouters`() {
        val refreshTokenRouter = mockk<RefreshTokenRouter>(relaxed = true)
        val loginRouter = mockk<LoginRouter>(relaxed = true)
        val registerRouter = mockk<RegisterRouter>(relaxed = true)
        val resetPasswordRouter = mockk<ResetPasswordRouter>(relaxed = true)
        val authSettingsRouter = mockk<AuthSettingsRouter>(relaxed = true)
        val route = mockk<Route>(relaxed = true)

        val router = AuthRouter(
            refreshTokenRouter = refreshTokenRouter,
            loginRouter = loginRouter,
            registerRouter = registerRouter,
            resetPasswordRouter = resetPasswordRouter,
            authSettingsRouter = authSettingsRouter
        )

        router.register(route)

        verify(exactly = 1) { refreshTokenRouter.register(route) }
        verify(exactly = 1) { loginRouter.register(route) }
        verify(exactly = 1) { registerRouter.register(route) }
        verify(exactly = 1) { resetPasswordRouter.register(route) }
        verify(exactly = 1) { authSettingsRouter.register(route) }
    }
}

