package io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.unlock

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.network.application.setupOpenTestEnvironment
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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.open.auth.unlock.OpenUnlockRoutes
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

class OpenUnlockRouterTest : BaseRouterTest() {

    private val sendUnlockConfirmationToEmailUseCase = mockk<SendUnlockConfirmationToEmailUseCase>()
    private val unlockByEmailUseCase = mockk<UnlockByEmailUseCase>()
    private val sendUnlockConfirmationToPhoneUseCase = mockk<SendUnlockConfirmationToPhoneUseCase>()
    private val unlockByPhoneUseCase = mockk<UnlockByPhoneUseCase>()
    private val unlockByExternalAuthProviderUseCase = mockk<UnlockByExternalAuthProviderUseCase>()

    private val router = OpenUnlockRouter(
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
    fun `send unlock email confirmation - success`() = testApplication {
        setupOpenTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val otpConfirmation = mockk<OtpConfirmation>(relaxed = true)
        coEvery {
            sendUnlockConfirmationToEmailUseCase(any(), any<RequestContext>())
        } returns AppResult.Success(otpConfirmation)

        val response = jsonClient.post(OpenUnlockRoutes.SEND_UNLOCK_EMAIL_CONFIRMATION) {
            contentType(ContentType.Application.Json)
            setBody(SendConfirmationToEmailRequest(email = "test@example.com"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `unlock by email - success`() = testApplication {
        setupOpenTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        coEvery {
            unlockByEmailUseCase(any(), any(), any<RequestContext>())
        } returns AppResult.Success(Unit)

        val response = jsonClient.post(OpenUnlockRoutes.UNLOCK_BY_EMAIL) {
            contentType(ContentType.Application.Json)
            setBody(UnlockByEmailConfirmationRequest(email = "test@example.com", confirmationCode = "123456"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `send unlock phone confirmation - success`() = testApplication {
        setupOpenTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val otpConfirmation = mockk<OtpConfirmation>(relaxed = true)
        coEvery {
            sendUnlockConfirmationToPhoneUseCase(any(), any<RequestContext>())
        } returns AppResult.Success(otpConfirmation)

        val response = jsonClient.post(OpenUnlockRoutes.SEND_UNLOCK_PHONE_CONFIRMATION) {
            contentType(ContentType.Application.Json)
            setBody(SendConfirmationToPhoneRequest(phoneNumber = "+1234567890"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `unlock by phone - success`() = testApplication {
        setupOpenTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        coEvery {
            unlockByPhoneUseCase(any(), any(), any<RequestContext>())
        } returns AppResult.Success(Unit)

        val response = jsonClient.post(OpenUnlockRoutes.UNLOCK_BY_PHONE) {
            contentType(ContentType.Application.Json)
            setBody(UnlockByPhoneConfirmationRequest(phoneNumber = "+1234567890", confirmationCode = "123456"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `unlock by external provider - success`() = testApplication {
        setupOpenTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        coEvery {
            unlockByExternalAuthProviderUseCase(any(), any(), any<RequestContext>())
        } returns AppResult.Success(Unit)

        val response = jsonClient.post(OpenUnlockRoutes.UNLOCK_BY_EXTERNAL_PROVIDER) {
            contentType(ContentType.Application.Json)
            setBody(UnlockByExternalAuthProviderRequest(authProvider = UserAuthProvider.GOOGLE.serialName, externalProviderToken = "ext_token"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
