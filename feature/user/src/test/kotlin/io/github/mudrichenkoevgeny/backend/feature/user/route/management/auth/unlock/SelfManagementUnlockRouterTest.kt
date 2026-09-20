package io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.unlock

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.network.application.setupManagementTestEnvironment
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.route.BaseRouterTest
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock.SendUnlockConfirmationToEmailUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock.SendUnlockConfirmationToPhoneUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock.UnlockByEmailUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock.UnlockByExternalAuthProviderUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock.UnlockByPhoneUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.unlock.UnlockByEmailConfirmationRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.unlock.UnlockByExternalAuthProviderRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.unlock.UnlockByPhoneConfirmationRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.confirmation.SendConfirmationToEmailRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.confirmation.SendConfirmationToPhoneRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.management.auth.unlock.SelfManagementUnlockRoutes
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SelfManagementUnlockRouterTest : BaseRouterTest() {

    private val sendUnlockConfirmationToEmailUseCase = mockk<SendUnlockConfirmationToEmailUseCase>()
    private val unlockByEmailUseCase = mockk<UnlockByEmailUseCase>()
    private val sendUnlockConfirmationToPhoneUseCase = mockk<SendUnlockConfirmationToPhoneUseCase>()
    private val unlockByPhoneUseCase = mockk<UnlockByPhoneUseCase>()
    private val unlockByExternalAuthProviderUseCase = mockk<UnlockByExternalAuthProviderUseCase>()

    private val router = SelfManagementUnlockRouter(
        appLogger = appLogger,
        appErrorParser = appErrorParser,
        sendUnlockConfirmationToEmailUseCase = sendUnlockConfirmationToEmailUseCase,
        unlockByEmailUseCase = unlockByEmailUseCase,
        sendUnlockConfirmationToPhoneUseCase = sendUnlockConfirmationToPhoneUseCase,
        unlockByPhoneUseCase = unlockByPhoneUseCase,
        unlockByExternalAuthProviderUseCase = unlockByExternalAuthProviderUseCase
    )

    @BeforeEach
    fun setUp() {
        clearMocks(
            sendUnlockConfirmationToEmailUseCase,
            unlockByEmailUseCase,
            sendUnlockConfirmationToPhoneUseCase,
            unlockByPhoneUseCase,
            unlockByExternalAuthProviderUseCase
        )
    }

    @Test
    fun `send management unlock email confirmation - success`() = testApplication {
        setupManagementTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val otpConfirmation = mockk<OtpConfirmation>(relaxed = true)
        coEvery {
            sendUnlockConfirmationToEmailUseCase(any(), any<RequestContext>(), any(), any())
        } returns AppResult.Success(otpConfirmation)

        val response = jsonClient.post(SelfManagementUnlockRoutes.SEND_UNLOCK_EMAIL_CONFIRMATION) {
            contentType(ContentType.Application.Json)
            setBody(SendConfirmationToEmailRequest(email = "admin@example.com"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `unlock management by email - success`() = testApplication {
        setupManagementTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        coEvery {
            unlockByEmailUseCase(any(), any(), any<RequestContext>(), any(), any())
        } returns AppResult.Success(Unit)

        val response = jsonClient.post(SelfManagementUnlockRoutes.UNLOCK_BY_EMAIL) {
            contentType(ContentType.Application.Json)
            setBody(UnlockByEmailConfirmationRequest(email = "admin@example.com", confirmationCode = "123456"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `send management unlock phone confirmation - success`() = testApplication {
        setupManagementTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val otpConfirmation = mockk<OtpConfirmation>(relaxed = true)
        coEvery {
            sendUnlockConfirmationToPhoneUseCase(any(), any<RequestContext>(), any(), any())
        } returns AppResult.Success(otpConfirmation)

        val response = jsonClient.post(SelfManagementUnlockRoutes.SEND_UNLOCK_PHONE_CONFIRMATION) {
            contentType(ContentType.Application.Json)
            setBody(SendConfirmationToPhoneRequest(phoneNumber = "+1234567890"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `unlock management by phone - success`() = testApplication {
        setupManagementTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        coEvery {
            unlockByPhoneUseCase(any(), any(), any<RequestContext>(), any(), any())
        } returns AppResult.Success(Unit)

        val response = jsonClient.post(SelfManagementUnlockRoutes.UNLOCK_BY_PHONE) {
            contentType(ContentType.Application.Json)
            setBody(UnlockByPhoneConfirmationRequest(phoneNumber = "+1234567890", confirmationCode = "123456"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `unlock management by external provider - success`() = testApplication {
        setupManagementTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        coEvery {
            unlockByExternalAuthProviderUseCase(any(), any(), any<RequestContext>(), any(), any())
        } returns AppResult.Success(Unit)

        val response = jsonClient.post(SelfManagementUnlockRoutes.UNLOCK_BY_EXTERNAL_PROVIDER) {
            contentType(ContentType.Application.Json)
            setBody(UnlockByExternalAuthProviderRequest(authProvider = UserAuthProvider.GOOGLE.serialName, externalProviderToken = "ext_token"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
