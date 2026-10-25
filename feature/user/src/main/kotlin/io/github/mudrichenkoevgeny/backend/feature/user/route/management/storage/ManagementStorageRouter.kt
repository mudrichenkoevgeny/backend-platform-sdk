package io.github.mudrichenkoevgeny.backend.feature.user.route.management.storage

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.model.SecurityRequirementType
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validatePathParameter
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.core.storage.network.query.parseManagementStorageFileListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.StorageSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage.ManagementDeleteFileUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage.ManagementDeleteFilesUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage.ManagementGetFileUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage.ManagementGetFilesUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.common.mapper.pagedresult.mapItems
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.audit.action.StorageAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.audit.resource.StorageAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.toFileIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.permission.StoragePermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.mapper.file.toStorageFilePrivatePayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.mapper.file.toStorageFileSummaryPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.contract.StorageApiPaths
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.model.file.StorageFilePrivatePayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.model.file.StorageFileSummaryPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.request.file.DeleteFilesBatchRequest
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.route.management.ManagementStorageRoutes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
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
 * Router for administrative storage file management endpoints.
 *
 * Registered routes:
 * 1. [ManagementStorageRoutes.GET_FILES] — retrieves paged list of files across all users via [ManagementGetFilesUseCase].
 * 2. [ManagementStorageRoutes.GET_FILE] — retrieves detailed file record via [ManagementGetFileUseCase].
 * 3. [ManagementStorageRoutes.DELETE_FILES_BATCH] — deletes multiple files via [ManagementDeleteFilesUseCase].
 * 4. [ManagementStorageRoutes.DELETE_FILE] — deletes single file via [ManagementDeleteFileUseCase].
 */
