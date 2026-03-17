package io.github.mudrichenkoevgeny.backend.feature.user.manager.user

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User
import io.github.mudrichenkoevgeny.backend.feature.user.testutil.ExposedTestDb
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserRole
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserManagerImplTest {

    private val userRepository: UserRepository = mockk()
    private val manager = UserManagerImpl(userRepository = userRepository)

    @Test
    fun `createUser delegates to repository with generated id`() = runBlocking {
        ExposedTestDb.initOnce()

        val userSlot = slot<User>()
        coEvery { userRepository.createUser(capture(userSlot)) } answers {
            AppResult.Success(userSlot.captured)
        }

        val result = manager.createUser(role = UserRole.ADMIN, accountStatus = UserAccountStatus.ACTIVE)

        assertTrue(result is AppResult.Success)
        val created = (result as AppResult.Success).data
        assertEquals(UserRole.ADMIN, created.role)
        assertEquals(UserAccountStatus.ACTIVE, created.accountStatus)
        assertTrue(created.id.value.toString().isNotBlank())
    }

    @Test
    fun `getOrCreateUser returns UserNotFound when repository returns null`() = runBlocking {
        ExposedTestDb.initOnce()

        val userId = UserId.generate()
        coEvery { userRepository.getUserById(userId) } returns AppResult.Success(null)

        val result = manager.getOrCreateUser(userId = userId)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserNotFound)
        coVerify(exactly = 1) { userRepository.getUserById(userId) }
    }

    @Test
    fun `deleteUserById delegates to repository`() = runBlocking {
        ExposedTestDb.initOnce()

        val userId = UserId.generate()
        coEvery { userRepository.deleteUser(userId) } returns AppResult.Success(Unit)

        val result = manager.deleteUserById(userId)

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) { userRepository.deleteUser(userId) }
    }
}

