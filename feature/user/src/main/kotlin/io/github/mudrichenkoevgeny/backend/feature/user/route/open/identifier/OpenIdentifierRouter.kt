package io.github.mudrichenkoevgeny.backend.feature.user.route.open.identifier

import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validatePathParameter
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.feature.user.mapper.confirmation.toSendConfirmationResponse
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.AddUserIdentifierEmailUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.AddUserIdentifierExternalAuthProviderUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.AddUserIdentifierPhoneUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.DeleteUserIdentifierUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.GetUserIdentifiersUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.IdentifierEmailChangePasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.SendAddEmailIdentifierConfirmationUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.SendAddPhoneIdentifierConfirmationUseCase
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.toUserIdentifierIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.identifier.toUserIdentifierPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiPaths
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.confirmation.SendConfirmationToEmailRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.confirmation.SendConfirmationToPhoneRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.security.password.PasswordChangeRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.security.useridentifiers.AddUserIdentifierEmailRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.security.useridentifiers.AddUserIdentifierExternalAuthProviderRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.security.useridentifiers.AddUserIdentifierPhoneRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.open.identifier.OpenIdentifierRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User identifier (identity) management routes for authenticated users.
 *
 * Allows listing linked identifiers and managing additional identifiers such as:
 * - Email
 * - Phone number
 * - External auth provider accounts
 *
 * Also provides endpoints for sending confirmation codes required to link identifiers.
 */
