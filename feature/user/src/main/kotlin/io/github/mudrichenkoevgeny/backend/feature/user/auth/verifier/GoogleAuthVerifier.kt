package io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier

import com.google.auth.oauth2.TokenVerifier
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.auth.model.ExternalAuthProviderData
import io.github.mudrichenkoevgeny.backend.feature.user.di.qualifiers.GoogleWebClientId
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ExternalAuthVerifier] implementation for Google Sign-In tokens.
 *
 * Uses [TokenVerifier] configured with an optional web client id from [GoogleWebClientId]. If the
 * verifier cannot be built (missing client id) or the token is invalid, returns [UserError.ExternalIdMismatch].
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
            ?: return AppResult.Error(UserError.ExternalIdMismatch())

        return withContext(Dispatchers.IO) {
            try {
                val jwt = authVerifier.verify(token)
                val externalId = jwt.payload.subject

                if (externalId == null) {
                    AppResult.Error(UserError.ExternalIdMismatch())
                } else {
                    AppResult.Success(
                        ExternalAuthProviderData(
                            authProvider = provider,
                            externalId = externalId
                        )
                    )
                }
            } catch (e: Exception) {
                AppResult.Error(UserError.ExternalIdMismatch(e))
            }
        }
    }
}