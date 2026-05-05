package io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier

import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier.UserIdentifierRepository
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordhash.PasswordHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.IdentifierPermissionCode
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class IdentifierManagerImplTest {

    private val passwordHasher = mockk<PasswordHasher>()
    private val userManager = mockk<UserManager>()
    private val repository = mockk<UserIdentifierRepository>()
    private val manager = IdentifierManagerImpl(passwordHasher, userManager, repository)

    private val userId = UserId.generate()
    private val managementUserId = UserId.generate()

    @BeforeEach
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
    }

    @Test
    fun `getUserIdentifierByIdForManagement returns UNMASKED when user has permission`() = runTest {
        val identifierId = UserIdentifierId.generate()
        val identifier = createSampleIdentifier(identifierId, userId, "test@test.com")
        val targetUser = createSampleUserDetails(userId, UserRole.USER)

        coEvery { repository.getUserIdentifierById(identifierId) } returns AppResult.Success(identifier)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(targetUser)

        val permissions = setOf(IdentifierPermissionCode.IDENTIFIER_GET_OF_USER_UNMASKED)
        val result = manager.getUserIdentifierByIdForManagement(identifierId, managementUserId, permissions)

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals("test@test.com", data?.identifier)
        assertEquals(false, data?.isSensitiveValuesMasked)
    }

    @Test
    fun `getUserIdentifierByIdForManagement returns MASKED when user has only masked permission`() = runTest {
        val identifierId = UserIdentifierId.generate()
        val rawEmail = "secret@example.com"
        val identifier = createSampleIdentifier(identifierId, userId, rawEmail)
        val targetUser = createSampleUserDetails(userId, UserRole.USER)

        coEvery { repository.getUserIdentifierById(identifierId) } returns AppResult.Success(identifier)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(targetUser)

        val permissions = setOf(IdentifierPermissionCode.IDENTIFIER_GET_OF_USER_MASKED)
        val result = manager.getUserIdentifierByIdForManagement(identifierId, managementUserId, permissions)

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(DataMasker.maskEmail(rawEmail), data?.identifier)
        assertEquals(true, data?.isSensitiveValuesMasked)
    }

    @Test
    fun `getUserIdentifierByIdForManagement returns Error when target is ADMIN`() = runTest {
        val identifierId = UserIdentifierId.generate()
        val identifier = createSampleIdentifier(identifierId, userId)
        val targetUser = createSampleUserDetails(userId, UserRole.ADMIN)

        coEvery { repository.getUserIdentifierById(identifierId) } returns AppResult.Success(identifier)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(targetUser)

        val permissions = setOf(IdentifierPermissionCode.IDENTIFIER_GET_OF_USER_UNMASKED)
        val result = manager.getUserIdentifierByIdForManagement(identifierId, managementUserId, permissions)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserMissingPermissions)
    }

    @Test
    fun `createUserIdentifier hashes password before saving`() = runTest {
        val rawPassword = "plain_password"
        val hashedPassword = PasswordHash("hashed_password")

        coEvery { passwordHasher.hash(rawPassword) } returns AppResult.Success(hashedPassword)
        coEvery { repository.createUserIdentifier(any()) } answers { AppResult.Success(firstArg()) }

        val result = manager.createUserIdentifier(
            userId = userId,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = "new@test.com",
            password = rawPassword
        )

        assertTrue(result is AppResult.Success)
        assertEquals(hashedPassword, (result as AppResult.Success).data.passwordHash)
    }

    @Test
    fun `getIdentifiersPageForManagement filters and masks items correctly`() = runTest {
        val user1Id = UserId.generate()
        val user2Id = UserId.generate()

        val iden1 = createSampleIdentifier(UserIdentifierId.generate(), user1Id, "user@test.com")
        val iden2 = createSampleIdentifier(UserIdentifierId.generate(), user2Id, "staff@test.com")

        val pagedResult = PagedResult(
            items = listOf(iden1, iden2),
            totalCount = 2,
            pageNumber = 1,
            pageSize = 10,
            totalPages = 1
        )

        coEvery {
            repository.getUserIdentifiersPageWithAccessFilter(any(), any(), any(), any(), any(), any(), any())
        } returns AppResult.Success(pagedResult)
        coEvery {
            userManager.getUserByIdForSelf(user1Id)
        } returns AppResult.Success(createSampleUserDetails(user1Id, UserRole.USER))
        coEvery {
            userManager.getUserByIdForSelf(user2Id)
        } returns AppResult.Success(createSampleUserDetails(user2Id, UserRole.STAFF))

        val permissions = setOf(IdentifierPermissionCode.IDENTIFIER_GET_OF_USER_UNMASKED)

        val result = manager.getIdentifiersPageForManagement(
            managementUserPermissionCodes = permissions,
            pageParams = PageParams(1, 10),
            sortBy = UserSortValues.UserIdentifierSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
            userIds = emptyList(),
            userAuthProviders = emptyList(),
            identifiers = emptyList()
        )

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(1, data.items.size)
        assertEquals("user@test.com", data.items.first().identifier)
    }

    private fun createSampleIdentifier(id: UserIdentifierId, uId: UserId, value: String = "test") = UserIdentifier(
        id = id,
        userId = uId,
        userAuthProvider = UserAuthProvider.EMAIL,
        identifier = value,
        externalProviderEmail = null,
        isSensitiveValuesMasked = false,
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    private fun createSampleUserDetails(uId: UserId, role: UserRole) = UserDetails(
        id = uId,
        role = role,
        accountStatus = UserAccountStatus.ACTIVE,
        accountStatusBeforeDeletion = null,
        authorityLevel = 1,
        permissionCodes = emptySet(),
        isTotpEnabled = false,
        createdAt = Clock.System.now()
    )
}