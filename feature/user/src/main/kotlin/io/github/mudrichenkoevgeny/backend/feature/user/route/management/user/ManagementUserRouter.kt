package io.github.mudrichenkoevgeny.backend.feature.user.route.management.user

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateFieldValue
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validatePathParameter
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.pagination.mapItems
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.query.parseUsersListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user.ManagementCreateUserUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user.ManagementDeleteUserUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user.ManagementGetUserUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user.ManagementGetUsersUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user.ManagementUpdateUserUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.toUserIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.user.toUserDetailsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiFields
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiPaths
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.create.CreateByEmailRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.user.UpdateUserRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.management.user.ManagementUserRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.patch
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management HTTP routes for administrative user account operations.
 *
 * Registered routes:
 * 1. [ManagementUserRoutes.GET_USER] — retrieves detailed information for a specific user via [ManagementGetUserUseCase].
 * 2. [ManagementUserRoutes.GET_USERS] — retrieves a paginated and filtered list of users via [ManagementGetUsersUseCase].
 * 3. [ManagementUserRoutes.CREATE_USER] — creates a new user account with administrative overrides via [ManagementCreateUserUseCase].
 * 4. [ManagementUserRoutes.UPDATE_USER] — performs partial updates on user status, authority, or permissions via [ManagementUpdateUserUseCase].
 * 5. [ManagementUserRoutes.DELETE_USER] — administratively removes a user account via [ManagementDeleteUserUseCase].
 */
