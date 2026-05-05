package io.github.mudrichenkoevgeny.backend.feature.security.api.route.management

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
import io.github.mudrichenkoevgeny.backend.feature.security.api.route.SecuritySwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.security.api.usecase.management.settings.UpdateSecuritySettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.security.api.usecase.open.settings.GetSecuritySettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.securitysettings.toSecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.securitysettings.toSecuritySettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.securitysettings.SecuritySettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.security.api.domain.audit.action.SecurityAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.security.api.domain.audit.resource.SecurityAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.security.api.domain.permission.SecurityPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.security.api.network.route.management.security.settings.ManagementSecuritySettingsRoutes
import io.github.mudrichenkoevgeny.shared.foundation.feature.security.api.network.route.open.security.settings.OpenSecuritySettingsRoutes
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
 * Router for administrative management of security-related system settings.
 *
 * Registered routes:
 * 1. [ManagementSecuritySettingsRoutes.UPDATE_SECURITY_SETTINGS] — updates settings via [UpdateSecuritySettingsUseCase].
 * 2. [OpenSecuritySettingsRoutes.GET_SECURITY_SETTINGS] — retrieves settings via [GetSecuritySettingsUseCase].
 */
@Singleton
class ManagementSecuritySettingsRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val updateSecuritySettingsUseCase: UpdateSecuritySettingsUseCase,
    private val getSecuritySettingsUseCase: GetSecuritySettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerUpdateSecuritySettingsRoute(this)
        }
        registerGetSecuritySettingsRoute(route)
    }

    private fun registerUpdateSecuritySettingsRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)
        val requiredPermissions = setOf(SecurityPermissionCode.SECURITY_SETTINGS_UPDATE)

        route.put(
            path = ManagementSecuritySettingsRoutes.UPDATE_SECURITY_SETTINGS,
            builder = { updateSecuritySettingsDocs(allowedRoles, allowedAccountStatuses, requiredPermissions) },
            body = { updateSecuritySettings(allowedRoles, allowedAccountStatuses, requiredPermissions) }
        )
    }

    private fun registerGetSecuritySettingsRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.get(
            path = ManagementSecuritySettingsRoutes.GET_SECURITY_SETTINGS,
            builder = { getSecuritySettingsDocs(allowedRoles, allowedAccountStatuses) },
            body = { getSecuritySettings() }
        )
    }

    private fun RouteConfig.updateSecuritySettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        summary = UPDATE_SECURITY_SETTINGS_ROUTE_SUMMARY
        operationId = UPDATE_SECURITY_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, SecuritySwaggerTags.SECURITY_SETTINGS)

        description = getFormattedDescription(
            description = UPDATE_SECURITY_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            requiredPermissions = requiredPermissions.mapToSet { it.value },
            isPublic = false
        )

        request {
            body<SecuritySettingsPayload>()
        }
        response {
            code(HttpStatusCode.NoContent) {
                description = UPDATE_SECURITY_SETTINGS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.updateSecuritySettings(
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
                actionType = SecurityAuditActionType.MANAGEMENT_UPDATE_SECURITY_SETTINGS,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val request = call.validateRequest<SecuritySettingsPayload>()

        val result = updateSecuritySettingsUseCase(
            securitySettings = request.toSecuritySettings(),
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun RouteConfig.getSecuritySettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_SECURITY_SETTINGS_ROUTE_SUMMARY
        operationId = GET_SECURITY_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN, SecuritySwaggerTags.SECURITY_SETTINGS)

        description = getFormattedDescription(
            description = GET_SECURITY_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = true
        )

        response {
            code(HttpStatusCode.OK) {
                description = GET_SECURITY_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getSecuritySettings() {
        val result = getSecuritySettingsUseCase()

        call.respondResult(result, appLogger, appErrorParser) { securitySettings ->
            securitySettings.toSecuritySettingsPayload()
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
            resource = SecurityAuditResourceType.SECURITY_SETTINGS,
            status = AuditStatus.DENIED,
            metadata = metadata
        )
    }

    companion object {
        const val UPDATE_SECURITY_SETTINGS_ROUTE_SUMMARY = "Update security settings"
        const val UPDATE_SECURITY_SETTINGS_ROUTE_DESCRIPTION =
            "Replaces effective security settings with the payload."
        const val UPDATE_SECURITY_SETTINGS_ROUTE_OPERATION_ID = "updateSecuritySettings"
        const val UPDATE_SECURITY_SETTINGS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION =
            "Settings were updated successfully; no response body."

        const val GET_SECURITY_SETTINGS_ROUTE_SUMMARY = "Get security settings"
        const val GET_SECURITY_SETTINGS_ROUTE_DESCRIPTION = "Returns security settings."
        const val GET_SECURITY_SETTINGS_ROUTE_OPERATION_ID = "getSecuritySettings"
        const val GET_SECURITY_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION = "Security settings data"
    }
}
