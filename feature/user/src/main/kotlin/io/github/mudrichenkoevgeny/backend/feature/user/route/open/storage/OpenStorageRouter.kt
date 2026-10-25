package io.github.mudrichenkoevgeny.backend.feature.user.route.open.storage

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validatePathParameter
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateRequest
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.route.CommonSwaggerTags
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.core.storage.network.query.parseSelfStorageFileListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.StorageSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage.ConfirmUploadUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage.DeleteFileUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage.DeleteFilesUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage.GeneratePresignedUploadUrlUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage.GetFileUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage.GetFilesUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.mapper.pagedresult.mapItems
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.toFileIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.mapper.file.toStorageFilePrivatePayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.mapper.file.toStorageFileSummaryPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.mapper.presigned.toPresignedUrlPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.contract.StorageApiFields
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.contract.StorageApiPaths
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.model.file.StorageFilePrivatePayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.model.file.StorageFileSummaryPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.model.presigned.PresignedUrlPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.request.file.DeleteFilesBatchRequest
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.request.presigned.PresignedUrlRequest
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.network.route.open.OpenStorageRoutes
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
 * Router managing user storage file operations.
 *
 * Registered routes:
 * 1. [OpenStorageRoutes.GET_FILES] — retrieves user's paged file list via [GetFilesUseCase].
 * 2. [OpenStorageRoutes.GET_FILE] — retrieves specific file payload via [GetFileUseCase].
 * 3. [OpenStorageRoutes.POST_PRESIGNED_UPLOAD_URL] — generates presigned S3 upload URL via [GeneratePresignedUploadUrlUseCase].
 * 4. [OpenStorageRoutes.CONFIRM_UPLOAD] — finalizes upload session via [ConfirmUploadUseCase].
 * 5. [OpenStorageRoutes.DELETE_FILE] — deletes user file via [DeleteFileUseCase].
 * 6. [OpenStorageRoutes.DELETE_FILES_BATCH] — deletes multiple user files in batch via [DeleteFilesUseCase].
 */
