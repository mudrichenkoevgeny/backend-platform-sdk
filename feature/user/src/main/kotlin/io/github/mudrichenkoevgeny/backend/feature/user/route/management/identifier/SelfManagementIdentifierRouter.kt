package io.github.mudrichenkoevgeny.backend.feature.user.route.management.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.pagination.mapItems
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.query.parseIdentifiersListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.GetIdentifiersUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier.IdentifierEmailChangePasswordUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.identifier.toUserIdentifierPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.security.password.EmailPasswordChangeRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.management.identifier.SelfManagementIdentifierRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management HTTP routes for self-service identifier management and security updates.
 *
 * Registered routes:
 * 1. [SelfManagementIdentifierRoutes.GET_IDENTIFIERS] — retrieves identifiers linked to the current account via [GetIdentifiersUseCase].
 * 2. [SelfManagementIdentifierRoutes.IDENTIFIER_EMAIL_CHANGE_PASSWORD] — handles password updates for email identifiers via [IdentifierEmailChangePasswordUseCase].
 */
@Singleton
class SelfManagementIdentifierRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val getIdentifiersUseCase: GetIdentifiersUseCase,
    private val identifierEmailChangePasswordUseCase: IdentifierEmailChangePasswordUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerGetIdentifiersRoute(this)
            registerIdentifierEmailChangePasswordRoute(this)
        }
    }

    private fun registerGetIdentifiersRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.get(
            path = SelfManagementIdentifierRoutes.GET_IDENTIFIERS,
            builder = { getIdentifiersDocs(allowedRoles, allowedAccountStatuses) },
            body = { getIdentifiers(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerIdentifierEmailChangePasswordRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.post(
            path = SelfManagementIdentifierRoutes.IDENTIFIER_EMAIL_CHANGE_PASSWORD,
            builder = { identifierEmailChangePasswordDocs(allowedRoles, allowedAccountStatuses) },
            body = { identifierEmailChangePassword(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.getIdentifiersDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_IDENTIFIERS_SUMMARY
        operationId = GET_IDENTIFIERS_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = GET_IDENTIFIERS_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        response {
            code(HttpStatusCode.OK) {
                description = GET_IDENTIFIERS_RESPONSE_DESCRIPTION
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

    private fun RouteConfig.identifierEmailChangePasswordDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = CHANGE_PASSWORD_SUMMARY
        operationId = CHANGE_PASSWORD_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = CHANGE_PASSWORD_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request { body<EmailPasswordChangeRequest>() }
        response {
            code(HttpStatusCode.OK) {
                description = CHANGE_PASSWORD_RESPONSE_DESCRIPTION
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
        error: AppError
    ) {
        val errorData = auditErrorConverter.convert(error)
        val metadata = authenticatedRequestContext.clientInfo.toAuditMetadata() + errorData.metadata

        auditLogger.log(
            actorId = authenticatedRequestContext.userId.asHexDashString(),
            actorType = AuditActorType.USER,
            actorUserRole = authenticatedRequestContext.userRole.serialName,
            action = actionType,
            resource = UserAuditResourceType.IDENTIFIER,
            status = AuditStatus.DENIED,
            metadata = metadata
        )
    }

    companion object {
        private const val GET_IDENTIFIERS_SUMMARY = "Get current management identifiers"
        private const val GET_IDENTIFIERS_OPERATION_ID = "getSelfManagementIdentifiers"
        private const val GET_IDENTIFIERS_DESCRIPTION = "Returns identifiers linked to the current authenticated management account."
        private const val GET_IDENTIFIERS_RESPONSE_DESCRIPTION = "Paged management identifiers"

        private const val CHANGE_PASSWORD_SUMMARY = "Change management password"
        private const val CHANGE_PASSWORD_OPERATION_ID = "selfManagementChangePassword"
        private const val CHANGE_PASSWORD_DESCRIPTION = "Updates the management account password using current credentials."
        private const val CHANGE_PASSWORD_RESPONSE_DESCRIPTION = "Identifier updated"
    }
}