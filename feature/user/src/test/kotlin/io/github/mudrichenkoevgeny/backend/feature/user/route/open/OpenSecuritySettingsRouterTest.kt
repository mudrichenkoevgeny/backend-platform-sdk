package io.github.mudrichenkoevgeny.backend.feature.user.route.open

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.domain.model.securitysettings.createTestOpenSecuritySettings
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.settings.GetOpenSecuritySettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.network.application.setupOpenTestEnvironment
import io.github.mudrichenkoevgeny.backend.feature.user.network.route.BaseRouterTest
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.route.open.security.settings.OpenSecuritySettingsRoutes
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpenSecuritySettingsRouterTest : BaseRouterTest() {

    private val getOpenSecuritySettingsUseCase = mockk<GetOpenSecuritySettingsUseCase>()

    private val router = OpenSecuritySettingsRouter(
        appLogger = appLogger,
        appErrorParser = appErrorParser,
        getOpenSecuritySettingsUseCase = getOpenSecuritySettingsUseCase
    )

    @BeforeEach
    fun setUp() {
        clearMocks(getOpenSecuritySettingsUseCase)
    }

    @Test
    fun `get security settings - success`() = testApplication {
        setupOpenTestEnvironment(router)
        val settings = createTestOpenSecuritySettings()
        coEvery { getOpenSecuritySettingsUseCase() } returns AppResult.Success(settings)

        val response = client.get(OpenSecuritySettingsRoutes.GET_OPEN_SECURITY_SETTINGS)

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
