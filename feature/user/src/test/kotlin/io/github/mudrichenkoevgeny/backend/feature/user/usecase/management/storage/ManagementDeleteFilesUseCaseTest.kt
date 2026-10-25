package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.error.CommonAuditErrorParser
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.audit.action.StorageAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.audit.resource.StorageAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.permission.StoragePermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManagementDeleteFilesUseCaseTest {

    private val storageManager = mockk<StorageManager>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val commonAuditErrorParser = CommonAuditErrorParser()
    private val auditErrorConverter = AuditErrorConverter(emptySet(), commonAuditErrorParser)
    private val userManager = mockk<UserManager>()

    private val useCase = ManagementDeleteFilesUseCase(
        storageManager = storageManager,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        userManager = userManager
    )

    private val context = createTestAuthenticatedRequestContext()
    private val managementUserId = context.userId
    private val fileId1 = FileId.generate()
    private val fileId2 = FileId.generate()
    private val fileIds = listOf(fileId1, fileId2)

    @Test
    fun `successfully deletes files batch and logs audit records when user is active and authorized`() = runTest {
        val managementUser = createTestUserInternal(
            id = managementUserId,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            permissionCodes = setOf(StoragePermissionCode.STORAGE_DELETE_OTHER_USERS_FILES)
        )

        coEvery { userManager.getUserInternalById(managementUserId) } returns AppResult.Success(managementUser)
        coEvery { storageManager.deleteStorageFilesBatch(fileIds) } returns AppResult.Success(Unit)

        val result = useCase(fileIds, context)

        assertEquals(AppResult.Success(Unit), result)
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = managementUserId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.value,
                action = StorageAuditActionType.MANAGEMENT_DELETE_FILE,
                resource = StorageAuditResourceType.STORAGE_FILE,
                resourceId = fileId1.asHexDashString(),
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
        }
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = managementUserId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.value,
                action = StorageAuditActionType.MANAGEMENT_DELETE_FILE,
                resource = StorageAuditResourceType.STORAGE_FILE,
                resourceId = fileId2.asHexDashString(),
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when management user account status is not active`() = runTest {
        val managementUser = createTestUserInternal(
            id = managementUserId,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.BANNED,
            permissionCodes = setOf(StoragePermissionCode.STORAGE_DELETE_OTHER_USERS_FILES)
        )

        coEvery { userManager.getUserInternalById(managementUserId) } returns AppResult.Success(managementUser)

        val result = useCase(fileIds, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserIllegalAccountStatus)
    }

    @Test
    fun `returns error when user lacks delete permission`() = runTest {
        val managementUser = createTestUserInternal(
            id = managementUserId,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            permissionCodes = emptySet()
        )

        coEvery { userManager.getUserInternalById(managementUserId) } returns AppResult.Success(managementUser)

        val result = useCase(fileIds, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserMissingPermissions)
    }

    @Test
    fun `returns error and logs audit failure when storageManager batch delete fails`() = runTest {
        val managementUser = createTestUserInternal(
            id = managementUserId,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            permissionCodes = setOf(StoragePermissionCode.STORAGE_DELETE_OTHER_USERS_FILES)
        )
        val expectedError = CommonError.Internal(RuntimeException("test"))

        coEvery { userManager.getUserInternalById(managementUserId) } returns AppResult.Success(managementUser)
        coEvery { storageManager.deleteStorageFilesBatch(fileIds) } returns AppResult.Error(expectedError)

        val result = useCase(fileIds, context)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = managementUserId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.value,
                action = StorageAuditActionType.MANAGEMENT_DELETE_FILE,
                resource = StorageAuditResourceType.STORAGE_FILE,
                resourceId = fileId1.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = managementUserId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.ADMIN.value,
                action = StorageAuditActionType.MANAGEMENT_DELETE_FILE,
                resource = StorageAuditResourceType.STORAGE_FILE,
                resourceId = fileId2.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }
}
