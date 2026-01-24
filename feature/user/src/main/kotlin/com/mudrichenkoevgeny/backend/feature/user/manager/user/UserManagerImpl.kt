package com.mudrichenkoevgeny.backend.feature.user.manager.user

import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.database.util.dbQuery
import com.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import com.mudrichenkoevgeny.backend.feature.user.enums.UserAccountStatus
import com.mudrichenkoevgeny.backend.feature.user.enums.UserRole
import com.mudrichenkoevgeny.backend.feature.user.model.user.User
import com.mudrichenkoevgeny.backend.core.common.model.UserId
import com.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import com.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserManagerImpl @Inject constructor(
    private val userRepository: UserRepository
): UserManager {

    override suspend fun getUserById(userId: UserId): AppResult<User?> = dbQuery {
        userRepository.getUserById(userId)
    }

    override suspend fun createUser(role: UserRole, accountStatus: UserAccountStatus): AppResult<User> = dbQuery {
        val now = Instant.now()

        val user = User(
            id = UserId(UUID.randomUUID()),
            role = role,
            accountStatus = accountStatus,
            lastLoginAt = now,
            lastActiveAt = now,
            createdAt = now,
            updatedAt = null
        )

        userRepository.createUser(user)
    }

    override suspend fun getOrCreateUser(
        userId: UserId?,
        role: UserRole,
        accountStatus: UserAccountStatus
    ): AppResult<User> {
        return if (userId == null) {
            createUser(
                role = role,
                accountStatus = accountStatus
            )
        } else {
            getUserById(userId).mapNotNullOrError(
                UserError.UserNotFound()
            )
        }
    }

    override suspend fun deleteUserById(userId: UserId): AppResult<Unit> = dbQuery {
        userRepository.deleteUser(userId)
    }
}