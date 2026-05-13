package io.github.mudrichenkoevgeny.backend.feature.user.route.open.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validatePathParameter
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.pagination.mapItems
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.query.parseIdentifiersListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.AddUserIdentifierEmailUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.AddUserIdentifierExternalAuthProviderUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.AddUserIdentifierPhoneUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.DeleteUserIdentifierUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.GetIdentifierUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.GetIdentifiersUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.IdentifierEmailChangePasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.SendAddEmailIdentifierConfirmationUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.SendAddPhoneIdentifierConfirmationUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.otpconfirmation.toOtpConfirmationPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.toUserIdentifierIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.identifier.toUserIdentifierPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiFields
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiPaths
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.confirmation.SendConfirmationToEmailRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.confirmation.SendConfirmationToPhoneRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.security.password.EmailPasswordChangeRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.security.useridentifiers.AddUserIdentifierEmailRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.security.useridentifiers.AddUserIdentifierExternalAuthProviderRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.security.useridentifiers.AddUserIdentifierPhoneRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.open.identifier.OpenIdentifierRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User identifier management routes for authenticated users.
 *
 * This router handles the lifecycle of user identities (email, phone, external providers),
 * including linking new identifiers, removing existing ones, and updating security
 * credentials like passwords. All routes are protected by JWT authentication.
 *
 * Registered routes:
 * 1. [OpenIdentifierRoutes.GET_IDENTIFIER] — retrieves specific identifier details via [GetIdentifierUseCase].
 * 2. [OpenIdentifierRoutes.GET_IDENTIFIERS] — lists linked identities via [GetIdentifiersUseCase].
 * 3. [OpenIdentifierRoutes.DELETE_IDENTIFIER] — unlinks a specific identity via [DeleteUserIdentifierUseCase].
 * 4. [OpenIdentifierRoutes.ADD_IDENTIFIER_EMAIL] — links an email via [AddUserIdentifierEmailUseCase].
 * 5. [OpenIdentifierRoutes.ADD_IDENTIFIER_PHONE] — links a phone via [AddUserIdentifierPhoneUseCase].
 * 6. [OpenIdentifierRoutes.ADD_IDENTIFIER_EXTERNAL_AUTH_PROVIDER] — links social/external auth via [AddUserIdentifierExternalAuthProviderUseCase].
 * 7. [OpenIdentifierRoutes.SEND_ADD_EMAIL_IDENTIFIER_CONFIRMATION] — sends email OTP via [SendAddEmailIdentifierConfirmationUseCase].
 * 8. [OpenIdentifierRoutes.SEND_ADD_PHONE_IDENTIFIER_CONFIRMATION] — sends SMS OTP via [SendAddPhoneIdentifierConfirmationUseCase].
 * 9. [OpenIdentifierRoutes.IDENTIFIER_EMAIL_CHANGE_PASSWORD] — updates password for email identities via [IdentifierEmailChangePasswordUseCase].
 */
