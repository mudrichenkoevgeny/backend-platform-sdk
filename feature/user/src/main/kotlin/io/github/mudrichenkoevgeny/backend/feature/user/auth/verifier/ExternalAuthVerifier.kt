package io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.auth.model.ExternalAuthProviderData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider

interface ExternalAuthVerifier {
    val provider: UserAuthProvider
    suspend fun verify(token: String): AppResult<ExternalAuthProviderData>
}