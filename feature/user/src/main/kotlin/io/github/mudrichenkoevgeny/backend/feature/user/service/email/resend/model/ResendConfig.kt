package io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model

data class ResendConfig(
    val apiKey: String,
    val url: String,
    val fromEmail: String,
    val fromName: String
) {
    companion object {
        fun createOrNull(
            apiKey: String?,
            url: String?,
            fromEmail: String?,
            fromName: String?
        ): ResendConfig? {
            if (apiKey.isNullOrBlank()
                || url.isNullOrBlank()
                || fromEmail.isNullOrBlank()
                || fromName.isNullOrBlank()
            ) {
                return null
            }
            return ResendConfig(apiKey, url, fromEmail, fromName)
        }
    }
}