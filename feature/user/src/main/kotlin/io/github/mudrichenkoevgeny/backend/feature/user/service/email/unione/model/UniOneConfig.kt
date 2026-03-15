package io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model

data class UniOneConfig(
    val apiKey: String,
    val url: String,
    val fromEmail: String,
    val fromName: String,
    val trackDomain: String,
    val apiSend: String
) {
    companion object {
        fun createOrNull(
            apiKey: String?,
            url: String?,
            fromEmail: String?,
            fromName: String?,
            trackDomain: String?,
            apiSend: String?
        ): UniOneConfig? {
            if (apiKey.isNullOrBlank()
                || url.isNullOrBlank()
                || fromEmail.isNullOrBlank()
                || fromName.isNullOrBlank()
                || trackDomain.isNullOrBlank()
                || apiSend.isNullOrBlank()
            ) {
                return null
            }
            return UniOneConfig(apiKey, url, fromEmail, fromName, trackDomain, apiSend)
        }
    }
}