package io.github.mudrichenkoevgeny.backend.feature.user.route.management.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.pagination.mapItems
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validatePathParameter
import io.github.mudrichenkoevgeny.backend.feature.user.audit.toDeniedUserAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.network.query.parseIdentifiersListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier.ManagementDeleteUserIdentifierUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier.ManagementGetUserIdentifierUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier.ManagementGetUserIdentifiersUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.toUserIdentifierIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.toUserIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.identifier.toUserIdentifierPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiPaths
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.management.identifier.ManagementIdentifierRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management HTTP routes for listing, reading, and deleting user identifiers.
 *
 * Fine-grained [io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.IdentifierPermissionCode]
 * checks are expected inside use cases once implemented; the router only requires an active staff or admin principal.
 */
@Singleton
class ManagementIdentifierRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val managementGetUserIdentifiersUseCase: ManagementGetUserIdentifiersUseCase,
    private val managementGetUserIdentifierUseCase: ManagementGetUserIdentifierUseCase,
    private val managementDeleteUserIdentifierUseCase: ManagementDeleteUserIdentifierUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.get(
            path = ManagementIdentifierRoutes.GET_IDENTIFIERS,
            builder = { getIdentifiersDocs() },
            body = { getIdentifiers() }
        )
        route.get(
            path = ManagementIdentifierRoutes.GET_IDENTIFIER,
            builder = { getIdentifierDocs() },
            body = { getIdentifier() }
        )
        route.delete(
            path = ManagementIdentifierRoutes.DELETE_IDENTIFIER,
            builder = { deleteIdentifierDocs() },
            body = { deleteIdentifier() }
        )
    }

    private fun RouteConfig.getIdentifiersDocs() {
        summary = GET_IDENTIFIERS_ROUTE_SUMMARY
        description = GET_IDENTIFIERS_ROUTE_DESCRIPTION
        operationId = GET_IDENTIFIERS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.IDENTIFIER)
        response {
            code(HttpStatusCode.OK) {
                description = GET_IDENTIFIERS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getIdentifiers() {
        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN),
            requiredAccountStatus = setOf(UserAccountStatus.ACTIVE)
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val queryParams = call.parseIdentifiersListQueryParams()

        val result = managementGetUserIdentifiersUseCase(
            authenticatedRequestContext = authenticatedRequestContext,
            pageParams = queryParams.listing.pageParams,
            sortBy = queryParams.listing.sortBy,
            sortOrder = queryParams.listing.sortOrder,
            userIds = queryParams.userIds,
            userAuthProviders = queryParams.userAuthProviders,
            identifiers = queryParams.identifiers
        )

        call.respondResult(result, appLogger, appErrorParser) { paged ->
            paged.mapItems { it.toUserIdentifierPayload() }
        }
    }

    private fun RouteConfig.getIdentifierDocs() {
        summary = GET_IDENTIFIER_ROUTE_SUMMARY
        description = GET_IDENTIFIER_ROUTE_DESCRIPTION
        operationId = GET_IDENTIFIER_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.IDENTIFIER)
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

    private suspend fun RoutingContext.getIdentifier() {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN),
            requiredAccountStatus = setOf(UserAccountStatus.ACTIVE)
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val userId = call.validatePathParameter(UserApiPaths.USER_ID) { userId ->
            userId.toUserIdOrThrow()
        }
        val userIdentifierId = call.validatePathParameter(UserApiPaths.USER_IDENTIFIER_ID) { userIdentifierId ->
            userIdentifierId.toUserIdentifierIdOrThrow()
        }

        val result = managementGetUserIdentifierUseCase(
            userId = userId,
            userIdentifierId = userIdentifierId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { internal ->
            internal.toUserIdentifierPayload()
        }
    }

    private fun RouteConfig.deleteIdentifierDocs() {
        summary = DELETE_IDENTIFIER_ROUTE_SUMMARY
        description = DELETE_IDENTIFIER_ROUTE_DESCRIPTION
        operationId = DELETE_IDENTIFIER_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.IDENTIFIER)
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

    private suspend fun RoutingContext.deleteIdentifier() {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN),
            requiredAccountStatus = setOf(UserAccountStatus.ACTIVE)
        )

        if (authorizeResult is AppResult.Error) {
            val metadata: Set<AuditEventMetadata> = authorizeResult.error.toDeniedUserAuditEventMetadata() +
                    authenticatedRequestContext.clientInfo.toAuditMetadata()
            auditLogger.log(
                actorId = authenticatedRequestContext.userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = authenticatedRequestContext.userRole.serialName,
                action = UserAuditActionType.MANAGEMENT_DELETE_IDENTIFIER,
                resource = UserAuditResourceType.USER_IDENTIFIER,
                status = AuditStatus.DENIED,
                metadata = metadata
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val userId = call.validatePathParameter(UserApiPaths.USER_ID) { userId ->
            userId.toUserIdOrThrow()
        }
        val userIdentifierId = call.validatePathParameter(UserApiPaths.USER_IDENTIFIER_ID) { userIdentifierId ->
            userIdentifierId.toUserIdentifierIdOrThrow()
        }

        val result = managementDeleteUserIdentifierUseCase(
            userId = userId,
            userIdentifierId = userIdentifierId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
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