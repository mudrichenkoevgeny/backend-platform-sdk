package com.mudrichenkoevgeny.backend.feature.user.route.security.useridentifiers

import com.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.common.model.toUserIdentifierIdOrThrow
import com.mudrichenkoevgeny.backend.core.common.network.constants.ApiFields
import com.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import com.mudrichenkoevgeny.backend.core.common.routing.respondResult
import com.mudrichenkoevgeny.backend.core.common.validation.validatePathParameter
import com.mudrichenkoevgeny.backend.core.common.validation.validateRequest
import com.mudrichenkoevgeny.backend.feature.user.mapper.toResponse
import com.mudrichenkoevgeny.backend.feature.user.network.request.confirmation.SendConfirmationToEmailRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.confirmation.SendConfirmationToPhoneRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.context.getRequestContext
import com.mudrichenkoevgeny.backend.feature.user.network.request.security.useridentifiers.AddUserIdentifierEmailRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.security.useridentifiers.AddUserIdentifierExternalAuthProviderRequest
import com.mudrichenkoevgeny.backend.feature.user.network.request.security.useridentifiers.AddUserIdentifierPhoneRequest
import com.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import com.mudrichenkoevgeny.backend.feature.user.route.security.SecurityRoutes
import com.mudrichenkoevgeny.backend.feature.user.usecase.security.useridentifiers.AddUserIdentifierEmailUseCase
import com.mudrichenkoevgeny.backend.feature.user.usecase.security.useridentifiers.AddUserIdentifierExternalAuthProviderUseCase
import com.mudrichenkoevgeny.backend.feature.user.usecase.security.useridentifiers.AddUserIdentifierPhoneUseCase
import com.mudrichenkoevgeny.backend.feature.user.usecase.security.useridentifiers.DeleteUserIdentifierUseCase
import com.mudrichenkoevgeny.backend.feature.user.usecase.security.useridentifiers.GetUserIdentifiersUseCase
import com.mudrichenkoevgeny.backend.feature.user.usecase.security.useridentifiers.SendAddEmailIdentifierConfirmationUseCase
import com.mudrichenkoevgeny.backend.feature.user.usecase.security.useridentifiers.SendAddPhoneIdentifierConfirmationUseCase
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

object SecurityUserIdentifiersRoutes {
    const val BASE_SECURITY_USER_IDENTIFIERS_ROUTE = "${SecurityRoutes.BASE_SECURITY_ROUTE}/user-identifier"
    const val GET_USER_IDENTIFIERS_ROUTE = BASE_SECURITY_USER_IDENTIFIERS_ROUTE
    const val DELETE_USER_IDENTIFIER = "$BASE_SECURITY_USER_IDENTIFIERS_ROUTE/{id}"
    const val ADD_USER_IDENTIFIER_EMAIL = "$BASE_SECURITY_USER_IDENTIFIERS_ROUTE/email/{id}"
    const val ADD_USER_IDENTIFIER_PHONE = "$BASE_SECURITY_USER_IDENTIFIERS_ROUTE/phone/{id}"
    const val ADD_USER_IDENTIFIER_EXTERNAL_AUTH_PROVIDER = "$BASE_SECURITY_USER_IDENTIFIERS_ROUTE/external-auth-provider/{id}"
    const val BASE_SECURITY_USER_IDENTIFIERS_CONFIRMATION_ROUTE = "${SecurityRoutes.BASE_SECURITY_ROUTE}/confirmation"
    const val SEND_ADD_EMAIL_IDENTIFIER_CONFIRMATION = "$BASE_SECURITY_USER_IDENTIFIERS_CONFIRMATION_ROUTE/send-to-email"
    const val SEND_ADD_PHONE_IDENTIFIER_CONFIRMATION = "$BASE_SECURITY_USER_IDENTIFIERS_CONFIRMATION_ROUTE/send-to-phone"
}

