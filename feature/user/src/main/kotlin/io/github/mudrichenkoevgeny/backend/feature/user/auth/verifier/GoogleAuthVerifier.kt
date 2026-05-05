package io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier

import com.google.auth.oauth2.TokenVerifier
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.auth.model.ExternalAuthProviderData
import io.github.mudrichenkoevgeny.backend.feature.user.di.qualifiers.GoogleWebClientId
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ExternalAuthVerifier] implementation for Google Sign-In tokens.
 *
 * Uses [TokenVerifier] configured with an optional web client id from [GoogleWebClientId]. If the
 * verifier cannot be built (missing client id) or the token is invalid,
 * returns [UserError.ExternalIdentifierLinkageFailed].
 */
@Singleton
class GoogleAuthVerifier @Inject constructor(
    @param:GoogleWebClientId private val webClientId: String?
) : ExternalAuthVerifier {
    override val provider = UserAuthProvider.GOOGLE

    private val verifier by lazy {
        webClientId?.let {
            TokenVerifier.newBuilder().setAudience(it).build()
        }
    }

    override suspend fun verify(token: String): AppResult<ExternalAuthProviderData> {
        val authVerifier = verifier
            ?: return AppResult.Error(UserError.ExternalIdentifierLinkageFailed())

        return withContext(Dispatchers.IO) {
            try {
                val jwt = authVerifier.verify(token)
                val payload = jwt.payload
                val externalId = payload.subject
                val isEmailVerified = payload.get("email_verified") as? Boolean ?: false
                val email = if (isEmailVerified) {
                    payload.get("email") as? String
                } else {
                    null
                }

                if (externalId == null) {
                    AppResult.Error(UserError.ExternalIdentifierLinkageFailed())
                } else {
                    AppResult.Success(
                        ExternalAuthProviderData(
                            authProvider = provider,
                            externalId = externalId,
                            email = email
                        )
                    )
                }
            } catch (e: Exception) {
                AppResult.Error(UserError.ExternalIdentifierLinkageFailed(e.message))
            }
        }
    }
}