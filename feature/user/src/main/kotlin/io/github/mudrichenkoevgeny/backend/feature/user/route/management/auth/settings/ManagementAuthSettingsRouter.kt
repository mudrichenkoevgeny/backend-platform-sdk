package io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.settings

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.feature.user.audit.toDeniedUserAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auth.settings.GetManagementAuthSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auth.settings.UpdateAuthSettingsUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.settings.toManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.settings.toManagementAuthSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.auth.settings.ManagementAuthSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.management.auth.settings.ManagementAuthSettingsRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management HTTP routes for reading and updating auth settings.
 */
@Singleton
class ManagementAuthSettingsRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val getManagementAuthSettingsUseCase: GetManagementAuthSettingsUseCase,
    private val updateAuthSettingsUseCase: UpdateAuthSettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.get(
            path = ManagementAuthSettingsRoutes.GET_AUTH_SETTINGS_MANAGEMENT,
            builder = { getAuthSettingsManagementDocs() },
            body = { getAuthSettingsManagement() }
        )
        route.put(
            path = ManagementAuthSettingsRoutes.UPDATE_AUTH_SETTINGS,
            builder = { updateAuthSettingsDocs() },
            body = { updateAuthSettings() }
        )
    }

    private fun RouteConfig.getAuthSettingsManagementDocs() {
        summary = GET_AUTH_SETTINGS_MANAGEMENT_ROUTE_SUMMARY
        description = GET_AUTH_SETTINGS_MANAGEMENT_ROUTE_DESCRIPTION
        operationId = GET_AUTH_SETTINGS_MANAGEMENT_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.AUTH_SETTINGS)
        response {
            code(HttpStatusCode.OK) {
                description = GET_AUTH_SETTINGS_MANAGEMENT_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getAuthSettingsManagement() {
        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN),
            requiredAccountStatus = setOf(
                UserAccountStatus.ACTIVE,
                UserAccountStatus.READ_ONLY
            )
        )
        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = getManagementAuthSettingsUseCase.execute()
        call.respondResult(result, appLogger, appErrorParser) { management ->
            management.toManagementAuthSettingsPayload()
        }
    }

    private fun RouteConfig.updateAuthSettingsDocs() {
        summary = UPDATE_AUTH_SETTINGS_ROUTE_SUMMARY
        description = UPDATE_AUTH_SETTINGS_ROUTE_DESCRIPTION
        operationId = UPDATE_AUTH_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT, UserSwaggerTags.AUTH_SETTINGS)
        request {
            body<ManagementAuthSettingsPayload>()
        }
        response {
            code(HttpStatusCode.NoContent) {
                description = UPDATE_AUTH_SETTINGS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.updateAuthSettings() {
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
                action = UserAuditActionType.MANAGEMENT_UPDATE_AUTH_SETTINGS,
                resource = UserAuditResourceType.AUTH_SETTINGS,
                status = AuditStatus.DENIED,
                metadata = metadata
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
        const val UPDATE_AUTH_SETTINGS_ROUTE_OPERATION_ID = "updateAuthSettings"
        const val UPDATE_AUTH_SETTINGS_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION =
            "Settings were updated successfully; no response body."
    }
}