@Singleton
class SecurityUserIdentifiersRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val getUserIdentifiersUseCase: GetUserIdentifiersUseCase,
    private val deleteUserIdentifierUseCase: DeleteUserIdentifierUseCase,
    private val addUserIdentifierEmailUseCase: AddUserIdentifierEmailUseCase,
    private val addUserIdentifierPhoneUseCase: AddUserIdentifierPhoneUseCase,
    private val addUserIdentifierExternalAuthProviderUseCase: AddUserIdentifierExternalAuthProviderUseCase,
    private val sendAddEmailIdentifierConfirmationUseCase: SendAddEmailIdentifierConfirmationUseCase,
    private val sendAddPhoneIdentifierConfirmationUseCase: SendAddPhoneIdentifierConfirmationUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.get(
            path = SecurityUserIdentifiersRoutes.GET_USER_IDENTIFIERS_ROUTE,
            builder = { getUserIdentifiersDocs() },
            body = { getUserIdentifiers() }
        )

        route.delete(
            path = SecurityUserIdentifiersRoutes.DELETE_USER_IDENTIFIER,
            builder = { deleteUserIdentifierDocs() },
            body = { deleteUserIdentifier() }
        )

        route.post(
            path = SecurityUserIdentifiersRoutes.ADD_USER_IDENTIFIER_EMAIL,
            builder = { addUserIdentifierEmailDocs() },
            body = { addUserIdentifierEmail() }
        )

        route.post(
            path = SecurityUserIdentifiersRoutes.ADD_USER_IDENTIFIER_PHONE,
            builder = { addUserIdentifierPhoneDocs() },
            body = { addUserIdentifierPhone() }
        )

        route.post(
            path = SecurityUserIdentifiersRoutes.ADD_USER_IDENTIFIER_EXTERNAL_AUTH_PROVIDER,
            builder = { addUserIdentifierExternalAuthProviderDocs() },
            body = { addUserIdentifierExternalAuthProvider() }
        )

        route.post(
            path = SecurityUserIdentifiersRoutes.SEND_ADD_EMAIL_IDENTIFIER_CONFIRMATION,
            builder = { sendAddEmailIdentifierConfirmationDocs() },
            body = { sendAddEmailIdentifierConfirmation() }
        )

        route.post(
            path = SecurityUserIdentifiersRoutes.SEND_ADD_PHONE_IDENTIFIER_CONFIRMATION,
            builder = { sendAddPhoneIdentifierConfirmationDocs() },
            body = { sendAddPhoneIdentifierConfirmation() }
        )
    }

    private fun RouteConfig.getUserIdentifiersDocs() {
        summary = GET_USER_IDENTIFIERS_ROUTE_SUMMARY
        description = GET_USER_IDENTIFIERS_ROUTE_DESCRIPTION
        operationId = GET_USER_IDENTIFIERS_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.SECURITY)
        response {
            code(HttpStatusCode.OK) {
                description = GET_USER_IDENTIFIERS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getUserIdentifiers() {
        val result = getUserIdentifiersUseCase.execute(
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) {
            identifiers -> identifiers.map { it.toResponse() }
        }
    }

    private fun RouteConfig.deleteUserIdentifierDocs() {
        summary = DELETE_USER_IDENTIFIER_ROUTE_SUMMARY
        description = DELETE_USER_IDENTIFIER_ROUTE_DESCRIPTION
        operationId = DELETE_USER_IDENTIFIER_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.SECURITY)
        response {
            code(HttpStatusCode.OK) {
                description = DELETE_USER_IDENTIFIER_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.deleteUserIdentifier() {
        val userIdentifierId = call.validatePathParameter(ApiFields.ID) { id ->
            id.toUserIdentifierIdOrThrow()
        }

        val result = deleteUserIdentifierUseCase.execute(
            userIdentifierId = userIdentifierId,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun RouteConfig.addUserIdentifierEmailDocs() {
        summary = ADD_EMAIL_SUMMARY
        description = ADD_EMAIL_DESCRIPTION
        operationId = ADD_EMAIL_OPERATION_ID
        tags = listOf(UserSwaggerTags.SECURITY)
        request {
            body<AddUserIdentifierEmailRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = ADD_IDENTIFIER_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.addUserIdentifierEmail() {
        val request = call.validateRequest<AddUserIdentifierEmailRequest>()

        val result = addUserIdentifierEmailUseCase.execute(
            email = request.email,
            password = request.password,
            confirmationCode = request.confirmationCode,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) {
            it.toResponse()
        }
    }

    private fun RouteConfig.addUserIdentifierPhoneDocs() {
        summary = ADD_PHONE_SUMMARY
        description = ADD_PHONE_DESCRIPTION
        operationId = ADD_PHONE_OPERATION_ID
        tags = listOf(UserSwaggerTags.SECURITY)
        request {
            body<AddUserIdentifierPhoneRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = ADD_IDENTIFIER_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.addUserIdentifierPhone() {
        val request = call.validateRequest<AddUserIdentifierPhoneRequest>()

        val result = addUserIdentifierPhoneUseCase.execute(
            phoneNumber = request.phoneNumber,
            confirmationCode = request.confirmationCode,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) {
            it.toResponse()
        }
    }

    private fun RouteConfig.addUserIdentifierExternalAuthProviderDocs() {
        summary = ADD_EXTERNAL_SUMMARY
        description = ADD_EXTERNAL_DESCRIPTION
        operationId = ADD_EXTERNAL_OPERATION_ID
        tags = listOf(UserSwaggerTags.SECURITY)
        request {
            body<AddUserIdentifierExternalAuthProviderRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = ADD_IDENTIFIER_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.addUserIdentifierExternalAuthProvider() {
        val request = call.validateRequest<AddUserIdentifierExternalAuthProviderRequest>()

        val result = addUserIdentifierExternalAuthProviderUseCase.execute(
            authProviderKey = request.authProvider,
            token = request.token,
            requestContext = call.getRequestContext()
        )

        call.respondResult(result, appLogger, appErrorParser) {
            it.toResponse()
        }
    }

    private fun RouteConfig.sendAddEmailIdentifierConfirmationDocs() {
        summary = SEND_ADD_EMAIL_CONFIRMATION_SUMMARY
        description = SEND_ADD_EMAIL_CONFIRMATION_DESCRIPTION
        operationId = SEND_ADD_EMAIL_CONFIRMATION_OPERATION_ID
        tags = listOf(UserSwaggerTags.SECURITY)
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
        call.respondResult(result, appLogger, appErrorParser) { it.toResponse() }
    }

    private fun RouteConfig.sendAddPhoneIdentifierConfirmationDocs() {
        summary = SEND_ADD_PHONE_CONFIRMATION_SUMMARY
        description = SEND_ADD_PHONE_CONFIRMATION_DESCRIPTION
        operationId = SEND_ADD_PHONE_CONFIRMATION_OPERATION_ID
        tags = listOf(UserSwaggerTags.SECURITY)
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
        call.respondResult(result, appLogger, appErrorParser) { it.toResponse() }
    }

    companion object {
        const val GET_USER_IDENTIFIERS_ROUTE_SUMMARY = "Get user identifiers"
        const val GET_USER_IDENTIFIERS_ROUTE_DESCRIPTION = "Returns all authentication identifiers of the current user."
        const val GET_USER_IDENTIFIERS_ROUTE_OPERATION_ID = "getUserIdentifiers"
        const val GET_USER_IDENTIFIERS_ROUTE_RESPONSE_OK_DESCRIPTION = "Success"

        const val DELETE_USER_IDENTIFIER_ROUTE_SUMMARY = "Delete user identifier"
        const val DELETE_USER_IDENTIFIER_ROUTE_DESCRIPTION = "Deletes a user authentication identifier."
        const val DELETE_USER_IDENTIFIER_ROUTE_OPERATION_ID = "deleteUserIdentifier"
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
    }
}
