package io.github.mudrichenkoevgeny.backend.feature.user.route.management.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.model.SecurityRequirementType
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validatePathParameter
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.query.parseManagementIdentifiersListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier.ManagementDeleteIdentifierPasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier.ManagementDeleteIdentifierUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier.ManagementGetIdentifierUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier.ManagementGetIdentifiersUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.mapper.pagedresult.mapItems
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.toUserIdentifierIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.toUserIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.identifier.toUserIdentifierPrivatePayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.identifier.toUserIdentifierSummaryPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiPaths
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.identifier.UserIdentifierPrivatePayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.identifier.UserIdentifierSummaryPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.management.users.identifiers.ManagementIdentifiersRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management HTTP routes for managing user identifiers (e.g., email, phone).
 *
 * Registered routes:
 * 1. [ManagementIdentifiersRoutes.GET_IDENTIFIERS] — retrieves a filtered, paginated list of identifiers via [ManagementGetIdentifiersUseCase].
 * 2. [ManagementIdentifiersRoutes.GET_USER_IDENTIFIERS] — retrieves a filtered, paginated list of identifiers for a target user via [ManagementGetIdentifiersUseCase].
 * 3. [ManagementIdentifiersRoutes.GET_IDENTIFIER] — retrieves a specific identifier's details via [ManagementGetIdentifierUseCase].
 * 4. [ManagementIdentifiersRoutes.DELETE_IDENTIFIER] — removes a user identifier via [ManagementDeleteIdentifierUseCase].
 * 5. [ManagementIdentifiersRoutes.DELETE_IDENTIFIER_PASSWORD] — revokes an identifier's password via [ManagementDeleteIdentifierPasswordUseCase].
 */
