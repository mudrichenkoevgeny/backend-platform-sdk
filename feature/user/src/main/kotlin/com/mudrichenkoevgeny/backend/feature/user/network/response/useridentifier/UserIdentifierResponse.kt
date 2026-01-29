package io.github.mudrichenkoevgeny.backend.feature.user.network.response.useridentifier

import io.github.mudrichenkoevgeny.backend.core.common.network.constants.ApiFields
import io.github.mudrichenkoevgeny.backend.feature.user.network.constants.UserApiFields
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserIdentifierResponse(
    @SerialName(ApiFields.ID)
    val id: String,

    @SerialName(UserApiFields.USER_ID)
    val userId: String,

    @SerialName(UserApiFields.USER_AUTH_PROVIDER)
    val userAuthProvider: String,

    @SerialName(UserApiFields.IDENTIFIER)
    val identifier: String,

    @SerialName(ApiFields.CREATED_AT)
    val createdAt: Long,

    @SerialName(ApiFields.UPDATED_AT)
    val updatedAt: Long? = null
)