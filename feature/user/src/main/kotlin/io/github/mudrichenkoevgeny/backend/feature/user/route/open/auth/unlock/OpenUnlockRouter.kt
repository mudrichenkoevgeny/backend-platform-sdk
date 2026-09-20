package io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.unlock

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock.SendUnlockConfirmationToEmailUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock.SendUnlockConfirmationToPhoneUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock.UnlockByEmailUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock.UnlockByExternalAuthProviderUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock.UnlockByPhoneUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.otpconfirmation.toOtpConfirmationPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.otpconfirmation.OtpConfirmationPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiFields
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.unlock.UnlockByEmailConfirmationRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.unlock.UnlockByExternalAuthProviderRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.unlock.UnlockByPhoneConfirmationRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.confirmation.SendConfirmationToEmailRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.confirmation.SendConfirmationToPhoneRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.open.auth.unlock.OpenUnlockRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Public authentication HTTP router for self-service account unlocking.
 *
 * Registered routes:
 * 1. [OpenUnlockRoutes.SEND_UNLOCK_EMAIL_CONFIRMATION] — triggers delivery of an unlock verification code to email via [SendUnlockConfirmationToEmailUseCase].
 * 2. [OpenUnlockRoutes.UNLOCK_BY_EMAIL] — unlocks account using email confirmation code via [UnlockByEmailUseCase].
 * 3. [OpenUnlockRoutes.SEND_UNLOCK_PHONE_CONFIRMATION] — triggers delivery of an unlock verification code to phone via [SendUnlockConfirmationToPhoneUseCase].
 * 4. [OpenUnlockRoutes.UNLOCK_BY_PHONE] — unlocks account using phone confirmation code via [UnlockByPhoneUseCase].
 * 5. [OpenUnlockRoutes.UNLOCK_BY_EXTERNAL_PROVIDER] — unlocks account using external provider token via [UnlockByExternalAuthProviderUseCase].
 */
