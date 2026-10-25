package io.github.mudrichenkoevgeny.backend.feature.user.route.open.storage

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.storage.settings.GetOpenStorageSettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.route.StorageSwaggerTags
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.mapper.storagesettings.toOpenStorageSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.model.storagesettings.OpenStorageSettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.route.open.OpenStorageSettingsRoutes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Router for public access to storage-related configuration.
 *
 * Registered routes:
 * 1. [OpenStorageSettingsRoutes.GET_OPEN_STORAGE_SETTINGS] — retrieves storage settings via [GetOpenStorageSettingsUseCase].
 */
@Singleton
class OpenStorageSettingsRouter @Inject constructor(
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val getOpenStorageSettingsUseCase: GetOpenStorageSettingsUseCase
) : BaseRouter {

    override fun register(route: Route) {
        registerGetStorageSettingsRoute(route)
    }

    private fun registerGetStorageSettingsRoute(route: Route) {
        val allowedRoles = UserRole.entries.toSet()
        val allowedAccountStatuses = UserAccountStatus.entries.toSet()

        route.get(
            path = OpenStorageSettingsRoutes.GET_OPEN_STORAGE_SETTINGS,
            builder = { getStorageSettingsDocs(allowedRoles, allowedAccountStatuses) },
            body = { getStorageSettings() }
        )
    }

    private fun RouteConfig.getStorageSettingsDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_STORAGE_SETTINGS_ROUTE_SUMMARY
        operationId = GET_STORAGE_SETTINGS_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + StorageSwaggerTags.STORAGE_SETTINGS)

        description = getFormattedDescription(
            description = GET_STORAGE_SETTINGS_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = true
        )

        response {
            code(HttpStatusCode.OK) {
                body<OpenStorageSettingsPayload>()
                description = GET_STORAGE_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getStorageSettings() {
        val result = getOpenStorageSettingsUseCase()

        call.respondResult(result, appLogger, appErrorParser) { storageSettings ->
            storageSettings.toOpenStorageSettingsPayload()
        }
    }

    companion object {
        const val GET_STORAGE_SETTINGS_ROUTE_SUMMARY = "Get storage settings"
        const val GET_STORAGE_SETTINGS_ROUTE_DESCRIPTION = "Returns public storage file upload limits."
        const val GET_STORAGE_SETTINGS_ROUTE_OPERATION_ID = "openGetStorageSettings"
        const val GET_STORAGE_SETTINGS_ROUTE_RESPONSE_OK_DESCRIPTION = "Public storage settings data"
    }
}