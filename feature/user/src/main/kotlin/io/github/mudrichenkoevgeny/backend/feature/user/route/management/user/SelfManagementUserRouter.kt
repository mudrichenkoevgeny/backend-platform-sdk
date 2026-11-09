package io.github.mudrichenkoevgeny.backend.feature.user.route.management.user

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.mapper.user.toUpdateUserParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.UpdateSelfUserUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.user.GetUserUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.user.toUserPrivatePayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.user.UserPrivatePayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.user.SelfUpdateUserRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.management.self.user.SelfManagementUserRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.patch
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management HTTP routes for self-service user profile operations.
 *
 * Registered routes:
 * 1. [SelfManagementUserRoutes.GET_USER] — retrieves detailed information about the currently authenticated user via
 * [GetUserUseCase].
 */
@Singleton
public class SelfManagementUserRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val getUserUseCase: GetUserUseCase,
    private val updateSelfUserUseCase: UpdateSelfUserUseCase
) : BaseRouter {
    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerGetUserRoute(this)
            registerUpdateUserRoute(this)
        }
    }

    private fun registerGetUserRoute(route: Route) {
        val allowedRoles = setOf(
            UserRole.STAFF,
            UserRole.ADMIN
        )
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.get(
            path = SelfManagementUserRoutes.GET_USER,
            builder = {
                getUserDocs(
                    allowedRoles,
                    allowedAccountStatuses
                )
            },
            body = {
                getUser(
                    allowedRoles,
                    allowedAccountStatuses
                )
            }
        )
    }

    private fun registerUpdateUserRoute(route: Route) {
        val allowedRoles = setOf(
            UserRole.STAFF,
            UserRole.ADMIN
        )
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.patch(
            path = SelfManagementUserRoutes.UPDATE_USER,
            builder = {
                updateUserDocs(
                    allowedRoles,
                    allowedAccountStatuses
                )
            },
            body = {
                updateUser(
                    allowedRoles,
                    allowedAccountStatuses
                )
            }
        )
    }

    private fun RouteConfig.getUserDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_SELF_USER_ROUTE_SUMMARY
        operationId = GET_SELF_USER_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + UserSwaggerTags.USER)

        description = getFormattedDescription(
            description = GET_SELF_USER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = false
        )

        response {
            code(HttpStatusCode.OK) {
                body<UserPrivatePayload>()
                description = GET_SELF_USER_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getUser(
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
            call.respondResult(
                authorizeResult,
                appLogger,
                appErrorParser
            )
            return
        }

        val result = getUserUseCase(
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(
            result,
            appLogger,
            appErrorParser
        ) { userPrivate ->
            userPrivate.toUserPrivatePayload()
        }
    }

    private fun RouteConfig.updateUserDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = UPDATE_SELF_USER_ROUTE_SUMMARY
        operationId = UPDATE_SELF_USER_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + UserSwaggerTags.USER)

        description = getFormattedDescription(
            description = UPDATE_SELF_USER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = false
        )

        request {
            body<SelfUpdateUserRequest>()
        }

        response {
            code(HttpStatusCode.OK) {
                body<UserPrivatePayload>()
                description = UPDATE_SELF_USER_ROUTE_RESPONSE_OK_DESCRIPTION
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
                actionType = UserAuditActionType.SELF_UPDATE_USER,
                error = authorizeResult.error
            )
            call.respondResult(
                authorizeResult,
                appLogger,
                appErrorParser
            )
            return
        }

        val selfUpdateUserRequest = call.validateRequest<SelfUpdateUserRequest>()
        val updateUserParams = selfUpdateUserRequest.toUpdateUserParams()

        val result = updateSelfUserUseCase(
            updateUserParams = updateUserParams,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(
            result,
            appLogger,
            appErrorParser
        ) { userPrivate ->
            userPrivate.toUserPrivatePayload()
        }
    }

    private fun logErrorToAudit(
        authenticatedRequestContext: AuthenticatedRequestContext,
        actionType: AuditActionType,
        error: AppError,
        extraMetadata: Set<AuditEventMetadata> = emptySet()
    ) {
        val errorData = auditErrorConverter.convert(error)
        val metadata = authenticatedRequestContext.clientInfo.toAuditMetadata() + errorData.metadata + extraMetadata

        auditLogger.log(
            actorId = authenticatedRequestContext.userId.asHexDashString(),
            actorType = AuditActorType.USER,
            actorUserRole = authenticatedRequestContext.userRole.value,
            action = actionType,
            resource = UserAuditResourceType.USER,
            resourceId = authenticatedRequestContext.userId.asHexDashString(),
            status = AuditStatus.DENIED,
            metadata = metadata
        )
    }

    public companion object {
        private const val GET_SELF_USER_ROUTE_SUMMARY = "Get current user profile (management)"
        private const val GET_SELF_USER_ROUTE_DESCRIPTION =
            "Returns the profile details of the currently authenticated management user."
        private const val GET_SELF_USER_ROUTE_OPERATION_ID = "getSelfManagementUser"
        private const val GET_SELF_USER_ROUTE_RESPONSE_OK_DESCRIPTION = "User profile data."

        private const val UPDATE_SELF_USER_ROUTE_SUMMARY = "Update current management user profile"
        private const val UPDATE_SELF_USER_ROUTE_DESCRIPTION =
            "Partially updates account details and profile fields of the currently authenticated staff member."
        private const val UPDATE_SELF_USER_ROUTE_OPERATION_ID = "updateSelfManagementUser"
        private const val UPDATE_SELF_USER_ROUTE_RESPONSE_OK_DESCRIPTION = "Updated user profile data."
    }
}
