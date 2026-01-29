package io.github.mudrichenkoevgeny.backend.feature.user.manager.user

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAccountStatus
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserRole
import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId

interface UserManager {
    suspend fun getUserById(
        userId: UserId
    ): AppResult<User?>

    suspend fun createUser(
        role: UserRole = UserRole.USER,
        accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE
    ): AppResult<User>

    suspend fun getOrCreateUser(
        userId: UserId? = null,
        role: UserRole = UserRole.USER,
        accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE
    ): AppResult<User>

    suspend fun deleteUserById(
        userId: UserId
    ): AppResult<Unit>
}