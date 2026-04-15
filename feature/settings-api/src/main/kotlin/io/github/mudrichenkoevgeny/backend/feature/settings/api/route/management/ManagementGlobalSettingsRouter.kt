package io.github.mudrichenkoevgeny.backend.feature.settings.api.route.management

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.feature.settings.api.route.SettingsSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.settings.api.usecase.management.globalsettings.UpdateGlobalSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.audit.toDeniedUserAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.mapper.globalsettings.toGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.model.globalsettings.GlobalSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.settings.api.domain.audit.action.SettingsAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.settings.api.domain.audit.resource.SettingsAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.settings.api.domain.permission.SettingsPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.settings.api.network.route.management.globalsettings.ManagementGlobalSettingsRoutes
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
 * Management HTTP routes for updating persisted global settings.
 */
@Singleton
class ManagementGlobalSettingsRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val updateGlobalSettingsUseCase: UpdateGlobalSettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.put(
            path = ManagementGlobalSettingsRoutes.UPDATE_GLOBAL_SETTINGS,
            builder = { updateGlobalSettingsDocs() },
            body = { updateGlobalSettings() }
        )
    }

    private fun RouteConfig.updateGlobalSettingsDocs() {
        summary = UPDATE_GLOBAL_SETTINGS_ROUTE_SUMMARY
        description = UPDATE_GLOBAL_SETTINGS_ROUTE_DESCRIPTION
        operationId = UPDATE_GLOBAL_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, SettingsSwaggerTags.GLOBAL_SETTINGS)
        request {
            body<GlobalSettingsPayload>()
        }
        response {
            code(HttpStatusCode.NoContent) {
                description = UPDATE_GLOBAL_SETTINGS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.updateGlobalSettings() {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN),
            requiredAccountStatus = setOf(UserAccountStatus.ACTIVE),
            requiredPermissions = setOf(SettingsPermissionCode.GLOBAL_SETTINGS_UPDATE)
        )

        if (authorizeResult is AppResult.Error) {
            val metadata: Set<AuditEventMetadata> = authorizeResult.error.toDeniedUserAuditEventMetadata() +
                    authenticatedRequestContext.clientInfo.toAuditMetadata()
            auditLogger.log(
                actorId = authenticatedRequestContext.userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = authenticatedRequestContext.userRole.serialName,
                action = SettingsAuditActionType.MANAGEMENT_UPDATE_GLOBAL_SETTINGS,
                resource = SettingsAuditResourceType.GLOBAL_SETTINGS,
                status = AuditStatus.DENIED,
                metadata = metadata
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

    companion object {
        const val UPDATE_GLOBAL_SETTINGS_ROUTE_SUMMARY = "Update global settings"
        const val UPDATE_GLOBAL_SETTINGS_ROUTE_DESCRIPTION = "Replaces effective global settings with the payload."
        const val UPDATE_GLOBAL_SETTINGS_ROUTE_OPERATION_ID = "updateGlobalSettings"
        const val UPDATE_GLOBAL_SETTINGS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION =
            "Settings were updated successfully; no response body."
    }
}
