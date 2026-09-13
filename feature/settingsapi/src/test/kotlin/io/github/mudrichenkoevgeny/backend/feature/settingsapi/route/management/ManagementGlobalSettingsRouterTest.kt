package io.github.mudrichenkoevgeny.backend.feature.settingsapi.route.management

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.config.model.createTestManagementGlobalSettings
import io.github.mudrichenkoevgeny.backend.feature.settingsapi.usecase.management.globalsettings.GetManagementGlobalSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.settingsapi.usecase.management.globalsettings.UpdateGlobalSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.createTestUserDetails
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.network.application.setupManagementTestEnvironment
import io.github.mudrichenkoevgeny.backend.feature.user.network.route.BaseRouterTest
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.mapper.globalsettings.toManagementGlobalSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.settingsapi.network.route.management.globalsettings.ManagementGlobalSettingsRoutes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ManagementGlobalSettingsRouterTest : BaseRouterTest() {

    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val updateGlobalSettingsUseCase = mockk<UpdateGlobalSettingsUseCase>()
    private val getManagementGlobalSettingsUseCase = mockk<GetManagementGlobalSettingsUseCase>()

    private val router = ManagementGlobalSettingsRouter(
        authenticationProvider = authProvider,
        appLogger = appLogger,
        appErrorParser = appErrorParser,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        updateGlobalSettingsUseCase = updateGlobalSettingsUseCase,
        getManagementGlobalSettingsUseCase = getManagementGlobalSettingsUseCase
    )

    @BeforeEach
    fun setUp() {
        clearMocks(updateGlobalSettingsUseCase, getManagementGlobalSettingsUseCase, auditErrorConverter)
    }

    private fun sampleSettings() = createTestManagementGlobalSettings(
        privacyPolicyUrl = "https://example.com/privacy",
        termsOfServiceUrl = "https://example.com/terms",
        contactSupportEmail = "support@example.com"
    )

    @Test
    fun `get management global settings - success`() = testApplication {
        val token = setupManagementTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        every { getManagementGlobalSettingsUseCase() } returns AppResult.Success(sampleSettings())

        val response = jsonClient.get(ManagementGlobalSettingsRoutes.GET_MANAGEMENT_GLOBAL_SETTINGS) {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `update global settings - success when admin`() = testApplication {
        val token = setupManagementTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        authProvider.shouldReturnSuccess(createTestUserDetails(role = UserRole.ADMIN))
        coEvery { updateGlobalSettingsUseCase(any(), any()) } returns AppResult.Success(Unit)

        val response = jsonClient.put(ManagementGlobalSettingsRoutes.UPDATE_MANAGEMENT_GLOBAL_SETTINGS) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(sampleSettings().toManagementGlobalSettingsPayload())
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `update global settings - forbidden when user role`() = testApplication {
        val token = setupManagementTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }
        val error = UserError.UserForbidden()

        authProvider.shouldReturnError(AppResult.Error(error))
        every { auditErrorConverter.convert(error) } returns AuditErrorLogData(AuditStatus.DENIED, emptySet())

        val response = jsonClient.put(ManagementGlobalSettingsRoutes.UPDATE_MANAGEMENT_GLOBAL_SETTINGS) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(sampleSettings().toManagementGlobalSettingsPayload())
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `update global settings - unauthorized when invalid token`() = testApplication {
        val token = setupManagementTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val error = UserError.InvalidAccessToken()
        authProvider.shouldReturnError(AppResult.Error(error))
        every { auditErrorConverter.convert(error) } returns AuditErrorLogData(AuditStatus.DENIED, emptySet())

        val response = jsonClient.put(ManagementGlobalSettingsRoutes.UPDATE_MANAGEMENT_GLOBAL_SETTINGS) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(sampleSettings().toManagementGlobalSettingsPayload())
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}