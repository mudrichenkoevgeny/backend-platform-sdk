package io.github.mudrichenkoevgeny.backend.feature.settingsapi.route.management

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.settingsapi.api.route.SettingsSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.settingsapi.api.usecase.management.globalsettings.UpdateGlobalSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.settingsapi.api.usecase.open.globalsettings.GetGlobalSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.mapper.globalsettings.toGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.mapper.globalsettings.toGlobalSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.model.globalsettings.GlobalSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.settingsapi.domain.audit.action.SettingsAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.settingsapi.domain.audit.resource.SettingsAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.settingsapi.domain.permission.SettingsPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.settingsapi.network.route.management.globalsettings.ManagementGlobalSettingsRoutes
import io.github.mudrichenkoevgeny.shared.foundation.feature.settingsapi.network.route.open.globalsettings.OpenGlobalSettingsRoutes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
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
 * 1. [ManagementGlobalSettingsRoutes.UPDATE_GLOBAL_SETTINGS] — updates settings via [UpdateGlobalSettingsUseCase].
 * 2. [OpenGlobalSettingsRoutes.GET_GLOBAL_SETTINGS] — retrieves settings via [GetGlobalSettingsUseCase].
 */
@Singleton
class ManagementGlobalSettingsRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val updateGlobalSettingsUseCase: UpdateGlobalSettingsUseCase,
    private val getGlobalSettingsUseCase: GetGlobalSettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerUpdateGlobalSettingsRoute(this)
        }
        registerGetGlobalSettingsRoute(route)
    }

    private fun registerUpdateGlobalSettingsRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)
        val requiredPermissions = setOf(SettingsPermissionCode.GLOBAL_SETTINGS_UPDATE)

        route.put(
            path = ManagementGlobalSettingsRoutes.UPDATE_GLOBAL_SETTINGS,
            builder = { updateGlobalSettingsDocs(allowedRoles, allowedAccountStatuses, requiredPermissions) },
            body = { updateGlobalSettings(allowedRoles, allowedAccountStatuses, requiredPermissions) }
        )
    }

    private fun registerGetGlobalSettingsRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.get(
            path = ManagementGlobalSettingsRoutes.GET_GLOBAL_SETTINGS,
            builder = { getGlobalSettingsDocs(allowedRoles, allowedAccountStatuses) },
            body = { getGlobalSettings() }
        )
    }

    private fun RouteConfig.updateGlobalSettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        summary = UPDATE_GLOBAL_SETTINGS_ROUTE_SUMMARY
        operationId = UPDATE_GLOBAL_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, SettingsSwaggerTags.GLOBAL_SETTINGS)

        description = getFormattedDescription(
            description = UPDATE_GLOBAL_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            requiredPermissions = requiredPermissions.mapToSet { it.value },
            isPublic = false
        )

        request {
            body<GlobalSettingsPayload>()
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

        val request = call.validateRequest<GlobalSettingsPayload>()

        val result = updateGlobalSettingsUseCase(
            globalSettings = request.toGlobalSettings(),
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
        tags = listOf(CommonSwaggerTags.OPEN, SettingsSwaggerTags.GLOBAL_SETTINGS)

        description = getFormattedDescription(
            description = GET_GLOBAL_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )

        response {
            code(HttpStatusCode.OK) {
                description = GET_GLOBAL_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getGlobalSettings() {
        val result = getGlobalSettingsUseCase()

        call.respondResult(result, appLogger, appErrorParser) { globalSettings ->
            globalSettings.toGlobalSettingsPayload()
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
        const val UPDATE_GLOBAL_SETTINGS_ROUTE_OPERATION_ID = "updateGlobalSettings"
        const val UPDATE_GLOBAL_SETTINGS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION =
            "Settings were updated successfully; no response body."

        const val GET_GLOBAL_SETTINGS_ROUTE_SUMMARY = "Get global settings"
        const val GET_GLOBAL_SETTINGS_ROUTE_DESCRIPTION = "Returns global system settings."
        const val GET_GLOBAL_SETTINGS_ROUTE_OPERATION_ID = "getGlobalSettings"
        const val GET_GLOBAL_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION = "Global settings data"
    }
}