@Singleton
class ManagementIdentifierRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val managementGetIdentifiersUseCase: ManagementGetIdentifiersUseCase,
    private val managementGetIdentifierUseCase: ManagementGetIdentifierUseCase,
    private val managementDeleteIdentifierUseCase: ManagementDeleteIdentifierUseCase,
    private val managementDeleteIdentifierPasswordUseCase: ManagementDeleteIdentifierPasswordUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerGetIdentifiersRoute(this)
            registerGetUserIdentifiersRoute(this)
            registerGetIdentifierRoute(this)
            registerDeleteIdentifierRoute(this)
            registerDeleteIdentifierPasswordRoute(this)
        }
    }

    private fun registerGetIdentifiersRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.get(
            path = ManagementIdentifiersRoutes.GET_IDENTIFIERS,
            builder = { getIdentifiersDocs(allowedRoles, allowedAccountStatuses) },
            body = { getIdentifiers(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerGetUserIdentifiersRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.get(
            path = ManagementIdentifiersRoutes.GET_USER_IDENTIFIERS,
            builder = { getUserIdentifiersDocs(allowedRoles, allowedAccountStatuses) },
            body = { getUserIdentifiers(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerGetIdentifierRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.get(
            path = ManagementIdentifiersRoutes.GET_IDENTIFIER,
            builder = { getIdentifierDocs(allowedRoles, allowedAccountStatuses) },
            body = { getIdentifier(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerDeleteIdentifierRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.delete(
            path = ManagementIdentifiersRoutes.DELETE_IDENTIFIER,
            builder = { deleteIdentifierDocs(allowedRoles, allowedAccountStatuses) },
            body = { deleteIdentifier(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerDeleteIdentifierPasswordRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.delete(
            path = ManagementIdentifiersRoutes.DELETE_IDENTIFIER_PASSWORD,
            builder = { deleteIdentifierPasswordDocs(allowedRoles, allowedAccountStatuses) },
            body = { deleteIdentifierPassword(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.getIdentifiersDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_IDENTIFIERS_ROUTE_SUMMARY
        operationId = GET_IDENTIFIERS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = GET_IDENTIFIERS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value }
        )
        response {
            code(HttpStatusCode.OK) {
                body<PagedResult<UserIdentifierSummaryPayload>>()
                description = GET_IDENTIFIERS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getIdentifiers(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val queryParams = call.parseManagementIdentifiersListQueryParams()

        val result = managementGetIdentifiersUseCase(
            pageParams = queryParams.listing.pageParams,
            sortBy = queryParams.listing.sortBy,
            sortOrder = queryParams.listing.sortOrder,
            userIds = queryParams.userIds,
            userAuthProviders = queryParams.userAuthProviders,
            identifiers = queryParams.identifiers,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { pagedIdentifiers ->
            pagedIdentifiers.mapItems { userIdentifierSummary -> userIdentifierSummary.toUserIdentifierSummaryPayload() }
        }
    }

    private fun RouteConfig.getUserIdentifiersDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_USER_IDENTIFIERS_ROUTE_SUMMARY
        operationId = GET_USER_IDENTIFIERS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = GET_USER_IDENTIFIERS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value }
        )
        request {
            pathParameter<String>(UserApiPaths.USER_ID) {
                description = GET_USER_IDENTIFIERS_ROUTE_PATH_USER_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.OK) {
                body<PagedResult<UserIdentifierSummaryPayload>>()
                description = GET_USER_IDENTIFIERS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getUserIdentifiers(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val userId = call.validatePathParameter(UserApiPaths.USER_ID) { id ->
            id.toUserIdOrThrow()
        }

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val queryParams = call.parseManagementIdentifiersListQueryParams()

        val result = managementGetIdentifiersUseCase(
            pageParams = queryParams.listing.pageParams,
            sortBy = queryParams.listing.sortBy,
            sortOrder = queryParams.listing.sortOrder,
            userIds = listOf(userId),
            userAuthProviders = queryParams.userAuthProviders,
            identifiers = queryParams.identifiers,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { pagedIdentifiers ->
            pagedIdentifiers.mapItems { userIdentifierSummary -> userIdentifierSummary.toUserIdentifierSummaryPayload() }
        }
    }

    private fun RouteConfig.getIdentifierDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_IDENTIFIER_ROUTE_SUMMARY
        operationId = GET_IDENTIFIER_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = GET_IDENTIFIER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value }
        )
        request {
            pathParameter<String>(UserApiPaths.USER_IDENTIFIER_ID) {
                description = GET_IDENTIFIER_ROUTE_PATH_IDENTIFIER_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.OK) {
                body<UserIdentifierPrivatePayload>()
                description = GET_IDENTIFIER_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getIdentifier(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val userIdentifierId = call.validatePathParameter(UserApiPaths.USER_IDENTIFIER_ID) { userIdentifierId ->
            userIdentifierId.toUserIdentifierIdOrThrow()
        }

        val result = managementGetIdentifierUseCase(
            userIdentifierId = userIdentifierId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { userIdentifierPrivate ->
            userIdentifierPrivate.toUserIdentifierPrivatePayload()
        }
    }

    private fun RouteConfig.deleteIdentifierDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = DELETE_IDENTIFIER_ROUTE_SUMMARY
        operationId = DELETE_IDENTIFIER_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = DELETE_IDENTIFIER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            securityType = SecurityRequirementType.SENSITIVE_STEP_UP_TOTP_REQUIRED
        )
        request {
            pathParameter<String>(UserApiPaths.USER_IDENTIFIER_ID) {
                description = DELETE_IDENTIFIER_ROUTE_PATH_IDENTIFIER_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.NoContent) {
                description = DELETE_IDENTIFIER_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.deleteIdentifier(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val userIdentifierId = call.validatePathParameter(UserApiPaths.USER_IDENTIFIER_ID) { userIdentifierId ->
            userIdentifierId.toUserIdentifierIdOrThrow()
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
                actionType = UserAuditActionType.MANAGEMENT_DELETE_IDENTIFIER,
                resourceId = userIdentifierId.asHexDashString(),
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = managementDeleteIdentifierUseCase(
            userIdentifierId = userIdentifierId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun RouteConfig.deleteIdentifierPasswordDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = DELETE_IDENTIFIER_PASSWORD_ROUTE_SUMMARY
        operationId = DELETE_IDENTIFIER_PASSWORD_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = DELETE_IDENTIFIER_PASSWORD_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            securityType = SecurityRequirementType.SENSITIVE_STEP_UP_TOTP_REQUIRED
        )
        request {
            pathParameter<String>(UserApiPaths.USER_IDENTIFIER_ID) {
                description = DELETE_IDENTIFIER_PASSWORD_ROUTE_PATH_IDENTIFIER_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.NoContent) {
                description = DELETE_IDENTIFIER_PASSWORD_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.deleteIdentifierPassword(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val userIdentifierId = call.validatePathParameter(UserApiPaths.USER_IDENTIFIER_ID) { userIdentifierId ->
            userIdentifierId.toUserIdentifierIdOrThrow()
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
                actionType = UserAuditActionType.MANAGEMENT_DELETE_IDENTIFIER_PASSWORD,
                resourceId = userIdentifierId.asHexDashString(),
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = managementDeleteIdentifierPasswordUseCase(
            userIdentifierId = userIdentifierId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun logErrorToAudit(
        authenticatedRequestContext: AuthenticatedRequestContext,
        actionType: AuditActionType,
        resourceId: String? = null,
        error: AppError
    ) {
        val errorData = auditErrorConverter.convert(error)
        val metadata = authenticatedRequestContext.clientInfo.toAuditMetadata() + errorData.metadata

        auditLogger.log(
            actorId = authenticatedRequestContext.userId.asHexDashString(),
            actorType = AuditActorType.USER,
            actorUserRole = authenticatedRequestContext.userRole.value,
            action = actionType,
            resource = UserAuditResourceType.IDENTIFIER,
            resourceId = resourceId,
            status = AuditStatus.DENIED,
            metadata = metadata
        )
    }

    companion object {
        const val GET_IDENTIFIERS_ROUTE_SUMMARY = "List user identifiers (management)"
        const val GET_IDENTIFIERS_ROUTE_DESCRIPTION =
            "Returns a paginated list of identifiers. Filters and row-level access follow the management identifier API contract."
        const val GET_IDENTIFIERS_ROUTE_OPERATION_ID = "getManagementUserIdentifiers"
        const val GET_IDENTIFIERS_ROUTE_RESPONSE_OK_DESCRIPTION = "Paged user identifiers"

        const val GET_USER_IDENTIFIERS_ROUTE_SUMMARY = "List target user's identifiers (management)"
        const val GET_USER_IDENTIFIERS_ROUTE_DESCRIPTION =
            "Returns a paginated list of identifiers for a specific user."
        const val GET_USER_IDENTIFIERS_ROUTE_OPERATION_ID = "getManagementUserTargetIdentifiers"
        const val GET_USER_IDENTIFIERS_ROUTE_PATH_USER_ID_DESCRIPTION = "User id (UUID string, hex with dashes)"
        const val GET_USER_IDENTIFIERS_ROUTE_RESPONSE_OK_DESCRIPTION = "Paged user identifiers"

        const val GET_IDENTIFIER_ROUTE_SUMMARY = "Get user identifier (management)"
        const val GET_IDENTIFIER_ROUTE_DESCRIPTION = "Returns a single identifier."
        const val GET_IDENTIFIER_ROUTE_OPERATION_ID = "getManagementUserIdentifier"
        const val GET_IDENTIFIER_ROUTE_PATH_IDENTIFIER_ID_DESCRIPTION = "User identifier id (UUID string, hex with dashes)"
        const val GET_IDENTIFIER_ROUTE_RESPONSE_OK_DESCRIPTION = "User identifier"

        const val DELETE_IDENTIFIER_ROUTE_SUMMARY = "Delete user identifier (management)"
        const val DELETE_IDENTIFIER_ROUTE_DESCRIPTION = "Deletes a user identifier record."
        const val DELETE_IDENTIFIER_ROUTE_OPERATION_ID = "deleteManagementUserIdentifier"
        const val DELETE_IDENTIFIER_ROUTE_PATH_IDENTIFIER_ID_DESCRIPTION =
            "User identifier id (UUID string, hex with dashes)"
        const val DELETE_IDENTIFIER_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION = "Identifier removed; no response body."

        const val DELETE_IDENTIFIER_PASSWORD_ROUTE_SUMMARY = "Delete user identifier password (management)"
        const val DELETE_IDENTIFIER_PASSWORD_ROUTE_DESCRIPTION =
            "Revokes and removes the password credential from a specific user identifier."
        const val DELETE_IDENTIFIER_PASSWORD_ROUTE_OPERATION_ID = "deleteManagementUserIdentifierPassword"
        const val DELETE_IDENTIFIER_PASSWORD_ROUTE_PATH_IDENTIFIER_ID_DESCRIPTION =
            "User identifier id (UUID string, hex with dashes)"
        const val DELETE_IDENTIFIER_PASSWORD_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION = "Password removed; no response body."
    }
}
