package io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.settings

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auth.settings.GetManagementAuthSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auth.settings.ResetAuthSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auth.settings.UpdateAuthSettingsUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.AuthSettingsPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.settings.toManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.settings.toManagementAuthSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.auth.settings.ManagementAuthSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.management.auth.settings.ManagementAuthSettingsRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management HTTP routes for reading, updating, and resetting authentication security policies.
 *
 * Registered routes:
 * 1. [ManagementAuthSettingsRoutes.GET_MANAGEMENT_AUTH_SETTINGS] — retrieves configuration via [GetManagementAuthSettingsUseCase].
 * 2. [ManagementAuthSettingsRoutes.UPDATE_MANAGEMENT_AUTH_SETTINGS] — updates security policies via [UpdateAuthSettingsUseCase].
 * 3. [ManagementAuthSettingsRoutes.RESET_MANAGEMENT_AUTH_SETTINGS] — resets security policies via [ResetAuthSettingsUseCase].
 */
@Singleton
class ManagementAuthSettingsRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val getManagementAuthSettingsUseCase: GetManagementAuthSettingsUseCase,
    private val updateAuthSettingsUseCase: UpdateAuthSettingsUseCase,
    private val resetAuthSettingsUseCase: ResetAuthSettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerGetAuthSettingsManagementRoute(this)
            registerUpdateAuthSettingsRoute(this)
            registerResetAuthSettingsRoute(this)
        }
    }

    private fun registerGetAuthSettingsManagementRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.get(
            path = ManagementAuthSettingsRoutes.GET_MANAGEMENT_AUTH_SETTINGS,
            builder = { getAuthSettingsManagementDocs(allowedRoles, allowedAccountStatuses) },
            body = { getAuthSettingsManagement(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerUpdateAuthSettingsRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)
        val requiredPermissions = setOf(AuthSettingsPermissionCode.AUTH_SETTINGS_UPDATE)

        route.put(
            path = ManagementAuthSettingsRoutes.UPDATE_MANAGEMENT_AUTH_SETTINGS,
            builder = { updateAuthSettingsDocs(allowedRoles, allowedAccountStatuses, requiredPermissions) },
            body = { updateAuthSettings(allowedRoles, allowedAccountStatuses, requiredPermissions) }
        )
    }

    private fun RouteConfig.getAuthSettingsManagementDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_AUTH_SETTINGS_MANAGEMENT_ROUTE_SUMMARY
        operationId = GET_AUTH_SETTINGS_MANAGEMENT_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + UserSwaggerTags.AUTH_SETTINGS)

        description = getFormattedDescription(
            description = GET_AUTH_SETTINGS_MANAGEMENT_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = false
        )

        response {
            code(HttpStatusCode.OK) {
                body<ManagementAuthSettingsPayload>()
                description = GET_AUTH_SETTINGS_MANAGEMENT_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getAuthSettingsManagement(
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

        val result = getManagementAuthSettingsUseCase()
        call.respondResult(result, appLogger, appErrorParser) { management ->
            management.toManagementAuthSettingsPayload()
        }
    }

    private fun RouteConfig.updateAuthSettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        summary = UPDATE_AUTH_SETTINGS_ROUTE_SUMMARY
        operationId = UPDATE_AUTH_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + UserSwaggerTags.AUTH_SETTINGS)

        description = getFormattedDescription(
            description = UPDATE_AUTH_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            requiredPermissions = requiredPermissions.mapToSet { it.value },
            isPublic = false
        )

        request {
            body<ManagementAuthSettingsPayload>()
        }
        response {
            code(HttpStatusCode.NoContent) {
                description = UPDATE_AUTH_SETTINGS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.updateAuthSettings(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses,
            requiredPermissions = requiredPermissions
        )

        if (authorizeResult is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = UserAuditActionType.MANAGEMENT_UPDATE_AUTH_SETTINGS,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val request = call.validateRequest<ManagementAuthSettingsPayload>()
        val result = updateAuthSettingsUseCase(
            managementAuthSettings = request.toManagementAuthSettings(),
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun registerResetAuthSettingsRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)
        val requiredPermissions = setOf(AuthSettingsPermissionCode.AUTH_SETTINGS_UPDATE)

        route.post(
            path = ManagementAuthSettingsRoutes.RESET_MANAGEMENT_AUTH_SETTINGS,
            builder = { resetAuthSettingsDocs(allowedRoles, allowedAccountStatuses, requiredPermissions) },
            body = { resetAuthSettings(allowedRoles, allowedAccountStatuses, requiredPermissions) }
        )
    }

    private fun RouteConfig.resetAuthSettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        summary = RESET_AUTH_SETTINGS_ROUTE_SUMMARY
        operationId = RESET_AUTH_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + UserSwaggerTags.AUTH_SETTINGS)

        description = getFormattedDescription(
            description = RESET_AUTH_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            requiredPermissions = requiredPermissions.mapToSet { it.value },
            isPublic = false
        )

        response {
            code(HttpStatusCode.OK) {
                body<ManagementAuthSettingsPayload>()
                description = RESET_AUTH_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.resetAuthSettings(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses,
            requiredPermissions = requiredPermissions
        )

        if (authorizeResult is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = UserAuditActionType.MANAGEMENT_RESET_AUTH_SETTINGS,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = resetAuthSettingsUseCase(authenticatedRequestContext)

        call.respondResult(result, appLogger, appErrorParser) { management ->
            management.toManagementAuthSettingsPayload()
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
            resource = UserAuditResourceType.AUTH_SETTINGS,
            status = AuditStatus.DENIED,
            metadata = metadata
        )
    }

    companion object {
        const val GET_AUTH_SETTINGS_MANAGEMENT_ROUTE_SUMMARY = "Get management auth settings"
        const val GET_AUTH_SETTINGS_MANAGEMENT_ROUTE_DESCRIPTION =
            "Returns management auth settings including token validity values and available providers."
        const val GET_AUTH_SETTINGS_MANAGEMENT_ROUTE_OPERATION_ID = "getManagementAuthSettings"
        const val GET_AUTH_SETTINGS_MANAGEMENT_ROUTE_RESPONSE_OK_DESCRIPTION =
            "Management auth settings data."
        const val UPDATE_AUTH_SETTINGS_ROUTE_SUMMARY = "Update auth settings"
        const val UPDATE_AUTH_SETTINGS_ROUTE_DESCRIPTION =
            "Replaces management auth settings with the provided payload."
        const val UPDATE_AUTH_SETTINGS_ROUTE_OPERATION_ID = "updateManagementAuthSettings"
        const val UPDATE_AUTH_SETTINGS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION =
            "Settings were updated successfully; no response body."

        const val RESET_AUTH_SETTINGS_ROUTE_SUMMARY = "Reset auth settings"
        const val RESET_AUTH_SETTINGS_ROUTE_DESCRIPTION =
            "Resets global authentication settings to default values."
        const val RESET_AUTH_SETTINGS_ROUTE_OPERATION_ID = "resetManagementAuthSettings"
        const val RESET_AUTH_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION =
            "Management auth settings restored to default values."
    }
}