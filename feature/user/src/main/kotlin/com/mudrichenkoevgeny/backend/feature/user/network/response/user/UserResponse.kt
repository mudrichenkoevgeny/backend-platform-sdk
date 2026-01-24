package com.mudrichenkoevgeny.backend.feature.user.network.response.user

import com.mudrichenkoevgeny.backend.core.common.network.constants.ApiFields
import com.mudrichenkoevgeny.backend.feature.user.network.constants.UserApiFields
import com.mudrichenkoevgeny.backend.feature.user.network.response.useridentifier.UserIdentifierResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    @SerialName(ApiFields.ID)
    val id: String,

    @SerialName(UserApiFields.ROLE)
    val role: String,

    @SerialName(UserApiFields.ACCOUNT_STATUS)
    val accountStatus: String,

    @SerialName(UserApiFields.USER_IDENTIFIERS)
    val userIdentifiers: List<UserIdentifierResponse>,

    @SerialName(UserApiFields.LAST_LOGIN_AT)
    val lastLoginAt: Long? = null,

    @SerialName(UserApiFields.LAST_ACTIVE_AT)
    val lastActiveAt: Long? = null,

    @SerialName(ApiFields.CREATED_AT)
    val createdAt: Long,

    @SerialName(ApiFields.UPDATED_AT)
    val updatedAt: Long? = null
)