package io.github.mudrichenkoevgeny.backend.feature.user.route.management

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
import io.github.mudrichenkoevgeny.backend.feature.user.route.SettingsSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.globalsettings.GetManagementGlobalSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.globalsettings.ResetGlobalSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.globalsettings.UpdateGlobalSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.mapper.globalsettings.toManagementGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.mapper.globalsettings.toManagementGlobalSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.model.globalsettings.ManagementGlobalSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.audit.action.SettingsAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.audit.resource.SettingsAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.permission.SettingsPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.route.management.globalsettings.ManagementGlobalSettingsRoutes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
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
 * Router for administrative management of global system settings.
 *
 * Registered routes:
 * 1. [ManagementGlobalSettingsRoutes.UPDATE_MANAGEMENT_GLOBAL_SETTINGS] — updates settings via [UpdateGlobalSettingsUseCase].
 * 2. [ManagementGlobalSettingsRoutes.GET_MANAGEMENT_GLOBAL_SETTINGS] — retrieves settings via [GetManagementGlobalSettingsUseCase].
 * 3. [ManagementGlobalSettingsRoutes.RESET_MANAGEMENT_GLOBAL_SETTINGS] — resets settings via [ResetGlobalSettingsUseCase].
 */
@Singleton
class ManagementGlobalSettingsRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val updateGlobalSettingsUseCase: UpdateGlobalSettingsUseCase,
    private val getManagementGlobalSettingsUseCase: GetManagementGlobalSettingsUseCase,
    private val resetGlobalSettingsUseCase: ResetGlobalSettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerUpdateGlobalSettingsRoute(this)
            registerGetGlobalSettingsRoute(this)
            registerResetGlobalSettingsRoute(this)
        }
    }

    private fun registerUpdateGlobalSettingsRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)
        val requiredPermissions = setOf(SettingsPermissionCode.GLOBAL_SETTINGS_UPDATE)

        route.put(
            path = ManagementGlobalSettingsRoutes.UPDATE_MANAGEMENT_GLOBAL_SETTINGS,
            builder = { updateGlobalSettingsDocs(allowedRoles, allowedAccountStatuses, requiredPermissions) },
            body = { updateGlobalSettings(allowedRoles, allowedAccountStatuses, requiredPermissions) }
        )
    }

    private fun registerGetGlobalSettingsRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.get(
            path = ManagementGlobalSettingsRoutes.GET_MANAGEMENT_GLOBAL_SETTINGS,
            builder = { getGlobalSettingsDocs(allowedRoles, allowedAccountStatuses) },
            body = { getGlobalSettings(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.updateGlobalSettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        summary = UPDATE_GLOBAL_SETTINGS_ROUTE_SUMMARY
        operationId = UPDATE_GLOBAL_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + SettingsSwaggerTags.GLOBAL_SETTINGS)

        description = getFormattedDescription(
            description = UPDATE_GLOBAL_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            requiredPermissions = requiredPermissions.mapToSet { it.value },
            isPublic = false
        )

        request {
            body<ManagementGlobalSettingsPayload>()
        }
        response {
            code(HttpStatusCode.NoContent) {
                description = UPDATE_GLOBAL_SETTINGS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.updateGlobalSettings(
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
                actionType = SettingsAuditActionType.MANAGEMENT_UPDATE_GLOBAL_SETTINGS,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val request = call.validateRequest<ManagementGlobalSettingsPayload>()

        val result = updateGlobalSettingsUseCase(
            managementGlobalSettings = request.toManagementGlobalSettings(),
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun RouteConfig.getGlobalSettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_GLOBAL_SETTINGS_ROUTE_SUMMARY
        operationId = GET_GLOBAL_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + SettingsSwaggerTags.GLOBAL_SETTINGS)

        description = getFormattedDescription(
            description = GET_GLOBAL_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = false
        )

        response {
            code(HttpStatusCode.OK) {
                body<ManagementGlobalSettingsPayload>()
                description = GET_GLOBAL_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getGlobalSettings(
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

        val result = getManagementGlobalSettingsUseCase()

        call.respondResult(result, appLogger, appErrorParser) { globalSettings ->
            globalSettings.toManagementGlobalSettingsPayload()
        }
    }

    private fun registerResetGlobalSettingsRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)
        val requiredPermissions = setOf(SettingsPermissionCode.GLOBAL_SETTINGS_UPDATE)

        route.post(
            path = ManagementGlobalSettingsRoutes.RESET_MANAGEMENT_GLOBAL_SETTINGS,
            builder = { resetGlobalSettingsDocs(allowedRoles, allowedAccountStatuses, requiredPermissions) },
            body = { resetGlobalSettings(allowedRoles, allowedAccountStatuses, requiredPermissions) }
        )
    }

    private fun RouteConfig.resetGlobalSettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        summary = RESET_GLOBAL_SETTINGS_ROUTE_SUMMARY
        operationId = RESET_GLOBAL_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + SettingsSwaggerTags.GLOBAL_SETTINGS)

        description = getFormattedDescription(
            description = RESET_GLOBAL_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            requiredPermissions = requiredPermissions.mapToSet { it.value },
            isPublic = false
        )

        response {
            code(HttpStatusCode.OK) {
                body<ManagementGlobalSettingsPayload>()
                description = RESET_GLOBAL_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.resetGlobalSettings(
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
                actionType = SettingsAuditActionType.MANAGEMENT_RESET_GLOBAL_SETTINGS,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = resetGlobalSettingsUseCase(authenticatedRequestContext)

        call.respondResult(result, appLogger, appErrorParser) { globalSettings ->
            globalSettings.toManagementGlobalSettingsPayload()
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
            resource = SettingsAuditResourceType.GLOBAL_SETTINGS,
            status = AuditStatus.DENIED,
            metadata = metadata
        )
    }

    companion object {
        const val UPDATE_GLOBAL_SETTINGS_ROUTE_SUMMARY = "Update global settings"
        const val UPDATE_GLOBAL_SETTINGS_ROUTE_DESCRIPTION = "Replaces effective global settings with the payload."
        const val UPDATE_GLOBAL_SETTINGS_ROUTE_OPERATION_ID = "managementUpdateGlobalSettings"
        const val UPDATE_GLOBAL_SETTINGS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION =
            "Settings were updated successfully; no response body."

        const val GET_GLOBAL_SETTINGS_ROUTE_SUMMARY = "Get global settings"
        const val GET_GLOBAL_SETTINGS_ROUTE_DESCRIPTION = "Returns global system settings."
        const val GET_GLOBAL_SETTINGS_ROUTE_OPERATION_ID = "managementGetGlobalSettings"
        const val GET_GLOBAL_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION = "Global settings data"

        const val RESET_GLOBAL_SETTINGS_ROUTE_SUMMARY = "Reset global settings"
        const val RESET_GLOBAL_SETTINGS_ROUTE_DESCRIPTION = "Resets effective global settings to default values."
        const val RESET_GLOBAL_SETTINGS_ROUTE_OPERATION_ID = "managementResetGlobalSettings"
        const val RESET_GLOBAL_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION = "Restored global settings data"
    }
}