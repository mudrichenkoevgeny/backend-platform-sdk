package io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier.UserIdentifierRepository
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserIdentifierManagerImpl @Inject constructor(
    private val passwordHasher: PasswordHasher,
    private val userIdentifierRepository: UserIdentifierRepository
): UserIdentifierManager {

    override suspend fun getUserIdentifier(
        userAuthProvider: UserAuthProvider,
        identifier: String
    ): AppResult<UserIdentifier?> = dbQuery {
        userIdentifierRepository.getUserIdentifier(
            userAuthProvider = userAuthProvider,
            identifier = identifier
        )
    }

    override suspend fun getUserIdentifierListByUserId(userId: UserId): AppResult<List<UserIdentifier>> = dbQuery {
        userIdentifierRepository.getUserIdentifiersListByUserId(
            userId = userId
        )
    }

    override suspend fun createUserIdentifier(
        userId: UserId,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String?
    ): AppResult<UserIdentifier> = dbQuery {
        val passwordHash = password?.let { password ->
            val passwordHashResult = passwordHasher.hash(password)

            when (passwordHashResult) {
                is AppResult.Success -> passwordHashResult.data
                is AppResult.Error -> return@dbQuery passwordHashResult
            }
        }

        val userIdentifier = UserIdentifier(
            id = UserIdentifierId(UUID.randomUUID()),
            userId = userId,
            userAuthProvider = userAuthProvider,
            identifier = identifier,
            passwordHash = passwordHash,
            createdAt = Instant.now(),
            updatedAt = null
        )

        userIdentifierRepository.createUserIdentifier(userIdentifier)
    }

    override suspend fun deleteUserIdentifier(userIdentifierId: UserIdentifierId): AppResult<Unit> = dbQuery {
        userIdentifierRepository.deleteUserIdentifier(userIdentifierId)
    }

    override suspend fun updateUserIdentifierPassword(
        userIdentifier: UserIdentifier,
        identifier: String,
        password: String
    ): AppResult<UserIdentifier> = dbQuery {
        val passwordHashResult = passwordHasher.hash(password)

        val passwordHash = when (passwordHashResult) {
            is AppResult.Success -> passwordHashResult.data
            is AppResult.Error -> return@dbQuery passwordHashResult
        }

        userIdentifierRepository.updateUserIdentifier(
            userIdentifier = userIdentifier,
            identifier = identifier,
            passwordHash = passwordHash
        )
    }
}