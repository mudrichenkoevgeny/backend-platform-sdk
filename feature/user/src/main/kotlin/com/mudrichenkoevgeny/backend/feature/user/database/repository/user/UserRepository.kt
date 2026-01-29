package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user

import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PagedResponse
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAccountStatus
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserRole
import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import java.time.Instant

interface UserRepository {
    suspend fun createUser(user: User): AppResult<User>
    suspend fun deleteUser(userId: UserId): AppResult<Unit>

    suspend fun updateUser(
        user: User,
        status: UserAccountStatus? = null,
        lastLoginAt: Instant? = null,
        lastActiveAt: Instant? = null
    ): AppResult<User>

    suspend fun getUserById(userId: UserId): AppResult<User?>
    suspend fun getUsersList(
        params: PageParams,
        role: UserRole? = null,
        accountStatus: UserAccountStatus? = null,
    ): AppResult<PagedResponse<User>>
}