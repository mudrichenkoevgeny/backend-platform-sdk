package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier

import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PagedResponse
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId

interface UserIdentifierRepository {
    suspend fun createUserIdentifier(userIdentifier: UserIdentifier): AppResult<UserIdentifier>
    suspend fun deleteUserIdentifier(userIdentifierId: UserIdentifierId): AppResult<Unit>
    suspend fun deleteAllUserIdentifierByUserId(userId: UserId): AppResult<Unit>

    suspend fun updateUserIdentifier(
        userIdentifier: UserIdentifier,
        identifier: String? = null,
        passwordHash: String? = null
    ): AppResult<UserIdentifier>

    suspend fun getUserIdentifierById(userIdentifierId: UserIdentifierId): AppResult<UserIdentifier?>
    suspend fun getUserIdentifiersListByUserId(
        userId: UserId,
        userAuthProvider: UserAuthProvider? = null
    ): AppResult<List<UserIdentifier>>
    suspend fun getUserIdentifier(
        userAuthProvider: UserAuthProvider,
        identifier: String,             // email, phoneNumber, externalId
    ): AppResult<UserIdentifier?>
    suspend fun getUserIdentifiersList(
        params: PageParams,
        userAuthProvider: UserAuthProvider? = null
    ): AppResult<PagedResponse<UserIdentifier>>
}