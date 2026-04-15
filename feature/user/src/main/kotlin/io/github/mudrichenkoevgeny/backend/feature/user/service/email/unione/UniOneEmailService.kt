package io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.bodySafe
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.naming.EmailTemplateArgs
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.naming.EmailTemplateKeys
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.EmailParser
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model.UniOneBody
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model.UniOneConfig
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model.UniOneEmailRequest
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model.UniOneErrorResponse
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model.UniOneMessage
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model.UniOneRecipient
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * [EmailService] implementation backed by the UniOne API.
 *
 * Templates are resolved via [EmailParser], rendered with arguments and sent as HTML emails.
 *
 * This service also exposes [fakeSendEmail] which simulates network latency without sending real emails.
 */
@Singleton
class UniOneEmailService @Inject constructor(
    private val config: UniOneConfig,
    private val httpClient: HttpClient,
    private val emailParser: EmailParser
) : EmailService {

    private val lastExecutionTime = AtomicLong(500L)
    private val defaultLanguage = "en"

    override suspend fun sendVerificationCode(email: String, code: String, language: String?): AppResult<Unit> {
        return executeTemplatedSend(
            email = email,
            templateKey = EmailTemplateKeys.VERIFICATION_CODE,
            args = mapOf(EmailTemplateArgs.CODE to code),
            language = language
        )
    }

    override suspend fun sendResetPasswordVerificationCode(email: String, code: String, language: String?): AppResult<Unit> {
        return executeTemplatedSend(
            email = email,
            templateKey = EmailTemplateKeys.RESET_PASSWORD_CODE,
            args = mapOf(EmailTemplateArgs.CODE to code),
            language = language
        )
    }

    override suspend fun sendAlreadyRegisteredEmail(
        email: String,
        ipAddress: String?,
        deviceName: String?,
        language: String?
    ): AppResult<Unit> {
        return executeTemplatedSend(
            email = email,
            templateKey = EmailTemplateKeys.ALREADY_REGISTERED,
            args = mapOf(
                EmailTemplateArgs.IP_ADDRESS to (ipAddress ?: "unknown"),
                EmailTemplateArgs.DEVICE_NAME to (deviceName ?: "unknown")
            ),
            language = language
        )
    }

    override suspend fun sendSuccessfulRegistrationEmail(email: String): AppResult<Unit> {
        return executeTemplatedSend(
            email = email,
            templateKey = EmailTemplateKeys.SUCCESSFUL_REGISTRATION,
            args = emptyMap(),
            language = defaultLanguage
        )
    }

    override suspend fun sendSuccessfulLoginEmail(email: String, ipAddress: String?, deviceName: String?): AppResult<Unit> {
        return executeTemplatedSend(
            email = email,
            templateKey = EmailTemplateKeys.SUCCESSFUL_LOGIN,
            args = mapOf(
                EmailTemplateArgs.IP_ADDRESS to (ipAddress ?: "unknown"),
                EmailTemplateArgs.DEVICE_NAME to (deviceName ?: "unknown")
            ),
            language = defaultLanguage
        )
    }

    override suspend fun sendPasswordSuccessfullyChangedEmail(
        email: String,
        ipAddress: String?,
        deviceName: String?
    ): AppResult<Unit> {
        return executeTemplatedSend(
            email = email,
            templateKey = EmailTemplateKeys.PASSWORD_CHANGED,
            args = mapOf(
                EmailTemplateArgs.IP_ADDRESS to (ipAddress ?: "unknown"),
                EmailTemplateArgs.DEVICE_NAME to (deviceName ?: "unknown")
            ),
            language = defaultLanguage
        )
    }

    override suspend fun fakeSendEmail(): AppResult<Unit> {
        val baseDelay = lastExecutionTime.get()
        val jitter = Random.nextLong(-50, 50)

        delay((baseDelay + jitter).coerceAtLeast(100L))

        return AppResult.Success(Unit)
    }

    private suspend fun executeTemplatedSend(
        email: String,
        templateKey: String,
        args: Map<String, Any>,
        language: String?
    ): AppResult<Unit> {
        val locale = language ?: defaultLanguage
        val template = emailParser.getTemplate(templateKey, args, locale)
            ?: return AppResult.Error(CommonError.ServiceUnavailable("Email template not found: $templateKey"))

        return executeSend(email, template.subject, template.body)
    }

    private suspend fun executeSend(email: String, subject: String, htmlContent: String): AppResult<Unit> {
        val cleanUrl = config.url.trim().removeSurrounding("\"").removeSuffix("/")
        val cleanEndpoint = config.apiSend.trim().removeSurrounding("\"").removePrefix("/")
        val fullUrl = "$cleanUrl/$cleanEndpoint"

        val requestBody = UniOneEmailRequest(
            apiKey = config.apiKey.trim().removeSurrounding("\""),
            message = UniOneMessage(
                recipients = listOf(UniOneRecipient(email)),
                subject = subject,
                fromEmail = config.fromEmail.trim().removeSurrounding("\""),
                fromName = config.fromName.trim().removeSurrounding("\""),
                body = UniOneBody(htmlContent),
                trackDomain = config.trackDomain.trim().removeSurrounding("\"")
            )
        )

        return try {
            val response = httpClient.post(fullUrl) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                AppResult.Success(Unit)
            } else {
                val errorBody = response.bodySafe<UniOneErrorResponse>()
                val errorMessage = errorBody?.let { "Code ${it.code}: ${it.message}" } ?: "Unknown UniOne error"
                AppResult.Error(CommonError.ServiceUnavailable(errorMessage))
            }
        } catch (e: Exception) {
            AppResult.Error(CommonError.Internal(e))
        }
    }
}
