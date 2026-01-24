package com.mudrichenkoevgeny.backend.feature.user.manager.auth

import com.mudrichenkoevgeny.backend.core.common.model.UserId
import com.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import com.mudrichenkoevgeny.backend.feature.user.enums.UserRole
import com.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import com.mudrichenkoevgeny.backend.feature.user.model.auth.AuthData

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