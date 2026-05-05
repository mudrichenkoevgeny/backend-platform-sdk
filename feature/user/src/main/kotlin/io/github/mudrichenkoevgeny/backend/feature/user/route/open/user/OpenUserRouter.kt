package io.github.mudrichenkoevgeny.backend.feature.user.route.open.user

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.model.SecurityRequirementType
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.ScheduleUserDeletionUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.GetUserUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.RestoreUserUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.user.toUserDetailsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.open.user.OpenUserRoutes
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
 * Router managing profile-level operations for the currently authenticated user.
 *
 * This component provides endpoints for self-service account management, including
 * retrieving profile details and managing the account lifecycle. It supports
 * scheduling account deletion and restoring accounts that are in the pending
 * deletion state. Sensitive lifecycle changes require step-up authentication.
 *
 * Registered routes:
 * 1. [OpenUserRoutes.GET_USER] — retrieves profile and account details via [GetUserUseCase].
 * 2. [OpenUserRoutes.SCHEDULE_DELETION] — initiates the account removal process via [ScheduleUserDeletionUseCase].
 * 3. [OpenUserRoutes.RESTORE_USER] — cancels a pending deletion and reactivates the account via [RestoreUserUseCase].
 */
@Singleton
class OpenUserRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val getUserUseCase: GetUserUseCase,
    private val scheduleUserDeletionUseCase: ScheduleUserDeletionUseCase,
    private val restoreUserUseCase: RestoreUserUseCase
) : BaseRouter {
    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerGetUserRoute(this)
            registerScheduleDeletionRoute(this)
            registerRestoreUserRoute(this)
        }
    }

    private fun registerGetUserRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.get(
            path = OpenUserRoutes.GET_USER,
            builder = { getUserDocs(allowedRoles, allowedAccountStatuses) },
            body = { getUser(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerScheduleDeletionRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = UserAccountStatus.entries.filter {
            it != UserAccountStatus.PENDING_DELETION
        }.toSet()

        route.delete(
            path = OpenUserRoutes.SCHEDULE_DELETION,
            builder = { scheduleDeletionDocs(allowedRoles, allowedAccountStatuses) },
            body = { scheduleDeletion(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerRestoreUserRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.PENDING_DELETION)

        route.post(
            path = OpenUserRoutes.RESTORE_USER,
            builder = { restoreUserDocs(allowedRoles, allowedAccountStatuses) },
            body = { restoreUser(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.getUserDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_USER_ROUTE_SUMMARY
        operationId = GET_USER_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.USER)

        description = getFormattedDescription(
            description = GET_USER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = false
        )

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

        val result = getUserUseCase(
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { userDetails ->
            userDetails.toUserDetailsPayload()
        }
    }

    private fun RouteConfig.scheduleDeletionDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = SCHEDULE_DELETION_ROUTE_SUMMARY
        operationId = SCHEDULE_DELETION_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.USER)

        description = getFormattedDescription(
            description = SCHEDULE_DELETION_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            securityType = SecurityRequirementType.SENSITIVE_STEP_UP
        )

        response {
            code(HttpStatusCode.OK) {
                description = SCHEDULE_DELETION_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.scheduleDeletion(
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
                actionType = UserAuditActionType.SELF_SCHEDULE_USER_DELETION,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = scheduleUserDeletionUseCase(
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { userDetails ->
            userDetails.toUserDetailsPayload()
        }
    }

    private fun RouteConfig.restoreUserDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = RESTORE_USER_ROUTE_SUMMARY
        operationId = RESTORE_USER_ROUTE_OPERATION_ID
        tags = listOf(UserSwaggerTags.USER)

        description = getFormattedDescription(
            description = RESTORE_USER_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            securityType = SecurityRequirementType.SENSITIVE_STEP_UP
        )

        response {
            code(HttpStatusCode.OK) {
                description = RESTORE_USER_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.restoreUser(
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
                actionType = UserAuditActionType.SELF_RESTORE_USER,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = restoreUserUseCase(
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { userDetails ->
            userDetails.toUserDetailsPayload()
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
            actorUserRole = authenticatedRequestContext.userRole.serialName,
            action = actionType,
            resource = UserAuditResourceType.USER,
            resourceId = authenticatedRequestContext.userId.asHexDashString(),
            status = AuditStatus.DENIED,
            metadata = metadata
        )
    }

    companion object {
        const val GET_USER_ROUTE_SUMMARY = "Get current user"
        const val GET_USER_ROUTE_DESCRIPTION = "Returns information about the currently authenticated user."
        const val GET_USER_ROUTE_OPERATION_ID = "getUser"
        const val GET_USER_ROUTE_RESPONSE_OK_DESCRIPTION = "User data retrieved successfully"

        const val SCHEDULE_DELETION_ROUTE_SUMMARY = "Schedule account deletion"
        const val SCHEDULE_DELETION_ROUTE_DESCRIPTION = "Schedules the currently authenticated user account for permanent deletion."
        const val SCHEDULE_DELETION_ROUTE_OPERATION_ID = "scheduleDeletion"
        const val SCHEDULE_DELETION_ROUTE_RESPONSE_OK_DESCRIPTION = "User account scheduled for deletion"

        const val RESTORE_USER_ROUTE_SUMMARY = "Restore user account"
        const val RESTORE_USER_ROUTE_DESCRIPTION = "Restores an account that was previously scheduled for deletion."
        const val RESTORE_USER_ROUTE_OPERATION_ID = "restoreUser"
        const val RESTORE_USER_ROUTE_RESPONSE_OK_DESCRIPTION = "User account restored successfully"
    }
}