@Singleton
class OpenUnlockRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val sendUnlockConfirmationToEmailUseCase: SendUnlockConfirmationToEmailUseCase,
    private val unlockByEmailUseCase: UnlockByEmailUseCase,
    private val sendUnlockConfirmationToPhoneUseCase: SendUnlockConfirmationToPhoneUseCase,
    private val unlockByPhoneUseCase: UnlockByPhoneUseCase,
    private val unlockByExternalAuthProviderUseCase: UnlockByExternalAuthProviderUseCase
) : BaseRouter {

    override fun register(route: Route) {
        registerSendUnlockEmailConfirmationRoute(route)
        registerUnlockByEmailRoute(route)
        registerSendUnlockPhoneConfirmationRoute(route)
        registerUnlockByPhoneRoute(route)
        registerUnlockByExternalProviderRoute(route)
    }

    private fun registerSendUnlockEmailConfirmationRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY, UserAccountStatus.PENDING_DELETION)

        route.post(
            path = OpenUnlockRoutes.SEND_UNLOCK_EMAIL_CONFIRMATION,
            builder = { sendUnlockEmailConfirmationDocs(allowedRoles, allowedAccountStatuses) },
            body = { sendUnlockEmailConfirmation(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerUnlockByEmailRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY, UserAccountStatus.PENDING_DELETION)

        route.post(
            path = OpenUnlockRoutes.UNLOCK_BY_EMAIL,
            builder = { unlockByEmailDocs(allowedRoles, allowedAccountStatuses) },
            body = { unlockByEmail(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerSendUnlockPhoneConfirmationRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY, UserAccountStatus.PENDING_DELETION)

        route.post(
            path = OpenUnlockRoutes.SEND_UNLOCK_PHONE_CONFIRMATION,
            builder = { sendUnlockPhoneConfirmationDocs(allowedRoles, allowedAccountStatuses) },
            body = { sendUnlockPhoneConfirmation(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerUnlockByPhoneRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY, UserAccountStatus.PENDING_DELETION)

        route.post(
            path = OpenUnlockRoutes.UNLOCK_BY_PHONE,
            builder = { unlockByPhoneDocs(allowedRoles, allowedAccountStatuses) },
            body = { unlockByPhone(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerUnlockByExternalProviderRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY, UserAccountStatus.PENDING_DELETION)

        route.post(
            path = OpenUnlockRoutes.UNLOCK_BY_EXTERNAL_PROVIDER,
            builder = { unlockByExternalProviderDocs(allowedRoles, allowedAccountStatuses) },
            body = { unlockByExternalProvider(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.sendUnlockEmailConfirmationDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = SEND_UNLOCK_EMAIL_CONFIRMATION_SUMMARY
        operationId = SEND_UNLOCK_EMAIL_CONFIRMATION_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = SEND_UNLOCK_EMAIL_CONFIRMATION_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )
        request { body<SendConfirmationToEmailRequest>() }
        response {
            code(HttpStatusCode.OK) {
                body<OtpConfirmationPayload>()
                description = OK_RESPONSE_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.sendUnlockEmailConfirmation(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val request = call.validateRequest<SendConfirmationToEmailRequest>()

        val result = sendUnlockConfirmationToEmailUseCase(
            email = request.email,
            requestContext = call.getRequestContext(),
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        call.respondResult(result, appLogger, appErrorParser) { otpConfirmation ->
            otpConfirmation.toOtpConfirmationPayload()
        }
    }

    private fun RouteConfig.unlockByEmailDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = UNLOCK_BY_EMAIL_SUMMARY
        operationId = UNLOCK_BY_EMAIL_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = UNLOCK_BY_EMAIL_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )
        request { body<UnlockByEmailConfirmationRequest>() }
        response {
            code(HttpStatusCode.OK) {
                description = OK_RESPONSE_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.unlockByEmail(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val request = call.validateRequest<UnlockByEmailConfirmationRequest>()

        val result = unlockByEmailUseCase(
            email = request.email,
            confirmationCode = request.confirmationCode,
            requestContext = call.getRequestContext(),
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        call.respondResult(result, appLogger, appErrorParser, successStatus = HttpStatusCode.OK)
    }

    private fun RouteConfig.sendUnlockPhoneConfirmationDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = SEND_UNLOCK_PHONE_CONFIRMATION_SUMMARY
        operationId = SEND_UNLOCK_PHONE_CONFIRMATION_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = SEND_UNLOCK_PHONE_CONFIRMATION_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )
        request { body<SendConfirmationToPhoneRequest>() }
        response {
            code(HttpStatusCode.OK) {
                body<OtpConfirmationPayload>()
                description = OK_RESPONSE_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.sendUnlockPhoneConfirmation(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val request = call.validateRequest<SendConfirmationToPhoneRequest>()

        val result = sendUnlockConfirmationToPhoneUseCase(
            phoneNumber = request.phoneNumber,
            requestContext = call.getRequestContext(),
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        call.respondResult(result, appLogger, appErrorParser) { otpConfirmation ->
            otpConfirmation.toOtpConfirmationPayload()
        }
    }

    private fun RouteConfig.unlockByPhoneDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = UNLOCK_BY_PHONE_SUMMARY
        operationId = UNLOCK_BY_PHONE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = UNLOCK_BY_PHONE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )
        request { body<UnlockByPhoneConfirmationRequest>() }
        response {
            code(HttpStatusCode.OK) {
                description = OK_RESPONSE_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.unlockByPhone(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val request = call.validateRequest<UnlockByPhoneConfirmationRequest>()

        val result = unlockByPhoneUseCase(
            phoneNumber = request.phoneNumber,
            confirmationCode = request.confirmationCode,
            requestContext = call.getRequestContext(),
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        call.respondResult(result, appLogger, appErrorParser, successStatus = HttpStatusCode.OK)
    }

    private fun RouteConfig.unlockByExternalProviderDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = UNLOCK_BY_EXTERNAL_PROVIDER_SUMMARY
        operationId = UNLOCK_BY_EXTERNAL_PROVIDER_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + UserSwaggerTags.AUTH)
        description = getFormattedDescription(
            description = UNLOCK_BY_EXTERNAL_PROVIDER_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )
        request { body<UnlockByExternalAuthProviderRequest>() }
        response {
            code(HttpStatusCode.OK) {
                description = OK_RESPONSE_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.unlockByExternalProvider(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val request = call.validateRequest<UnlockByExternalAuthProviderRequest>()

        val authProvider = UserAuthProvider.fromValueOrNull(request.authProvider)
            ?: throw RequestHandlingException(
                CommonError.InvalidFieldValue(
                    fieldName = UserApiFields.AUTH_PROVIDER
                )
            )

        val result = unlockByExternalAuthProviderUseCase(
            authProvider = authProvider,
            token = request.externalProviderToken,
            requestContext = call.getRequestContext(),
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        call.respondResult(result, appLogger, appErrorParser, successStatus = HttpStatusCode.OK)
    }

    companion object {
        const val SEND_UNLOCK_EMAIL_CONFIRMATION_SUMMARY = "Send unlock confirmation code to email"
        const val SEND_UNLOCK_EMAIL_CONFIRMATION_DESCRIPTION = "Sends a verification code to the email to unlock a locked account."
        const val SEND_UNLOCK_EMAIL_CONFIRMATION_OPERATION_ID = "openSendUnlockConfirmationToEmail"

        const val UNLOCK_BY_EMAIL_SUMMARY = "Unlock account by email confirmation code"
        const val UNLOCK_BY_EMAIL_DESCRIPTION = "Unlocks a temporarily locked user account using email confirmation code."
        const val UNLOCK_BY_EMAIL_OPERATION_ID = "openUnlockByEmail"

        const val SEND_UNLOCK_PHONE_CONFIRMATION_SUMMARY = "Send unlock confirmation code to phone"
        const val SEND_UNLOCK_PHONE_CONFIRMATION_DESCRIPTION = "Sends a verification code to the phone to unlock a locked account."
        const val SEND_UNLOCK_PHONE_CONFIRMATION_OPERATION_ID = "openSendUnlockConfirmationToPhone"

        const val UNLOCK_BY_PHONE_SUMMARY = "Unlock account by phone confirmation code"
        const val UNLOCK_BY_PHONE_DESCRIPTION = "Unlocks a temporarily locked user account using phone confirmation code."
        const val UNLOCK_BY_PHONE_OPERATION_ID = "openUnlockByPhone"

        const val UNLOCK_BY_EXTERNAL_PROVIDER_SUMMARY = "Unlock account by external auth provider"
        const val UNLOCK_BY_EXTERNAL_PROVIDER_DESCRIPTION = "Unlocks a temporarily locked user account using external provider token."
        const val UNLOCK_BY_EXTERNAL_PROVIDER_OPERATION_ID = "openUnlockByExternalProvider"

        const val OK_RESPONSE_DESCRIPTION = "Success."
    }
}
