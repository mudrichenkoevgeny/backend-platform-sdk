package io.github.mudrichenkoevgeny.backend.feature.user.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserAccountStatus {
    @SerialName(STATUS_ACTIVE)
    ACTIVE,

    @SerialName(STATUS_READ_ONLY)
    READ_ONLY,

    @SerialName(STATUS_BANNED)
    BANNED;

    val serialName: String
        get() = when (this) {
            ACTIVE -> STATUS_ACTIVE
            READ_ONLY -> STATUS_READ_ONLY
            BANNED -> STATUS_BANNED
        }

    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_READ_ONLY = "read_only"
        const val STATUS_BANNED = "banned"

        fun fromValue(value: String): UserAccountStatus? =
            runCatching { valueOf(value.uppercase()) }.getOrNull()
    }
}