@Singleton
class OpenIdentifierRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val getUserIdentifiersUseCase: GetUserIdentifiersUseCase,
    private val deleteUserIdentifierUseCase: DeleteUserIdentifierUseCase,
    private val addUserIdentifierEmailUseCase: AddUserIdentifierEmailUseCase,
    private val addUserIdentifierPhoneUseCase: AddUserIdentifierPhoneUseCase,
    private val addUserIdentifierExternalAuthProviderUseCase: AddUserIdentifierExternalAuthProviderUseCase,
    private val sendAddEmailIdentifierConfirmationUseCase: SendAddEmailIdentifierConfirmationUseCase,
    private val sendAddPhoneIdentifierConfirmationUseCase: SendAddPhoneIdentifierConfirmationUseCase,
    private val identifierEmailChangePasswordUseCase: IdentifierEmailChangePasswordUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.get(
            path = OpenIdentifierRoutes.GET_IDENTIFIERS,
            builder = { getIdentifiersDocs() },
            body = { getIdentifiers() }
        )

        route.delete(
            path = OpenIdentifierRoutes.DELETE_IDENTIFIER,
            builder = { deleteIdentifierDocs() },
            body = { deleteIdentifier() }
        )

        route.post(
            path = OpenIdentifierRoutes.ADD_IDENTIFIER_EMAIL,
            builder = { addIdentifierEmailDocs() },
            body = { addIdentifierEmail() }
        )

        route.post(
            path = OpenIdentifierRoutes.ADD_IDENTIFIER_PHONE,
            builder = { addIdentifierPhoneDocs() },
            body = { addIdentifierPhone() }
        )

        route.post(
            path = OpenIdentifierRoutes.ADD_IDENTIFIER_EXTERNAL_AUTH_PROVIDER,
            builder = { addIdentifierExternalAuthProviderDocs() },
            body = { addIdentifierExternalAuthProvider() }
        )

        route.post(
            path = OpenIdentifierRoutes.SEND_ADD_EMAIL_IDENTIFIER_CONFIRMATION,
            builder = { sendAddEmailIdentifierConfirmationDocs() },
            body = { sendAddEmailIdentifierConfirmation() }
        )

        route.post(
            path = OpenIdentifierRoutes.SEND_ADD_PHONE_IDENTIFIER_CONFIRMATION,
            builder = { sendAddPhoneIdentifierConfirmationDocs() },
            body = { sendAddPhoneIdentifierConfirmation() }
        )

        route.post(
            path = OpenIdentifierRoutes.IDENTIFIER_EMAIL_CHANGE_PASSWORD,
            builder = { identifierEmailChangePasswordDocs() },
            body = { identifierEmailChangePassword() }
        )
    }

    private fun RouteConfig.getIdentifiersDocs() {
        summary = GET_USER_IDENTIFIERS_ROUTE_SUMMARY
        description = GET_USER_IDENTIFIERS_ROUTE_DESCRIPTION
        operationId = GET_USER_IDENTIFIERS_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        response {
            code(HttpStatusCode.OK) {
                description = GET_USER_IDENTIFIERS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getIdentifiers() {
        val result = getUserIdentifiersUseCase.execute(
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) { userIdentifiers ->
            userIdentifiers.map { userIdentifier ->
                userIdentifier.toUserIdentifierResponse()
            }
        }
    }

    private fun RouteConfig.deleteIdentifierDocs() {
        summary = DELETE_USER_IDENTIFIER_ROUTE_SUMMARY
        description = DELETE_USER_IDENTIFIER_ROUTE_DESCRIPTION
        operationId = DELETE_USER_IDENTIFIER_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        request {
            pathParameter<String>(UserApiPaths.USER_IDENTIFIER_ID) {
                description = DELETE_USER_IDENTIFIER_ROUTE_PATH_PARAMETER_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = DELETE_USER_IDENTIFIER_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.deleteIdentifier() {
        val userIdentifierId = call.validatePathParameter(UserApiPaths.USER_IDENTIFIER_ID) { id ->
            id.toUserIdentifierIdOrThrow()
        }

        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val result = deleteUserIdentifierUseCase(
            userIdentifierId = userIdentifierId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun RouteConfig.addIdentifierEmailDocs() {
        summary = ADD_EMAIL_SUMMARY
        description = ADD_EMAIL_DESCRIPTION
        operationId = ADD_EMAIL_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        request {
            body<AddUserIdentifierEmailRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = ADD_IDENTIFIER_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.addIdentifierEmail() {
        val request = call.validateRequest<AddUserIdentifierEmailRequest>()

        val result = addUserIdentifierEmailUseCase.execute(
            email = request.email,
            password = request.password,
            confirmationCode = request.confirmationCode,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) { userIdentifier ->
            userIdentifier.toUserIdentifierResponse()
        }
    }

    private fun RouteConfig.addIdentifierPhoneDocs() {
        summary = ADD_PHONE_SUMMARY
        description = ADD_PHONE_DESCRIPTION
        operationId = ADD_PHONE_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        request {
            body<AddUserIdentifierPhoneRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = ADD_IDENTIFIER_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.addIdentifierPhone() {
        val request = call.validateRequest<AddUserIdentifierPhoneRequest>()

        val result = addUserIdentifierPhoneUseCase.execute(
            phoneNumber = request.phoneNumber,
            confirmationCode = request.confirmationCode,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) { userIdentifier ->
            userIdentifier.toUserIdentifierResponse()
        }
    }

    private fun RouteConfig.addIdentifierExternalAuthProviderDocs() {
        summary = ADD_EXTERNAL_SUMMARY
        description = ADD_EXTERNAL_DESCRIPTION
        operationId = ADD_EXTERNAL_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        request {
            body<AddUserIdentifierExternalAuthProviderRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = ADD_IDENTIFIER_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.addIdentifierExternalAuthProvider() {
        val request = call.validateRequest<AddUserIdentifierExternalAuthProviderRequest>()

        val result = addUserIdentifierExternalAuthProviderUseCase.execute(
            authProviderKey = request.authProvider,
            token = request.token,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) { userIdentifier ->
            userIdentifier.toUserIdentifierResponse()
        }
    }

    private fun RouteConfig.sendAddEmailIdentifierConfirmationDocs() {
        summary = SEND_ADD_EMAIL_CONFIRMATION_SUMMARY
        description = SEND_ADD_EMAIL_CONFIRMATION_DESCRIPTION
        operationId = SEND_ADD_EMAIL_CONFIRMATION_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        request { body<SendConfirmationToEmailRequest>() }
        response {
            code(HttpStatusCode.OK) {
                description = CONFIRMATION_SENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.sendAddEmailIdentifierConfirmation() {
        val request = call.validateRequest<SendConfirmationToEmailRequest>()
        val result = sendAddEmailIdentifierConfirmationUseCase.execute(
            email = request.email,
            requestContext = call.getRequestContext()
        )
        call.respondResult(result, appLogger, appErrorParser) { sendConfirmation ->
            sendConfirmation.toSendConfirmationResponse()
        }
    }

    private fun RouteConfig.sendAddPhoneIdentifierConfirmationDocs() {
        summary = SEND_ADD_PHONE_CONFIRMATION_SUMMARY
        description = SEND_ADD_PHONE_CONFIRMATION_DESCRIPTION
        operationId = SEND_ADD_PHONE_CONFIRMATION_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        request { body<SendConfirmationToPhoneRequest>() }
        response {
            code(HttpStatusCode.OK) {
                description = CONFIRMATION_SENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.sendAddPhoneIdentifierConfirmation() {
        val request = call.validateRequest<SendConfirmationToPhoneRequest>()
        val result = sendAddPhoneIdentifierConfirmationUseCase.execute(
            phoneNumber = request.phoneNumber,
            requestContext = call.getRequestContext()
        )
        call.respondResult(result, appLogger, appErrorParser) { sendConfirmation ->
            sendConfirmation.toSendConfirmationResponse()
        }
    }

    private fun RouteConfig.identifierEmailChangePasswordDocs() {
        summary = IDENTIFIER_EMAIL_CHANGE_PASSWORD_SUMMARY
        description = IDENTIFIER_EMAIL_CHANGE_PASSWORD_DESCRIPTION
        operationId = IDENTIFIER_EMAIL_CHANGE_PASSWORD_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        request {
            body<PasswordChangeRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = IDENTIFIER_EMAIL_CHANGE_PASSWORD_RESPONSE_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.identifierEmailChangePassword() {
        val request = call.validateRequest<PasswordChangeRequest>()

        val result = identifierEmailChangePasswordUseCase.execute(
            email = request.email,
            oldPassword = request.oldPassword,
            newPassword = request.newPassword,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) { userIdentifier ->
            userIdentifier.toUserIdentifierPayload()
        }
    }

    companion object {
        const val GET_USER_IDENTIFIERS_ROUTE_SUMMARY = "Get user identifiers"
        const val GET_USER_IDENTIFIERS_ROUTE_DESCRIPTION = "Returns all authentication identifiers of the current user."
        const val GET_USER_IDENTIFIERS_ROUTE_OPERATION_ID = "getUserIdentifiers"
        const val GET_USER_IDENTIFIERS_ROUTE_RESPONSE_OK_DESCRIPTION = "Success"

        const val DELETE_USER_IDENTIFIER_ROUTE_SUMMARY = "Delete user identifier"
        const val DELETE_USER_IDENTIFIER_ROUTE_DESCRIPTION = "Deletes a user authentication identifier."
        const val DELETE_USER_IDENTIFIER_ROUTE_OPERATION_ID = "deleteUserIdentifier"
        const val DELETE_USER_IDENTIFIER_ROUTE_PATH_PARAMETER_ID_DESCRIPTION = "ID of the user identifier to delete"
        const val DELETE_USER_IDENTIFIER_ROUTE_RESPONSE_OK_DESCRIPTION = "Deleted"

        const val ADD_EMAIL_SUMMARY = "Add email user identifier"
        const val ADD_EMAIL_DESCRIPTION = "Adds email as a new authentication identifier."
        const val ADD_EMAIL_OPERATION_ID = "addUserIdentifierEmail"

        const val ADD_PHONE_SUMMARY = "Add phone user identifier"
        const val ADD_PHONE_DESCRIPTION = "Adds phone number as a new authentication identifier."
        const val ADD_PHONE_OPERATION_ID = "addUserIdentifierPhone"

        const val ADD_EXTERNAL_SUMMARY = "Add external auth provider identifier"
        const val ADD_EXTERNAL_DESCRIPTION = "Adds external authentication provider as a new authentication identifier."
        const val ADD_EXTERNAL_OPERATION_ID = "addUserIdentifierExternalAuthProvider"

        const val ADD_IDENTIFIER_RESPONSE_OK_DESCRIPTION = "Success. User identifier added."

        const val SEND_ADD_EMAIL_CONFIRMATION_SUMMARY = "Send email confirmation (identity add)"
        const val SEND_ADD_EMAIL_CONFIRMATION_DESCRIPTION = "Sends a code to a new email to link it to the current account."
        const val SEND_ADD_EMAIL_CONFIRMATION_OPERATION_ID = "sendAddEmailIdentifierConfirmation"

        const val SEND_ADD_PHONE_CONFIRMATION_SUMMARY = "Send phone confirmation (identity add)"
        const val SEND_ADD_PHONE_CONFIRMATION_DESCRIPTION = "Sends a code to a new phone number to link it to the current account."
        const val SEND_ADD_PHONE_CONFIRMATION_OPERATION_ID = "sendAddPhoneIdentifierConfirmation"

        const val CONFIRMATION_SENT_DESCRIPTION = "Success. Confirmation code sent."

        const val IDENTIFIER_EMAIL_CHANGE_PASSWORD_SUMMARY = "Change email password"
        const val IDENTIFIER_EMAIL_CHANGE_PASSWORD_DESCRIPTION = "Changes the current user's email password."
        const val IDENTIFIER_EMAIL_CHANGE_PASSWORD_OPERATION_ID = "identifierEmailChangePassword"
        const val IDENTIFIER_EMAIL_CHANGE_PASSWORD_RESPONSE_DESCRIPTION = "Email password changed successfully."
    }
}