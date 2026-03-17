package io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model

/**
 * Resend provider configuration.
 *
 * @property apiKey provider API key
 * @property url base API url
 * @property fromEmail sender email
 * @property fromName sender display name
 */
data class ResendConfig(
    val apiKey: String,
    val url: String,
    val fromEmail: String,
    val fromName: String
) {
    companion object {
        /**
         * Creates a [ResendConfig] only when all required parameters are present.
         *
         * @return config instance or `null` when any parameter is blank
         */
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