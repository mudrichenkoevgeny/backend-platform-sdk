package io.github.mudrichenkoevgeny.backend.feature.security.api.route.management

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.validation.validateRequest
import io.github.mudrichenkoevgeny.backend.feature.security.api.route.SecuritySwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.security.api.usecase.management.settings.UpdateSecuritySettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.audit.toDeniedAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.securitysettings.toSecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.securitysettings.SecuritySettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.security.api.domain.audit.action.SecurityAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.security.api.domain.audit.resource.SecurityAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.security.api.domain.permission.SecurityPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.security.api.network.route.management.security.settings.ManagementSecuritySettingsRoutes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management HTTP routes for updating persisted security settings.
 */
@Singleton
class ManagementSecuritySettingsRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val updateSecuritySettingsUseCase: UpdateSecuritySettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.put(
            path = ManagementSecuritySettingsRoutes.UPDATE_SECURITY_SETTINGS,
            builder = { updateSecuritySettingsDocs() },
            body = { updateSecuritySettings() }
        )
    }

    private fun RouteConfig.updateSecuritySettingsDocs() {
        summary = UPDATE_SECURITY_SETTINGS_ROUTE_SUMMARY
        description = UPDATE_SECURITY_SETTINGS_ROUTE_DESCRIPTION
        operationId = UPDATE_SECURITY_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, SecuritySwaggerTags.SECURITY_SETTINGS)
        request {
            body<SecuritySettingsPayload>()
        }
        response {
            code(HttpStatusCode.NoContent) {
                description = UPDATE_SECURITY_SETTINGS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.updateSecuritySettings() {
        val requestContext = call.getRequestContext()
        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN),
            requiredAccountStatus = setOf(UserAccountStatus.ACTIVE),
            requiredPermissions = setOf(SecurityPermissionCode.SECURITY_SETTINGS_UPDATE)
        )

        if (authorizeResult is AppResult.Error) {
            val metadata: Set<AuditEventMetadata> = authorizeResult.error.toDeniedAuditEventMetadata() +
                    requestContext.clientInfo.toAuditMetadata()
            auditLogger.log(
                actorId = requestContext.userId?.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = requestContext.userRole?.serialName,
                action = SecurityAuditActionType.MANAGEMENT_UPDATE_SECURITY_SETTINGS,
                resource = SecurityAuditResourceType.SECURITY_SETTINGS,
                status = AuditStatus.DENIED,
                metadata = metadata
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val request = call.validateRequest<SecuritySettingsPayload>()

        val result = updateSecuritySettingsUseCase(
            securitySettings = request.toSecuritySettings(),
            requestContext = requestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    companion object {
        const val UPDATE_SECURITY_SETTINGS_ROUTE_SUMMARY = "Update security settings"
        const val UPDATE_SECURITY_SETTINGS_ROUTE_DESCRIPTION =
            "Replaces effective security settings with the payload."
        const val UPDATE_SECURITY_SETTINGS_ROUTE_OPERATION_ID = "updateSecuritySettings"
        const val UPDATE_SECURITY_SETTINGS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION =
            "Settings were updated successfully; no response body."
    }
}
