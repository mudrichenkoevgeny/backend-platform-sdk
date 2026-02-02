package io.github.mudrichenkoevgeny.backend.feature.user.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    @SerialName(ROLE_USER)
    USER,

    @SerialName(ROLE_MODERATOR)
    MODERATOR,

    @SerialName(ROLE_ADMIN)
    ADMIN;

    val serialName: String
        get() = when (this) {
            USER -> ROLE_USER
            MODERATOR -> ROLE_MODERATOR
            ADMIN -> ROLE_ADMIN
        }

    companion object {
        const val ROLE_USER = "user"
        const val ROLE_MODERATOR = "moderator"
        const val ROLE_ADMIN = "admin"

        fun fromValue(value: String): UserRole? =
            runCatching { valueOf(value.uppercase()) }.getOrNull()
    }
}