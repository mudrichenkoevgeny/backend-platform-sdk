package io.github.mudrichenkoevgeny.backend.feature.user.auth.model

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider

/**
 * Result of successful external provider token verification.
 *
 * @param authProvider external provider that issued/validated the token.
 * @param externalId stable subject identifier extracted from the provider token.
 * @param email The user's email address from the provider. **CRITICAL:** This must ONLY be
 *   populated if the external provider explicitly guarantees that the email is verified
 *   (e.g., via `email_verified = true` claim). If the email is unverified, this MUST be `null`.
 */
data class ExternalAuthProviderData(
    val authProvider: UserAuthProvider,
    val externalId: String,
    val email: String?
)