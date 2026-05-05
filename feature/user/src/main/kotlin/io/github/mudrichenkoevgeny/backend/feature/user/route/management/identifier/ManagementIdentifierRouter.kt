package io.github.mudrichenkoevgeny.backend.feature.user.route.management.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.pagination.mapItems
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validatePathParameter
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.query.parseIdentifiersListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier.ManagementDeleteIdentifierUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier.ManagementGetIdentifierUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier.ManagementGetIdentifiersUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.toUserIdentifierIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.identifier.toUserIdentifierPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiPaths
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.management.identifier.ManagementIdentifierRoutes
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
 * 1. [ManagementIdentifierRoutes.GET_IDENTIFIERS] — retrieves a filtered, paginated list of identifiers via [ManagementGetIdentifiersUseCase].
 * 2. [ManagementIdentifierRoutes.GET_IDENTIFIER] — retrieves a specific identifier's details via [ManagementGetIdentifierUseCase].
 * 3. [ManagementIdentifierRoutes.DELETE_IDENTIFIER] — removes a user identifier via [ManagementDeleteIdentifierUseCase].
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
    private val managementDeleteIdentifierUseCase: ManagementDeleteIdentifierUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerGetIdentifiersRoute(this)
            registerGetIdentifierRoute(this)
            registerDeleteIdentifierRoute(this)
        }
    }

    private fun registerGetIdentifiersRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.get(
            path = ManagementIdentifierRoutes.GET_IDENTIFIERS,
            builder = { getIdentifiersDocs(allowedRoles, allowedAccountStatuses) },
            body = { getIdentifiers(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerGetIdentifierRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.get(
            path = ManagementIdentifierRoutes.GET_IDENTIFIER,
            builder = { getIdentifierDocs(allowedRoles, allowedAccountStatuses) },
            body = { getIdentifier(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerDeleteIdentifierRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.delete(
            path = ManagementIdentifierRoutes.DELETE_IDENTIFIER,
            builder = { deleteIdentifierDocs(allowedRoles, allowedAccountStatuses) },
            body = { deleteIdentifier(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.getIdentifiersDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_IDENTIFIERS_ROUTE_SUMMARY
        operationId = GET_IDENTIFIERS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = GET_IDENTIFIERS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        response {
            code(HttpStatusCode.OK) {
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
        val queryParams = call.parseIdentifiersListQueryParams()

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
            pagedIdentifiers.mapItems { userIdentifier -> userIdentifier.toUserIdentifierPayload() }
        }
    }

    private fun RouteConfig.getIdentifierDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_IDENTIFIER_ROUTE_SUMMARY
        operationId = GET_IDENTIFIER_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = GET_IDENTIFIER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request {
            pathParameter<String>(UserApiPaths.USER_ID) {
                description = GET_IDENTIFIER_ROUTE_PATH_USER_ID_DESCRIPTION
            }
            pathParameter<String>(UserApiPaths.USER_IDENTIFIER_ID) {
                description = GET_IDENTIFIER_ROUTE_PATH_IDENTIFIER_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.OK) {
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

        call.respondResult(result, appLogger, appErrorParser) { internal ->
            internal.toUserIdentifierPayload()
        }
    }

    private fun RouteConfig.deleteIdentifierDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = DELETE_IDENTIFIER_ROUTE_SUMMARY
        operationId = DELETE_IDENTIFIER_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.IDENTIFIER)
        description = getFormattedDescription(
            description = DELETE_IDENTIFIER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request {
            pathParameter<String>(UserApiPaths.USER_ID) {
                description = DELETE_IDENTIFIER_ROUTE_PATH_USER_ID_DESCRIPTION
            }
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
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val userIdentifierId = call.validatePathParameter(UserApiPaths.USER_IDENTIFIER_ID) { userIdentifierId ->
            userIdentifierId.toUserIdentifierIdOrThrow()
        }

        val result = managementDeleteIdentifierUseCase(
            userIdentifierId = userIdentifierId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
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
        const val GET_IDENTIFIERS_ROUTE_SUMMARY = "List user identifiers (management)"
        const val GET_IDENTIFIERS_ROUTE_DESCRIPTION =
            "Returns a paginated list of identifiers. Filters and row-level access follow the management identifier API contract."
        const val GET_IDENTIFIERS_ROUTE_OPERATION_ID = "getManagementUserIdentifiers"
        const val GET_IDENTIFIERS_ROUTE_RESPONSE_OK_DESCRIPTION = "Paged user identifiers"

        const val GET_IDENTIFIER_ROUTE_SUMMARY = "Get user identifier (management)"
        const val GET_IDENTIFIER_ROUTE_DESCRIPTION = "Returns a single identifier for the given user."
        const val GET_IDENTIFIER_ROUTE_OPERATION_ID = "getManagementUserIdentifier"
        const val GET_IDENTIFIER_ROUTE_PATH_USER_ID_DESCRIPTION = "User id (UUID string, hex with dashes)"
        const val GET_IDENTIFIER_ROUTE_PATH_IDENTIFIER_ID_DESCRIPTION = "User identifier id (UUID string, hex with dashes)"
        const val GET_IDENTIFIER_ROUTE_RESPONSE_OK_DESCRIPTION = "User identifier"

        const val DELETE_IDENTIFIER_ROUTE_SUMMARY = "Delete user identifier (management)"
        const val DELETE_IDENTIFIER_ROUTE_DESCRIPTION = "Deletes a user identifier record for the given user."
        const val DELETE_IDENTIFIER_ROUTE_OPERATION_ID = "deleteManagementUserIdentifier"
        const val DELETE_IDENTIFIER_ROUTE_PATH_USER_ID_DESCRIPTION = "User id (UUID string, hex with dashes)"
        const val DELETE_IDENTIFIER_ROUTE_PATH_IDENTIFIER_ID_DESCRIPTION =
            "User identifier id (UUID string, hex with dashes)"
        const val DELETE_IDENTIFIER_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION = "Identifier removed; no response body."
    }
}