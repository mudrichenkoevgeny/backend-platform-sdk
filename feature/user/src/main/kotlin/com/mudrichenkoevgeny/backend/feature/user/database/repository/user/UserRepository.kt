package com.mudrichenkoevgeny.backend.feature.user.database.repository.user

import com.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import com.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PagedResponse
import com.mudrichenkoevgeny.backend.feature.user.enums.UserAccountStatus
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.feature.user.enums.UserRole
import com.mudrichenkoevgeny.backend.feature.user.model.user.User
import com.mudrichenkoevgeny.backend.core.common.model.UserId
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