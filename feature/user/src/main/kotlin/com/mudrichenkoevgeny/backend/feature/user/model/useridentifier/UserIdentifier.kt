package com.mudrichenkoevgeny.backend.feature.user.model.useridentifier

import com.mudrichenkoevgeny.backend.core.common.model.UserId
import com.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import com.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import java.time.Instant

data class UserIdentifier(
    val id: UserIdentifierId,
    val userId: UserId,
    val userAuthProvider: UserAuthProvider,
    val identifier: String,
    val passwordHash: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant?
)