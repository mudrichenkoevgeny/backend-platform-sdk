package io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier

interface UserIdentifierManager {
    suspend fun getUserIdentifier(
        userAuthProvider: UserAuthProvider,
        identifier: String,
    ): AppResult<UserIdentifier?>

    suspend fun getUserIdentifierListByUserId(
        userId: UserId
    ): AppResult<List<UserIdentifier>>

    suspend fun createUserIdentifier(
        userId: UserId,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String? = null
    ): AppResult<UserIdentifier>

    suspend fun deleteUserIdentifier(
        userIdentifierId: UserIdentifierId
    ): AppResult<Unit>

    suspend fun updateUserIdentifierPassword(
        userIdentifier: UserIdentifier,
        identifier: String,
        password: String
    ): AppResult<UserIdentifier>
}