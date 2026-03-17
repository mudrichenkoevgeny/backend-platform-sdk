package io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.auth.model.ExternalAuthProviderData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider

/**
 * Verifies a token issued by an external authentication provider (e.g. Google) and extracts the
 * provider identity data used by the user feature.
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