@Singleton
class OpenStorageRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val getFilesUseCase: GetFilesUseCase,
    private val getFileUseCase: GetFileUseCase,
    private val generatePresignedUploadUrlUseCase: GeneratePresignedUploadUrlUseCase,
    private val confirmUploadUseCase: ConfirmUploadUseCase,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val deleteFilesUseCase: DeleteFilesUseCase,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser
) : BaseRouter {

    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerGetFilesRoute(this)
            registerGetFileRoute(this)
            registerPostPresignedUrlRoute(this)
            registerConfirmUploadRoute(this)
            registerDeleteFileRoute(this)
            registerDeleteFilesBatchRoute(this)
        }
    }

    private fun registerGetFilesRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.get(
            path = OpenStorageRoutes.GET_FILES,
            builder = { getFilesDocs(allowedRoles, allowedAccountStatuses) },
            body = { getFiles(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.getFilesDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_FILES_ROUTE_SUMMARY
        operationId = GET_FILES_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + StorageSwaggerTags.STORAGE_FILES)

        description = getFormattedDescription(
            description = GET_FILES_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
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
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val queryParams = call.parseSelfStorageFileListQueryParams()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = getFilesUseCase(
            authenticatedRequestContext = authenticatedRequestContext,
            pageParams = queryParams.listing.pageParams,
            sortBy = queryParams.listing.sortBy,
            sortOrder = queryParams.listing.sortOrder,
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
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.get(
            path = OpenStorageRoutes.GET_FILE,
            builder = { getFileDocs(allowedRoles, allowedAccountStatuses) },
            body = { getFile(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.getFileDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_FILE_ROUTE_SUMMARY
        operationId = GET_FILE_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + StorageSwaggerTags.STORAGE_FILES)

        description = getFormattedDescription(
            description = GET_FILE_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
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
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val fileId = call.validatePathParameter(StorageApiPaths.FILE_ID) { id ->
            id.toFileIdOrThrow()
        }

        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = getFileUseCase(
            fileId = fileId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { storageFilePrivate ->
            storageFilePrivate.toStorageFilePrivatePayload()
        }
    }

    private fun registerPostPresignedUrlRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.post(
            path = OpenStorageRoutes.POST_PRESIGNED_UPLOAD_URL,
            builder = { postPresignedUrlDocs(allowedRoles, allowedAccountStatuses) },
            body = { postPresignedUrl(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.postPresignedUrlDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = POST_PRESIGNED_URL_ROUTE_SUMMARY
        operationId = POST_PRESIGNED_URL_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + StorageSwaggerTags.STORAGE_FILES)
        description = getFormattedDescription(
            description = POST_PRESIGNED_URL_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = false
        )
        request { body<PresignedUrlRequest>() }
        response {
            code(HttpStatusCode.OK) {
                body<PresignedUrlPayload>()
                description = POST_PRESIGNED_URL_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.postPresignedUrl(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()
        val request = call.validateRequest<PresignedUrlRequest>()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val storageCategory = StorageCategory.fromValueOrNull(request.category)
            ?: throw RequestHandlingException(
                CommonError.InvalidFieldValue(
                    fieldName = StorageApiFields.CATEGORY
                )
            )

        val storageFileVisibility = StorageFileVisibility.fromValueOrNull(request.visibility)
            ?: throw RequestHandlingException(
                CommonError.InvalidFieldValue(
                    fieldName = StorageApiFields.VISIBILITY
                )
            )

        val result = generatePresignedUploadUrlUseCase(
            fileName = request.fileName,
            fileSizeBytes = request.fileSizeBytes,
            mimeType = request.mimeType,
            storageCategory = storageCategory,
            storageFileVisibility = storageFileVisibility,
            targetScope = request.targetScope,
            authenticatedRequestContext = authenticatedRequestContext
        )
        call.respondResult(result, appLogger, appErrorParser) { presignedUrl ->
            presignedUrl.toPresignedUrlPayload()
        }
    }

    private fun registerConfirmUploadRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.post(
            path = OpenStorageRoutes.CONFIRM_UPLOAD,
            builder = { confirmUploadDocs(allowedRoles, allowedAccountStatuses) },
            body = { confirmUpload(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.confirmUploadDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = CONFIRM_UPLOAD_ROUTE_SUMMARY
        operationId = CONFIRM_UPLOAD_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + StorageSwaggerTags.STORAGE_FILES)
        description = getFormattedDescription(
            description = CONFIRM_UPLOAD_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = false
        )
        response {
            code(HttpStatusCode.OK) {
                body<StorageFilePrivatePayload>()
                description = CONFIRM_UPLOAD_ROUTE_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.confirmUpload(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val fileId = call.validatePathParameter(StorageApiPaths.FILE_ID) { fileId ->
            fileId.toFileIdOrThrow()
        }
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = confirmUploadUseCase(
            fileId = fileId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { storageFilePrivate ->
            storageFilePrivate.toStorageFilePrivatePayload()
        }
    }

    private fun registerDeleteFileRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.delete(
            path = OpenStorageRoutes.DELETE_FILE,
            builder = { deleteFileDocs(allowedRoles, allowedAccountStatuses) },
            body = { deleteFile(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.deleteFileDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = DELETE_FILE_ROUTE_SUMMARY
        operationId = DELETE_FILE_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + StorageSwaggerTags.STORAGE_FILES)

        description = getFormattedDescription(
            description = DELETE_FILE_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = false
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
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val fileId = call.validatePathParameter(StorageApiPaths.FILE_ID) { id ->
            id.toFileIdOrThrow()
        }

        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = deleteFileUseCase(
            fileId = fileId,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun registerDeleteFilesBatchRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE)

        route.post(
            path = OpenStorageRoutes.DELETE_FILES_BATCH,
            builder = { deleteFilesBatchDocs(allowedRoles, allowedAccountStatuses) },
            body = { deleteFilesBatch(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.deleteFilesBatchDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = DELETE_FILES_BATCH_ROUTE_SUMMARY
        operationId = DELETE_FILES_BATCH_ROUTE_OPERATION_ID
        tags = listOf(CommonSwaggerTags.OPEN_PREFIX + StorageSwaggerTags.STORAGE_FILES)

        description = getFormattedDescription(
            description = DELETE_FILES_BATCH_ROUTE_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.value },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.value },
            isPublic = false
        )

        request {
            body<DeleteFilesBatchRequest>()
        }

        response {
            code(HttpStatusCode.NoContent) {
                description = DELETE_FILES_BATCH_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.deleteFilesBatch(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val request = call.validateRequest<DeleteFilesBatchRequest>()
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val fileIds = request.fileIds.map { id ->
            id.toFileIdOrThrow()
        }

        val result = deleteFilesUseCase(
            fileIds = fileIds,
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser)
    }

    companion object {
        const val GET_FILES_ROUTE_SUMMARY = "List storage files"
        const val GET_FILES_ROUTE_DESCRIPTION = "Retrieves a paged list of summary storage files uploaded by the current authenticated user."
        const val GET_FILES_ROUTE_OPERATION_ID = "openGetStorageFiles"
        const val GET_FILES_ROUTE_RESPONSE_OK_DESCRIPTION = "Storage files retrieved successfully"

        const val GET_FILE_ROUTE_SUMMARY = "Get storage file"
        const val GET_FILE_ROUTE_DESCRIPTION = "Retrieves detailed private view of a specific file uploaded by the current authenticated user."
        const val GET_FILE_ROUTE_OPERATION_ID = "openGetStorageFile"
        const val GET_FILE_ROUTE_PATH_PARAMETER_ID_DESCRIPTION = "The ID of the storage file to retrieve."
        const val GET_FILE_ROUTE_RESPONSE_OK_DESCRIPTION = "Storage file retrieved successfully"

        const val POST_PRESIGNED_URL_ROUTE_SUMMARY = "Request upload presigned URL"
        const val POST_PRESIGNED_URL_ROUTE_DESCRIPTION = "Requests a short-lived presigned S3 PUT URL for direct client binary upload."
        const val POST_PRESIGNED_URL_ROUTE_OPERATION_ID = "postOpenPresignedUrl"
        const val POST_PRESIGNED_URL_ROUTE_RESPONSE_OK_DESCRIPTION = "Presigned URL payload"

        const val CONFIRM_UPLOAD_ROUTE_SUMMARY = "Confirm file upload"
        const val CONFIRM_UPLOAD_ROUTE_DESCRIPTION = "Finalizes upload session after binary upload to S3 completes."
        const val CONFIRM_UPLOAD_ROUTE_OPERATION_ID = "postOpenConfirmUpload"
        const val CONFIRM_UPLOAD_ROUTE_RESPONSE_OK_DESCRIPTION = "Confirmed file record payload"

        const val DELETE_FILE_ROUTE_SUMMARY = "Delete storage file"
        const val DELETE_FILE_ROUTE_DESCRIPTION = "Deletes a specific file uploaded by the current authenticated user."
        const val DELETE_FILE_ROUTE_OPERATION_ID = "openDeleteStorageFile"
        const val DELETE_FILE_ROUTE_PATH_PARAMETER_ID_DESCRIPTION = "The ID of the storage file to delete."
        const val DELETE_FILE_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION = "Storage file deleted successfully"

        const val DELETE_FILES_BATCH_ROUTE_SUMMARY = "Delete multiple storage files"
        const val DELETE_FILES_BATCH_ROUTE_DESCRIPTION = "Deletes multiple specific files uploaded by the current authenticated user in a single batch operation."
        const val DELETE_FILES_BATCH_ROUTE_OPERATION_ID = "openDeleteStorageFilesBatch"
        const val DELETE_FILES_BATCH_ROUTE_RESPONSE_NO_CONTENT_DESCRIPTION = "Storage files deleted successfully"
    }
}
