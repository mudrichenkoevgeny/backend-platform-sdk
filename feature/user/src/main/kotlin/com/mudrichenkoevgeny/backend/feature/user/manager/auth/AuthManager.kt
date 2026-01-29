package io.github.mudrichenkoevgeny.backend.feature.user.manager.auth

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserRole
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthData

interface AuthManager {
    suspend fun provideAuthData(
        userIdentifier: UserIdentifier,
        clientInfo: ClientInfo,
        allowedRoles: Set<UserRole>
    ): AppResult<AuthData>

    suspend fun createUserIdentifier(
        userId: UserId,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String? = null
    ): AppResult<UserIdentifier>

    suspend fun getOrCreateUserIdentifier(
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String? = null,
        userId: UserId? = null,
        userRole: UserRole = UserRole.USER
    ): AppResult<UserIdentifier>
}