@Singleton
class ManagementStorageRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val managementGetFilesUseCase: ManagementGetFilesUseCase,
    private val managementGetFileUseCase: ManagementGetFileUseCase,
    private val managementDeleteFileUseCase: ManagementDeleteFileUseCase,
    private val managementDeleteFilesUseCase: ManagementDeleteFilesUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerGetFilesRoute(this)
            registerGetFileRoute(this)
            registerDeleteFilesBatchRoute(this)
            registerDeleteFileRoute(this)
        }
    }

    private fun registerGetFilesRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)
        val requiredPermissions = setOf(StoragePermissionCode.STORAGE_VIEW_OTHER_USERS_FILES)

        route.get(
            path = ManagementStorageRoutes.GET_FILES,
            builder = { getFilesDocs(allowedRoles, allowedAccountStatuses, requiredPermissions) },
            body = { getFiles(allowedRoles, allowedAccountStatuses, requiredPermissions) }
        )
    }

    private fun RouteConfig.getFilesDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        summary = GET_FILES_ROUTE_SUMMARY
        operationId = GET_FILES_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + StorageSwaggerTags.STORAGE_FILES)

        description = getFormattedDescription(
            description = GET_FILES_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            requiredPermissions = requiredPermissions.mapToSet { it.value },
            isPublic = false
        )

        response {
            code(HttpStatusCode.OK) {
                body<PagedResult<StorageFileSummaryPayload>>()
                description = GET_FILES_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getFiles(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val queryParams = call.parseManagementStorageFileListQueryParams()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses,
            requiredPermissions = requiredPermissions
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = managementGetFilesUseCase(
            authenticatedRequestContext = authenticatedRequestContext,
            pageParams = queryParams.listing.pageParams,
            sortBy = queryParams.listing.sortBy,
            sortOrder = queryParams.listing.sortOrder,
            ownerUserIds = queryParams.ownerUserIds,
            categories = queryParams.categories,
            visibilities = queryParams.visibilities,
            statuses = queryParams.statuses,
            mimeTypes = queryParams.mimeTypes,
            fileNames = queryParams.fileNames
        )

        call.respondResult(result, appLogger, appErrorParser) { pagedFiles ->
            pagedFiles.mapItems { storageFileSummary -> storageFileSummary.toStorageFileSummaryPayload() }
        }
    }

    private fun registerGetFileRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)
        val requiredPermissions = setOf(StoragePermissionCode.STORAGE_VIEW_OTHER_USERS_FILES)

        route.get(
            path = ManagementStorageRoutes.GET_FILE,
            builder = { getFileDocs(allowedRoles, allowedAccountStatuses, requiredPermissions) },
            body = { getFile(allowedRoles, allowedAccountStatuses, requiredPermissions) }
        )
    }

    private fun RouteConfig.getFileDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        summary = GET_FILE_ROUTE_SUMMARY
        operationId = GET_FILE_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + StorageSwaggerTags.STORAGE_FILES)

        description = getFormattedDescription(
            description = GET_FILE_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            requiredPermissions = requiredPermissions.mapToSet { it.value },
            isPublic = false
        )

        request {
            pathParameter<String>(StorageApiPaths.FILE_ID) {
                description = GET_FILE_ROUTE_PATH_PARAMETER_ID_DESCRIPTION
            }
        }

        response {
            code(HttpStatusCode.OK) {
                body<StorageFilePrivatePayload>()
                description = GET_FILE_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.getFile(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        val fileId = call.validatePathParameter(StorageApiPaths.FILE_ID) { id ->
            id.toFileIdOrThrow()
        }

        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses,
            requiredPermissions = requiredPermissions
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = managementGetFileUseCase(
            fileId = fileId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { storageFilePrivate ->
            storageFilePrivate.toStorageFilePrivatePayload()
        }
    }

    private fun registerDeleteFilesBatchRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)
        val requiredPermissions = setOf(StoragePermissionCode.STORAGE_DELETE_OTHER_USERS_FILES)

        route.post(
            path = ManagementStorageRoutes.DELETE_FILES_BATCH,
            builder = { deleteFilesBatchDocs(allowedRoles, allowedAccountStatuses, requiredPermissions) },
            body = { deleteFilesBatch(allowedRoles, allowedAccountStatuses, requiredPermissions) }
        )
    }

    private fun RouteConfig.deleteFilesBatchDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        summary = DELETE_FILES_BATCH_ROUTE_SUMMARY
        operationId = DELETE_FILES_BATCH_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + StorageSwaggerTags.STORAGE_FILES)

        description = getFormattedDescription(
            description = DELETE_FILES_BATCH_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            requiredPermissions = requiredPermissions.mapToSet { it.value },
            securityType = SecurityRequirementType.SENSITIVE_STEP_UP_TOTP_REQUIRED
        )

        request {
            body<DeleteFilesBatchRequest>()
        }

        response {
            code(HttpStatusCode.OK) {
                description = DELETE_FILES_BATCH_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.deleteFilesBatch(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val request = call.validateRequest<DeleteFilesBatchRequest>()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses,
            requiredPermissions = requiredPermissions
        )

        if (authorizeResult is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = StorageAuditActionType.MANAGEMENT_DELETE_FILE,
                resourceIds = request.fileIds,
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val fileIds = request.fileIds.map { id ->
            id.toFileIdOrThrow()
        }

        val result = managementDeleteFilesUseCase(
            fileIds = fileIds,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun registerDeleteFileRoute(route: Route) {
        val allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)
        val requiredPermissions = setOf(StoragePermissionCode.STORAGE_DELETE_OTHER_USERS_FILES)

        route.delete(
            path = ManagementStorageRoutes.DELETE_FILE,
            builder = { deleteFileDocs(allowedRoles, allowedAccountStatuses, requiredPermissions) },
            body = { deleteFile(allowedRoles, allowedAccountStatuses, requiredPermissions) }
        )
    }

    private fun RouteConfig.deleteFileDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        summary = DELETE_FILE_ROUTE_SUMMARY
        operationId = DELETE_FILE_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.MANAGEMENT_PREFIX + StorageSwaggerTags.STORAGE_FILES)

        description = getFormattedDescription(
            description = DELETE_FILE_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            requiredPermissions = requiredPermissions.mapToSet { it.value },
            securityType = SecurityRequirementType.SENSITIVE_STEP_UP_TOTP_REQUIRED
        )

        request {
            pathParameter<String>(StorageApiPaths.FILE_ID) {
                description = DELETE_FILE_ROUTE_PATH_PARAMETER_ID_DESCRIPTION
            }
        }

        response {
            code(HttpStatusCode.NoContent) {
                description = DELETE_FILE_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.deleteFile(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ) {
        val fileId = call.validatePathParameter(StorageApiPaths.FILE_ID) { id ->
            id.toFileIdOrThrow()
        }

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
                actionType = StorageAuditActionType.MANAGEMENT_DELETE_FILE,
                resourceIds = listOf(fileId.asHexDashString()),
                error = authorizeResult.error
            )
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = managementDeleteFileUseCase(
            fileId = fileId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun logErrorToAudit(
        authenticatedRequestContext: AuthenticatedRequestContext,
        actionType: AuditActionType,
        resourceIds: List<String> = emptyList(),
        error: AppError
    ) {
        val errorData = auditErrorConverter.convert(error)
        val metadata = authenticatedRequestContext.clientInfo.toAuditMetadata() + errorData.metadata

        if (resourceIds.isEmpty()) {
            auditLogger.log(
                actorId = authenticatedRequestContext.userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = authenticatedRequestContext.userRole.value,
                action = actionType,
                resource = StorageAuditResourceType.STORAGE_FILE,
                resourceId = null,
                status = AuditStatus.DENIED,
                metadata = metadata
            )
        } else {
            for (resourceId in resourceIds) {
                auditLogger.log(
                    actorId = authenticatedRequestContext.userId.asHexDashString(),
                    actorType = AuditActorType.USER,
                    actorUserRole = authenticatedRequestContext.userRole.value,
                    action = actionType,
                    resource = StorageAuditResourceType.STORAGE_FILE,
                    resourceId = resourceId,
                    status = AuditStatus.DENIED,
                    metadata = metadata
                )
            }
        }
    }

    companion object {
        const val GET_FILES_ROUTE_SUMMARY = "Get all platform files (management)"
        const val GET_FILES_ROUTE_DESCRIPTION = "Returns a paged list of all storage files across the platform for administrative inspection."
        const val GET_FILES_ROUTE_OPERATION_ID = "managementGetStorageFiles"
        const val GET_FILES_ROUTE_RESPONSE_OK_DESCRIPTION = "Storage files retrieved successfully"

        const val GET_FILE_ROUTE_SUMMARY = "Get storage file (management)"
        const val GET_FILE_ROUTE_DESCRIPTION = "Retrieves detailed private view of any specific storage file in the platform."
        const val GET_FILE_ROUTE_OPERATION_ID = "managementGetStorageFile"
        const val GET_FILE_ROUTE_PATH_PARAMETER_ID_DESCRIPTION = "The ID of the storage file to retrieve."
        const val GET_FILE_ROUTE_RESPONSE_OK_DESCRIPTION = "Storage file retrieved successfully"

        const val DELETE_FILES_BATCH_ROUTE_SUMMARY = "Batch delete files (management)"
        const val DELETE_FILES_BATCH_ROUTE_DESCRIPTION = "Deletes multiple specified files across the platform."
        const val DELETE_FILES_BATCH_ROUTE_OPERATION_ID = "managementDeleteStorageFilesBatch"
        const val DELETE_FILES_BATCH_ROUTE_RESPONSE_OK_DESCRIPTION = "Batch delete operation completed"

        const val DELETE_FILE_ROUTE_SUMMARY = "Delete storage file (management)"
        const val DELETE_FILE_ROUTE_DESCRIPTION = "Deletes a specific file and removes its binary from S3, ignoring ownership."
        const val DELETE_FILE_ROUTE_OPERATION_ID = "managementDeleteStorageFile"
        const val DELETE_FILE_ROUTE_PATH_PARAMETER_ID_DESCRIPTION = "The ID of the storage file to delete."
        const val DELETE_FILE_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION = "Storage file deleted successfully"
    }
}
