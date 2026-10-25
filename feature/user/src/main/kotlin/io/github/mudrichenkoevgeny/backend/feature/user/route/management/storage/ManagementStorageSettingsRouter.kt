package io.github.mudrichenkoevgeny.backend.feature.user.route.management.storage

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.model.SecurityRequirementType
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage.settings.GetManagementStorageSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage.settings.ResetStorageSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage.settings.UpdateStorageSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.storage.settings.GetOpenStorageSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.StorageSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.audit.action.StorageAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.audit.resource.StorageAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.permission.StoragePermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.mapper.storagesettings.toManagementStorageSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.mapper.storagesettings.toManagementStorageSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.mapper.storagesettings.toOpenStorageSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.model.storagesettings.ManagementStorageSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.model.storagesettings.OpenStorageSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.route.management.ManagementStorageSettingsRoutes
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
 * Router for administrative management of storage system settings.
 *
 * Registered routes:
 * 1. [ManagementStorageSettingsRoutes.UPDATE_MANAGEMENT_STORAGE_SETTINGS] — updates settings via [UpdateStorageSettingsUseCase].
 * 2. [ManagementStorageSettingsRoutes.GET_MANAGEMENT_STORAGE_SETTINGS] — retrieves settings via [GetManagementStorageSettingsUseCase].
 * 3. [ManagementStorageSettingsRoutes.RESET_MANAGEMENT_STORAGE_SETTINGS] — resets settings via [ResetStorageSettingsUseCase].
 * 4. [ManagementStorageSettingsRoutes.GET_OPEN_STORAGE_SETTINGS] — retrieves open settings via [GetOpenStorageSettingsUseCase].
 */
