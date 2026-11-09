package io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model

/**
 * UniOne provider configuration.
 *
 * @property apiKey provider API key
 * @property url base API url
 * @property fromEmail sender email
 * @property fromName sender display name
 * @property trackDomain provider tracking domain
 * @property apiSend API path for sending emails (relative to [url])
 */
public data class UniOneConfig(
    public val apiKey: String,
    public val url: String,
    public val fromEmail: String,
    public val fromName: String,
    public val trackDomain: String,
    public val apiSend: String
) {
    public companion object {
        /**
         * Creates a [UniOneConfig] only when all required parameters are present.
         *
         * @return config instance or `null` when any parameter is blank
         */
        public fun createOrNull(
            apiKey: String?,
            url: String?,
            fromEmail: String?,
            fromName: String?,
            trackDomain: String?,
            apiSend: String?
        ): UniOneConfig? {
            if (apiKey.isNullOrBlank() ||
                url.isNullOrBlank() ||
                fromEmail.isNullOrBlank() ||
                fromName.isNullOrBlank() ||
                trackDomain.isNullOrBlank() ||
                apiSend.isNullOrBlank()
            ) {
                return null
            }
            return UniOneConfig(
                apiKey,
                url,
                fromEmail,
                fromName,
                trackDomain,
                apiSend
            )
        }
    }
}
