package io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier.UserIdentifierRepository
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.feature.user.testutil.ExposedTestDb
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserIdentifierManagerImplTest {

    private val passwordHasher: PasswordHasher = mockk()
    private val repository: UserIdentifierRepository = mockk()
    private val manager = IdentifierManagerImpl(
        passwordHasher = passwordHasher,
        userIdentifierRepository = repository
    )

    @Test
    fun `createUserIdentifier hashes password when provided`() = runBlocking {
        ExposedTestDb.initOnce()

        every { passwordHasher.hash(PASSWORD_RAW) } returns AppResult.Success(PASSWORD_HASH)

        val createdSlot = slot<UserIdentifier>()
        coEvery { repository.createUserIdentifier(capture(createdSlot)) } answers {
            AppResult.Success(createdSlot.captured)
        }

        val result = manager.createUserIdentifier(
            userId = USER_ID,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = IDENTIFIER_EMAIL,
            password = PASSWORD_RAW
        )

        assertTrue(result is AppResult.Success)
        val created = (result as AppResult.Success).data
        assertEquals(PASSWORD_HASH, created.passwordHash)
        assertEquals(IDENTIFIER_EMAIL, created.identifier)
        assertEquals(UserAuthProvider.EMAIL, created.userAuthProvider)
        assertEquals(USER_ID, created.userId)

        coVerify(exactly = 1) { repository.createUserIdentifier(any()) }
    }

    @Test
    fun `createUserIdentifier does not hash password when absent`() = runBlocking {
        ExposedTestDb.initOnce()

        val createdSlot = slot<UserIdentifier>()
        coEvery { repository.createUserIdentifier(capture(createdSlot)) } answers {
            AppResult.Success(createdSlot.captured)
        }

        val result = manager.createUserIdentifier(
            userId = USER_ID,
            userAuthProvider = UserAuthProvider.PHONE,
            identifier = IDENTIFIER_PHONE,
            password = null
        )

        assertTrue(result is AppResult.Success)
        val created = (result as AppResult.Success).data
        assertEquals(null, created.passwordHash)

        coVerify(exactly = 0) { passwordHasher.hash(any()) }
    }

    @Test
    fun `updateUserIdentifierPassword hashes new password and delegates to repository`() = runBlocking {
        ExposedTestDb.initOnce()

        val existing = UserIdentifier(
            id = io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId.generate(),
            userId = USER_ID,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = IDENTIFIER_EMAIL,
            passwordHash = null,
            createdAt = java.time.Instant.now(),
            updatedAt = null
        )

        every { passwordHasher.hash(PASSWORD_RAW) } returns AppResult.Success(PASSWORD_HASH)

        coEvery {
            repository.updateUserIdentifier(
                userIdentifier = existing,
                identifier = IDENTIFIER_EMAIL,
                passwordHash = PASSWORD_HASH
            )
        } returns AppResult.Success(existing.copy(passwordHash = PASSWORD_HASH))

        val result = manager.updateUserIdentifierPassword(
            userIdentifier = existing,
            identifier = IDENTIFIER_EMAIL,
            password = PASSWORD_RAW
        )

        assertTrue(result is AppResult.Success)
        assertEquals(PASSWORD_HASH, (result as AppResult.Success).data.passwordHash)
    }

    private companion object {
        val USER_ID: UserId = UserId.generate()

        const val IDENTIFIER_EMAIL = "user@example.com"
        const val IDENTIFIER_PHONE = "+10000000000"

        const val PASSWORD_RAW = "p@ssw0rd"
        const val PASSWORD_HASH = "hash"
    }
}

