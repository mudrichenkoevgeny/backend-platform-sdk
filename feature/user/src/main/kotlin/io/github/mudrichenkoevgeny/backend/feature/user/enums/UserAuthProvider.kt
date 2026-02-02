package io.github.mudrichenkoevgeny.backend.feature.user.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserAuthProvider {
    @SerialName(PROVIDER_EMAIL)
    EMAIL,

    @SerialName(PROVIDER_PHONE)
    PHONE,

    @SerialName(PROVIDER_GOOGLE)
    GOOGLE,

    @SerialName(PROVIDER_APPLE)
    APPLE;

    val serialName: String
        get() = when (this) {
            EMAIL -> PROVIDER_EMAIL
            PHONE -> PROVIDER_PHONE
            GOOGLE -> PROVIDER_GOOGLE
            APPLE -> PROVIDER_APPLE
        }

    companion object {
        const val PROVIDER_EMAIL = "email"
        const val PROVIDER_PHONE = "phone"
        const val PROVIDER_GOOGLE = "google"
        const val PROVIDER_APPLE = "apple"

        fun fromValue(value: String): UserAuthProvider? = runCatching { valueOf(value.uppercase()) }.getOrNull()
    }
}