@Singleton
class ManagementUserRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val managementGetUserUseCase: ManagementGetUserUseCase,
    private val managementGetUsersUseCase: ManagementGetUsersUseCase,
    private val managementCreateUserUseCase: ManagementCreateUserUseCase,
    private val managementUpdateUserUseCase: ManagementUpdateUserUseCase,
    private val managementDeleteUserUseCase: ManagementDeleteUserUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerGetUserRoute(this)
            registerGetUsersRoute(this)
            registerCreateUserRoute(this)
            registerUpdateUserRoute(this)
            registerDeleteUserRoute(this)
        }
    }

    private fun registerGetUserRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.get(
            path = ManagementUserRoutes.GET_USER,
            builder = { getUserDocs(allowedRoles, allowedAccountStatuses) },
            body = { getUser(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerGetUsersRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.get(
            path = ManagementUserRoutes.GET_USERS,
            builder = { getUsersDocs(allowedRoles, allowedAccountStatuses) },
            body = { getUsers(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerCreateUserRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.post(
            path = ManagementUserRoutes.CREATE_USER,
            builder = { createUserDocs(allowedRoles, allowedAccountStatuses) },
            body = { createUser(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerUpdateUserRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.patch(
            path = ManagementUserRoutes.UPDATE_USER,
            builder = { updateUserDocs(allowedRoles, allowedAccountStatuses) },
            body = { updateUser(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerDeleteUserRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.delete(
            path = ManagementUserRoutes.DELETE_USER,
            builder = { deleteUserDocs(allowedRoles, allowedAccountStatuses) },
            body = { deleteUser(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.getUserDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_USER_ROUTE_SUMMARY
        operationId = GET_USER_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.USER)
        description = getFormattedDescription(
            description = GET_USER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request {
            pathParameter<String>(UserApiPaths.USER_ID) {
                description = GET_USER_ROUTE_PATH_USER_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = GET_USER_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getUser(
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
        val userId = call.validatePathParameter(UserApiPaths.USER_ID) { userId ->
            userId.toUserIdOrThrow()
        }

        val result = managementGetUserUseCase(
            userId = userId,
            authenticatedRequestContext = authenticatedRequestContext
        )
        call.respondResult(result, appLogger, appErrorParser) { userDetails ->
            userDetails.toUserDetailsPayload()
        }
    }

    private fun RouteConfig.getUsersDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_USERS_ROUTE_SUMMARY
        operationId = GET_USERS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.USER)
        description = getFormattedDescription(
            description = GET_USERS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        response {
            code(HttpStatusCode.OK) {
                description = GET_USERS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getUsers(
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
        val queryParams = call.parseUsersListQueryParams()

        val result = managementGetUsersUseCase(
            pageParams = queryParams.listing.pageParams,
            sortBy = queryParams.listing.sortBy,
            sortOrder = queryParams.listing.sortOrder,
            roles = queryParams.roles,
            accountStatuses = queryParams.accountStatuses,
            accountStatusesBeforeDeletion = queryParams.accountStatusesBeforeDeletion,
            authorityLevelFrom = queryParams.authorityLevelFrom,
            authorityLevelTo = queryParams.authorityLevelTo,
            requiredPermissionCodes = queryParams.requiredPermissionCodes,
            isTotpEnabled = queryParams.isTotpEnabled,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { pagedUsers ->
            pagedUsers.mapItems { userDetails -> userDetails.toUserDetailsPayload() }
        }
    }

    private fun RouteConfig.createUserDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = CREATE_USER_ROUTE_SUMMARY
        operationId = CREATE_USER_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.USER)
        description = getFormattedDescription(
            description = CREATE_USER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        response {
            code(HttpStatusCode.Created) {
                description = CREATE_USER_ROUTE_RESPONSE_CREATED_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.createUser(
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
                actionType = UserAuditActionType.MANAGEMENT_CREATE_USER,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val request = call.validateRequest<CreateByEmailRequest>()

        val userRole = request.role.validateFieldValue(
            fieldName = UserApiFields.ROLE,
            parser = UserRole::fromValueOrNull
        )

        val accountStatus = request.status.validateFieldValue(
            fieldName = UserApiFields.ACCOUNT_STATUS,
            parser = UserAccountStatus::fromValueOrNull
        )

        val result = managementCreateUserUseCase(
            email = request.email,
            password = request.password,
            role = userRole,
            accountStatus = accountStatus,
            authorityLevel = request.authorityLevel,
            permissionCodes = request.permissionCodes.map { permissionCode ->
                PermissionCode(permissionCode)
            }.toSet(),
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { userDetails ->
            userDetails.toUserDetailsPayload()
        }
    }

    private fun RouteConfig.updateUserDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = UPDATE_USER_ROUTE_SUMMARY
        operationId = UPDATE_USER_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.USER)
        description = getFormattedDescription(
            description = UPDATE_USER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request {
            pathParameter<String>(UserApiPaths.USER_ID) {
                description = UPDATE_USER_ROUTE_PATH_USER_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = UPDATE_USER_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.updateUser(
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
                actionType = UserAuditActionType.MANAGEMENT_UPDATE_USER,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val userId = call.validatePathParameter(UserApiPaths.USER_ID) { userId ->
            userId.toUserIdOrThrow()
        }

        val request = call.validateRequest<UpdateUserRequest>()

        val accountStatus = request.accountStatus?.validateFieldValue(
            fieldName = UserApiFields.ACCOUNT_STATUS,
            parser = UserAccountStatus::fromValueOrNull
        )

        val result = managementUpdateUserUseCase(
            userId = userId,
            accountStatus = accountStatus,
            authorityLevel = request.authorityLevel,
            permissionCodes = request.permissionCodes?.map { permissionCode ->
                PermissionCode(permissionCode)
            }?.toSet(),
            authenticatedRequestContext = authenticatedRequestContext
        )
        call.respondResult(result, appLogger, appErrorParser) { userDetails ->
            userDetails.toUserDetailsPayload()
        }
    }

    private fun RouteConfig.deleteUserDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = DELETE_USER_ROUTE_SUMMARY
        operationId = DELETE_USER_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.USER)
        description = getFormattedDescription(
            description = DELETE_USER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName }
        )
        request {
            pathParameter<String>(UserApiPaths.USER_ID) {
                description = DELETE_USER_ROUTE_PATH_USER_ID_DESCRIPTION
            }
        }
        response {
            code(HttpStatusCode.NoContent) {
                description = DELETE_USER_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.deleteUser(
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
                actionType = UserAuditActionType.MANAGEMENT_DELETE_USER,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val userId = call.validatePathParameter(UserApiPaths.USER_ID) { userId ->
            userId.toUserIdOrThrow()
        }

        val result = managementDeleteUserUseCase(
            userId = userId,
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
            resource = UserAuditResourceType.USER,
            status = AuditStatus.DENIED,
            metadata = metadata
        )
    }

    companion object {
        const val GET_USER_ROUTE_SUMMARY = "Get user (management)"
        const val GET_USER_ROUTE_DESCRIPTION = "Returns the user for the given identifier."
        const val GET_USER_ROUTE_OPERATION_ID = "getManagementUser"
        const val GET_USER_ROUTE_PATH_USER_ID_DESCRIPTION = "User id (UUID string, hex with dashes)"
        const val GET_USER_ROUTE_RESPONSE_OK_DESCRIPTION = "User details"

        const val GET_USERS_ROUTE_SUMMARY = "List users (management)"
        const val GET_USERS_ROUTE_DESCRIPTION = "Returns a paginated list of users. Access is restricted based " +
                "on the caller's permissions and target roles."
        const val GET_USERS_ROUTE_OPERATION_ID = "getManagementUsers"
        const val GET_USERS_ROUTE_RESPONSE_OK_DESCRIPTION = "Paged user details"

        const val CREATE_USER_ROUTE_SUMMARY = "Create user (management)"
        const val CREATE_USER_ROUTE_DESCRIPTION = "Creates a new user account with specified role and permissions."
        const val CREATE_USER_ROUTE_OPERATION_ID = "createManagementUser"
        const val CREATE_USER_ROUTE_RESPONSE_CREATED_DESCRIPTION = "User created successfully"

        const val UPDATE_USER_ROUTE_SUMMARY = "Update user (management)"
        const val UPDATE_USER_ROUTE_DESCRIPTION = "Partially updates user's account status and/or permissions."
        const val UPDATE_USER_ROUTE_OPERATION_ID = "updateManagementUser"
        const val UPDATE_USER_ROUTE_PATH_USER_ID_DESCRIPTION = "User id (UUID string, hex with dashes)"
        const val UPDATE_USER_ROUTE_RESPONSE_OK_DESCRIPTION = "User updated successfully"

        const val DELETE_USER_ROUTE_SUMMARY = "Delete user (management)"
        const val DELETE_USER_ROUTE_DESCRIPTION = "Deletes the user account for the given identifier."
        const val DELETE_USER_ROUTE_OPERATION_ID = "deleteManagementUser"
        const val DELETE_USER_ROUTE_PATH_USER_ID_DESCRIPTION = "User id (UUID string, hex with dashes)"
        const val DELETE_USER_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION = "User removed; no response body."
    }
}