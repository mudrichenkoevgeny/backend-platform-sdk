package io.github.mudrichenkoevgeny.backend.feature.settings.api.route.open

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.settings.api.usecase.open.globalsettings.GetGlobalSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.network.application.setupOpenTestEnvironment
import io.github.mudrichenkoevgeny.backend.feature.user.network.route.BaseRouterTest
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.GlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.settings.api.network.route.open.globalsettings.OpenGlobalSettingsRoutes
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpenGlobalSettingsRouterTest : BaseRouterTest() {

    private val getGlobalSettingsUseCase = mockk<GetGlobalSettingsUseCase>()

    private val router = OpenGlobalSettingsRouter(
        appLogger = appLogger,
        appErrorParser = appErrorParser,
        getGlobalSettingsUseCase = getGlobalSettingsUseCase
    )

    @BeforeEach
    fun setUp() {
        clearMocks(getGlobalSettingsUseCase)
    }

    private fun sampleSettings() = GlobalSettings(
        privacyPolicyUrl = "https://example.com/privacy",
        termsOfServiceUrl = "https://example.com/terms",
        contactSupportEmail = "support@example.com"
    )

    @Test
    fun `get global settings - success`() = testApplication {
        setupOpenTestEnvironment(router)
        val settings = sampleSettings()
        coEvery { getGlobalSettingsUseCase() } returns AppResult.Success(settings)

        val response = client.get(OpenGlobalSettingsRoutes.GET_GLOBAL_SETTINGS)

        assertEquals(HttpStatusCode.OK, response.status)
    }
}