@Singleton
class ManagementStorageSettingsRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val updateStorageSettingsUseCase: UpdateStorageSettingsUseCase,
    private val getManagementStorageSettingsUseCase: GetManagementStorageSettingsUseCase,
    private val resetStorageSettingsUseCase: ResetStorageSettingsUseCase,
    private val getOpenStorageSettingsUseCase: GetOpenStorageSettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        registerGetOpenStorageSettingsRoute(route)
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerUpdateStorageSettingsRoute(this)
            registerGetStorageSettingsRoute(this)
            registerResetStorageSettingsRoute(this)
        }
    }

    private fun registerUpdateStorageSettingsRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)
        val requiredPermissions = setOf(StoragePermissionCode.STORAGE_SETTINGS_UPDATE)

        route.put(
            path = ManagementStorageSettingsRoutes.UPDATE_MANAGEMENT_STORAGE_SETTINGS,
            builder = { updateStorageSettingsDocs(allowedRoles, allowedAccountStatuses, requiredPermissions) },
            body = { updateStorageSettings(allowedRoles, allowedAccountStatuses, requiredPermissions) }
        )
    }

    private fun registerGetStorageSettingsRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.get(
            path = ManagementStorageSettingsRoutes.GET_MANAGEMENT_STORAGE_SETTINGS,
            builder = { getStorageSettingsDocs(allowedRoles, allowedAccountStatuses) },
            body = { getStorageSettings(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.updateStorageSettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        summary = UPDATE_STORAGE_SETTINGS_ROUTE_SUMMARY
        operationId = UPDATE_STORAGE_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + StorageSwaggerTags.STORAGE_SETTINGS)

        description = getFormattedDescription(
            description = UPDATE_STORAGE_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            requiredPermissions = requiredPermissions.mapToSet { it.value },
            isPublic = false,
            securityType = SecurityRequirementType.SENSITIVE_STEP_UP_TOTP_REQUIRED
        )

        request {
            body<ManagementStorageSettingsPayload>()
        }
        response {
            code(HttpStatusCode.OK) {
                body<ManagementStorageSettingsPayload>()
                description = UPDATE_STORAGE_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.updateStorageSettings(
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
                actionType = StorageAuditActionType.MANAGEMENT_UPDATE_STORAGE_SETTINGS,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val request = call.validateRequest<ManagementStorageSettingsPayload>()

        val result = updateStorageSettingsUseCase(
            managementStorageSettings = request.toManagementStorageSettings(),
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { storageSettings ->
            storageSettings.toManagementStorageSettingsPayload()
        }
    }

    private fun RouteConfig.getStorageSettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_STORAGE_SETTINGS_ROUTE_SUMMARY
        operationId = GET_STORAGE_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + StorageSwaggerTags.STORAGE_SETTINGS)

        description = getFormattedDescription(
            description = GET_STORAGE_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = false
        )

        response {
            code(HttpStatusCode.OK) {
                body<ManagementStorageSettingsPayload>()
                description = GET_STORAGE_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getStorageSettings(
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

        val result = getManagementStorageSettingsUseCase()

        call.respondResult(result, appLogger, appErrorParser) { storageSettings ->
            storageSettings.toManagementStorageSettingsPayload()
        }
    }

    private fun registerResetStorageSettingsRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)
        val requiredPermissions = setOf(StoragePermissionCode.STORAGE_SETTINGS_UPDATE)

        route.post(
            path = ManagementStorageSettingsRoutes.RESET_MANAGEMENT_STORAGE_SETTINGS,
            builder = { resetStorageSettingsDocs(allowedRoles, allowedAccountStatuses, requiredPermissions) },
            body = { resetStorageSettings(allowedRoles, allowedAccountStatuses, requiredPermissions) }
        )
    }

    private fun RouteConfig.resetStorageSettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        summary = RESET_STORAGE_SETTINGS_ROUTE_SUMMARY
        operationId = RESET_STORAGE_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + StorageSwaggerTags.STORAGE_SETTINGS)

        description = getFormattedDescription(
            description = RESET_STORAGE_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            requiredPermissions = requiredPermissions.mapToSet { it.value },
            isPublic = false,
            securityType = SecurityRequirementType.SENSITIVE_STEP_UP_TOTP_REQUIRED
        )

        response {
            code(HttpStatusCode.OK) {
                body<ManagementStorageSettingsPayload>()
                description = RESET_STORAGE_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.resetStorageSettings(
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
                actionType = StorageAuditActionType.MANAGEMENT_RESET_STORAGE_SETTINGS,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = resetStorageSettingsUseCase(
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { storageSettings ->
            storageSettings.toManagementStorageSettingsPayload()
        }
    }

    private fun registerGetOpenStorageSettingsRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.get(
            path = ManagementStorageSettingsRoutes.GET_OPEN_STORAGE_SETTINGS,
            builder = { getOpenStorageSettingsDocs(allowedRoles, allowedAccountStatuses) },
            body = { getOpenStorageSettings() }
        )
    }

    private fun RouteConfig.getOpenStorageSettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_OPEN_STORAGE_SETTINGS_ROUTE_SUMMARY
        operationId = GET_OPEN_STORAGE_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + StorageSwaggerTags.STORAGE_SETTINGS)

        description = getFormattedDescription(
            description = GET_OPEN_STORAGE_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = true
        )

        response {
            code(HttpStatusCode.OK) {
                body<OpenStorageSettingsPayload>()
                description = GET_OPEN_STORAGE_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getOpenStorageSettings() {
        val result = getOpenStorageSettingsUseCase()

        call.respondResult(result, appLogger, appErrorParser) { storageSettings ->
            storageSettings.toOpenStorageSettingsPayload()
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
            actorUserRole = authenticatedRequestContext.userRole.value,
            action = actionType,
            resource = StorageAuditResourceType.STORAGE_SETTINGS,
            status = AuditStatus.DENIED,
            metadata = metadata
        )
    }

    companion object {
        const val GET_OPEN_STORAGE_SETTINGS_ROUTE_SUMMARY = "Get open storage settings"
        const val GET_OPEN_STORAGE_SETTINGS_ROUTE_DESCRIPTION = "Returns storage settings for unauthenticated management users."
        const val GET_OPEN_STORAGE_SETTINGS_ROUTE_OPERATION_ID = "managementGetOpenStorageSettings"
        const val GET_OPEN_STORAGE_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION = "Open storage settings data"

        const val UPDATE_STORAGE_SETTINGS_ROUTE_SUMMARY = "Update storage settings"
        const val UPDATE_STORAGE_SETTINGS_ROUTE_DESCRIPTION = "Replaces effective storage settings with the payload."
        const val UPDATE_STORAGE_SETTINGS_ROUTE_OPERATION_ID = "managementUpdateStorageSettings"
        const val UPDATE_STORAGE_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION =
            "Storage settings updated successfully."

        const val GET_STORAGE_SETTINGS_ROUTE_SUMMARY = "Get storage settings"
        const val GET_STORAGE_SETTINGS_ROUTE_DESCRIPTION = "Returns management storage settings."
        const val GET_STORAGE_SETTINGS_ROUTE_OPERATION_ID = "managementGetStorageSettings"
        const val GET_STORAGE_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION = "Storage settings data"

        const val RESET_STORAGE_SETTINGS_ROUTE_SUMMARY = "Reset storage settings"
        const val RESET_STORAGE_SETTINGS_ROUTE_DESCRIPTION = "Resets effective storage settings to default values."
        const val RESET_STORAGE_SETTINGS_ROUTE_OPERATION_ID = "managementResetStorageSettings"
        const val RESET_STORAGE_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION = "Restored storage settings data"
    }
}