@Singleton
class OpenIdentifierRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val getIdentifierUseCase: GetIdentifierUseCase,
    private val getIdentifiersUseCase: GetIdentifiersUseCase,
    private val deleteUserIdentifierUseCase: DeleteUserIdentifierUseCase,
    private val addUserIdentifierEmailUseCase: AddUserIdentifierEmailUseCase,
    private val addUserIdentifierPhoneUseCase: AddUserIdentifierPhoneUseCase,
    private val addUserIdentifierExternalAuthProviderUseCase: AddUserIdentifierExternalAuthProviderUseCase,
    private val sendAddEmailIdentifierConfirmationUseCase: SendAddEmailIdentifierConfirmationUseCase,
    private val sendAddPhoneIdentifierConfirmationUseCase: SendAddPhoneIdentifierConfirmationUseCase,
    private val identifierEmailChangePasswordUseCase: IdentifierEmailChangePasswordUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerGetIdentifierRoute(this)
            registerGetIdentifiersRoute(this)
            registerDeleteIdentifierRoute(this)
            registerAddIdentifierEmailRoute(this)
            registerAddIdentifierPhoneRoute(this)
            registerAddIdentifierExternalAuthProviderRoute(this)
            registerSendAddEmailIdentifierConfirmationRoute(this)
            registerSendAddPhoneIdentifierConfirmationRoute(this)
            registerIdentifierEmailChangePasswordRoute(this)
        }
    }

    private fun registerGetIdentifierRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.get(
            path = OpenIdentifierRoutes.GET_IDENTIFIER,
            builder = { getIdentifierDocs(allowedRoles, allowedAccountStatuses) },
            body = { getIdentifier(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerGetIdentifiersRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.get(
            path = OpenIdentifierRoutes.GET_IDENTIFIERS,
            builder = { getIdentifiersDocs(allowedRoles, allowedAccountStatuses) },
            body = { getIdentifiers(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerDeleteIdentifierRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.delete(
            path = OpenIdentifierRoutes.DELETE_IDENTIFIER,
            builder = { deleteIdentifierDocs(allowedRoles, allowedAccountStatuses) },
            body = { deleteIdentifier(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerAddIdentifierEmailRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.post(
            path = OpenIdentifierRoutes.ADD_IDENTIFIER_EMAIL,
            builder = { addIdentifierEmailDocs(allowedRoles, allowedAccountStatuses) },
            body = { addIdentifierEmail(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerAddIdentifierPhoneRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.post(
            path = OpenIdentifierRoutes.ADD_IDENTIFIER_PHONE,
            builder = { addIdentifierPhoneDocs(allowedRoles, allowedAccountStatuses) },
            body = { addIdentifierPhone(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerAddIdentifierExternalAuthProviderRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.post(
            path = OpenIdentifierRoutes.ADD_IDENTIFIER_EXTERNAL_AUTH_PROVIDER,
            builder = { addIdentifierExternalAuthProviderDocs(allowedRoles, allowedAccountStatuses) },
            body = { addIdentifierExternalAuthProvider(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerSendAddEmailIdentifierConfirmationRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.post(
            path = OpenIdentifierRoutes.SEND_ADD_EMAIL_IDENTIFIER_CONFIRMATION,
            builder = { sendAddEmailIdentifierConfirmationDocs(allowedRoles, allowedAccountStatuses) },
            body = { sendAddEmailIdentifierConfirmation(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerSendAddPhoneIdentifierConfirmationRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.post(
            path = OpenIdentifierRoutes.SEND_ADD_PHONE_IDENTIFIER_CONFIRMATION,
            builder = { sendAddPhoneIdentifierConfirmationDocs(allowedRoles, allowedAccountStatuses) },
            body = { sendAddPhoneIdentifierConfirmation(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerIdentifierEmailChangePasswordRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.post(
            path = OpenIdentifierRoutes.IDENTIFIER_EMAIL_CHANGE_PASSWORD,
            builder = { identifierEmailChangePasswordDocs(allowedRoles, allowedAccountStatuses) },
            body = { identifierEmailChangePassword(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.getIdentifierDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_USER_IDENTIFIER_ROUTE_SUMMARY
        operationId = GET_USER_IDENTIFIER_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = GET_USER_IDENTIFIER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request {
            pathParameter<String>(UserApiPaths.USER_IDENTIFIER_ID) {
                description = GET_USER_IDENTIFIER_ROUTE_PATH_PARAMETER_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = GET_USER_IDENTIFIER_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getIdentifier(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val identifierId = call.validatePathParameter(UserApiPaths.USER_IDENTIFIER_ID) { identifierId ->
            identifierId.toUserIdentifierIdOrThrow()
        }
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = getIdentifierUseCase(
            identifierId = identifierId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { userIdentifier ->
            userIdentifier.toUserIdentifierPayload()
        }
    }

    private fun RouteConfig.getIdentifiersDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_USER_IDENTIFIERS_ROUTE_SUMMARY
        operationId = GET_USER_IDENTIFIERS_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = GET_USER_IDENTIFIERS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        response {
            code(HttpStatusCode.OK) {
                description = GET_USER_IDENTIFIERS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getIdentifiers(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val queryParams = call.parseIdentifiersListQueryParams()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = getIdentifiersUseCase(
            authenticatedRequestContext = authenticatedRequestContext,
            pageParams = queryParams.listing.pageParams,
            sortBy = queryParams.listing.sortBy,
            sortOrder = queryParams.listing.sortOrder,
            userAuthProviders = queryParams.userAuthProviders,
            identifiers = queryParams.identifiers
        )

        call.respondResult(result, appLogger, appErrorParser) { pagedIdentifiers ->
            pagedIdentifiers.mapItems { userIdentifier -> userIdentifier.toUserIdentifierPayload() }
        }
    }

    private fun RouteConfig.deleteIdentifierDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = DELETE_USER_IDENTIFIER_ROUTE_SUMMARY
        operationId = DELETE_USER_IDENTIFIER_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = DELETE_USER_IDENTIFIER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
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

    private suspend fun RoutingContext.deleteIdentifier(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val userIdentifierId = call.validatePathParameter(UserApiPaths.USER_IDENTIFIER_ID) { id ->
            id.toUserIdentifierIdOrThrow()
        }
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = UserAuditActionType.SELF_DELETE_IDENTIFIER,
                resourceId = userIdentifierId.asHexDashString(),
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = deleteUserIdentifierUseCase(
            userIdentifierId = userIdentifierId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun RouteConfig.addIdentifierEmailDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = ADD_EMAIL_SUMMARY
        operationId = ADD_EMAIL_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = ADD_EMAIL_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request { body<AddUserIdentifierEmailRequest>() }
        response {
            code(HttpStatusCode.OK) {
                description = ADD_IDENTIFIER_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.addIdentifierEmail(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val request = call.validateRequest<AddUserIdentifierEmailRequest>()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = UserAuditActionType.ADD_IDENTIFIER_EMAIL,
                resourceId = null,
                error = authorizeResult.error,
                extraMetadata = setOf(
                    AuditEventMetadata(
                        key = UserAuditMetadataKey.EMAIL_ADDRESS,
                        value = request.email
                    )
                )
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = addUserIdentifierEmailUseCase(
            email = request.email,
            password = request.password,
            confirmationCode = request.confirmationCode,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { userIdentifier ->
            userIdentifier.toUserIdentifierPayload()
        }
    }

    private fun RouteConfig.addIdentifierPhoneDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = ADD_PHONE_SUMMARY
        operationId = ADD_PHONE_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = ADD_PHONE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request { body<AddUserIdentifierPhoneRequest>() }
        response {
            code(HttpStatusCode.OK) {
                description = ADD_IDENTIFIER_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.addIdentifierPhone(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val request = call.validateRequest<AddUserIdentifierPhoneRequest>()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = UserAuditActionType.ADD_IDENTIFIER_PHONE,
                resourceId = null,
                error = authorizeResult.error,
                extraMetadata = setOf(
                    AuditEventMetadata(
                        key = UserAuditMetadataKey.PHONE_NUMBER,
                        value = request.phoneNumber
                    )
                )
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = addUserIdentifierPhoneUseCase(
            phoneNumber = request.phoneNumber,
            confirmationCode = request.confirmationCode,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { userIdentifier ->
            userIdentifier.toUserIdentifierPayload()
        }
    }

    private fun RouteConfig.addIdentifierExternalAuthProviderDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = ADD_EXTERNAL_SUMMARY
        operationId = ADD_EXTERNAL_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = ADD_EXTERNAL_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request { body<AddUserIdentifierExternalAuthProviderRequest>() }
        response {
            code(HttpStatusCode.OK) {
                description = ADD_IDENTIFIER_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.addIdentifierExternalAuthProvider(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val request = call.validateRequest<AddUserIdentifierExternalAuthProviderRequest>()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = UserAuditActionType.ADD_IDENTIFIER_EXTERNAL_AUTH_PROVIDER,
                resourceId = null,
                error = authorizeResult.error,
                extraMetadata = setOf(
                    AuditEventMetadata(
                        key = UserAuditMetadataKey.EXTERNAL_ID,
                        value = request.token
                    )
                )
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val authProvider = UserAuthProvider.fromValueOrNull(request.authProvider)
            ?: throw RequestHandlingException(
                CommonError.InvalidFieldValue(
                    fieldName = UserApiFields.AUTH_PROVIDER
                )
            )

        val result = addUserIdentifierExternalAuthProviderUseCase(
            authProvider = authProvider,
            token = request.token,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { userIdentifier ->
            userIdentifier.toUserIdentifierPayload()
        }
    }

    private fun RouteConfig.sendAddEmailIdentifierConfirmationDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = SEND_ADD_EMAIL_CONFIRMATION_SUMMARY
        operationId = SEND_ADD_EMAIL_CONFIRMATION_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = SEND_ADD_EMAIL_CONFIRMATION_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request { body<SendConfirmationToEmailRequest>() }
        response {
            code(HttpStatusCode.OK) {
                description = CONFIRMATION_SENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.sendAddEmailIdentifierConfirmation(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val request = call.validateRequest<SendConfirmationToEmailRequest>()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = sendAddEmailIdentifierConfirmationUseCase(
            email = request.email,
            authenticatedRequestContext = authenticatedRequestContext
        )
        call.respondResult(result, appLogger, appErrorParser) { otpConfirmation ->
            otpConfirmation.toOtpConfirmationPayload()
        }
    }

    private fun RouteConfig.sendAddPhoneIdentifierConfirmationDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = SEND_ADD_PHONE_CONFIRMATION_SUMMARY
        operationId = SEND_ADD_PHONE_CONFIRMATION_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = SEND_ADD_PHONE_CONFIRMATION_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request { body<SendConfirmationToPhoneRequest>() }
        response {
            code(HttpStatusCode.OK) {
                description = CONFIRMATION_SENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.sendAddPhoneIdentifierConfirmation(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val request = call.validateRequest<SendConfirmationToPhoneRequest>()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = sendAddPhoneIdentifierConfirmationUseCase(
            phoneNumber = request.phoneNumber,
            authenticatedRequestContext = authenticatedRequestContext
        )
        call.respondResult(result, appLogger, appErrorParser) { otpConfirmation ->
            otpConfirmation.toOtpConfirmationPayload()
        }
    }

    private fun RouteConfig.identifierEmailChangePasswordDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = IDENTIFIER_EMAIL_CHANGE_PASSWORD_SUMMARY
        operationId = IDENTIFIER_EMAIL_CHANGE_PASSWORD_OPERATION_ID
        tags = listOf(UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = IDENTIFIER_EMAIL_CHANGE_PASSWORD_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request { body<EmailPasswordChangeRequest>() }
        response {
            code(HttpStatusCode.OK) {
                description = IDENTIFIER_EMAIL_CHANGE_PASSWORD_RESPONSE_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.identifierEmailChangePassword(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val request = call.validateRequest<EmailPasswordChangeRequest>()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = UserAuditActionType.CHANGE_PASSWORD,
                resourceId = null,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = identifierEmailChangePasswordUseCase(
            email = request.email,
            oldPassword = request.oldPassword,
            newPassword = request.newPassword,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { userIdentifier ->
            userIdentifier.toUserIdentifierPayload()
        }
    }

    private fun logErrorToAudit(
        authenticatedRequestContext: AuthenticatedRequestContext,
        actionType: AuditActionType,
        resourceId: String?,
        error: AppError,
        extraMetadata: Set<AuditEventMetadata> = emptySet()
    ) {
        val errorData = auditErrorConverter.convert(error)
        val metadata = authenticatedRequestContext.clientInfo.toAuditMetadata() +
                errorData.metadata +
                extraMetadata +
                AuditEventMetadata(
                    key = UserAuditMetadataKey.SESSION_ID,
                    value = authenticatedRequestContext.sessionId.asHexDashString()
                )

        auditLogger.log(
            actorId = authenticatedRequestContext.userId.asHexDashString(),
            actorType = AuditActorType.USER,
            actorUserRole = authenticatedRequestContext.userRole.serialName,
            action = actionType,
            resource = UserAuditResourceType.IDENTIFIER,
            resourceId = resourceId,
            status = AuditStatus.DENIED,
            metadata = metadata
        )
    }

    companion object {
        const val GET_USER_IDENTIFIER_ROUTE_SUMMARY = "Get user identifier"
        const val GET_USER_IDENTIFIER_ROUTE_DESCRIPTION = "Retrieves specific identifier details linked to the current account."
        const val GET_USER_IDENTIFIER_ROUTE_OPERATION_ID = "getUserIdentifier"
        const val GET_USER_IDENTIFIER_ROUTE_PATH_PARAMETER_ID_DESCRIPTION = "The unique identifier ID"
        const val GET_USER_IDENTIFIER_ROUTE_RESPONSE_OK_DESCRIPTION = "Identifier details"

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