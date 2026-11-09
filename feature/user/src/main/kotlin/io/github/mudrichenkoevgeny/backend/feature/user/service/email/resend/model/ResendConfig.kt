package io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model

/**
 * Resend provider configuration.
 *
 * @property apiKey provider API key
 * @property url base API url
 * @property fromEmail sender email
 * @property fromName sender display name
 */
public data class ResendConfig(
    public val apiKey: String,
    public val url: String,
    public val fromEmail: String,
    public val fromName: String
) {
    public companion object {
        /**
         * Creates a [ResendConfig] only when all required parameters are present.
         *
         * @return config instance or `null` when any parameter is blank
         */
        public fun createOrNull(
            apiKey: String?,
            url: String?,
            fromEmail: String?,
            fromName: String?
        ): ResendConfig? {
            if (apiKey.isNullOrBlank() ||
                url.isNullOrBlank() ||
                fromEmail.isNullOrBlank() ||
                fromName.isNullOrBlank()
            ) {
                return null
            }
            return ResendConfig(
                apiKey,
                url,
                fromEmail,
                fromName
            )
        }
    }
}
