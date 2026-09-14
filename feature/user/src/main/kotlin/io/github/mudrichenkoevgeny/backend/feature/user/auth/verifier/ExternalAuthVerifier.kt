package io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.auth.model.ExternalAuthProviderData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider

/**
 * Verifies a token issued by an external authentication provider (e.g. Google) and extracts the
 * provider identity data used by the user feature.
 *
 * **Security Requirement:** Implementations MUST strictly enforce email verification checks.
 * If the external provider includes an email address but does not explicitly confirm that it is
 * verified by the user, the implementation MUST discard the email (set it to `null` in
 * [ExternalAuthProviderData]) to prevent Account Takeover (ATO) via implicit account linking.
 */
interface ExternalAuthVerifier {
    /**
     * Provider supported by this verifier implementation.
     */
    val provider: UserAuthProvider

    /**
     * Verifies the provided token and returns extracted provider identity data.
     *
     * @param token provider-issued token (typically an ID token / JWT)
     * @return [AppResult.Success] with [ExternalAuthProviderData] when verified; otherwise [AppResult.Error]
     */
    suspend fun verify(token: String): AppResult<ExternalAuthProviderData>
}