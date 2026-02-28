package io.github.mudrichenkoevgeny.backend.feature.user.model.auth

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.ExternalAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import kotlinx.serialization.Serializable

@Serializable
data class AvailableAuthProviders(
    val primary: List<UserAuthProvider>,
    val secondary: List<UserAuthProvider>
) {
    val supportedExternalProviders: Set<ExternalAuthProvider>
        get() {
            val activeTypes = primary + secondary

            // todo wait for shared-foundation update
            return setOf(
                ExternalAuthProvider.Google,
                ExternalAuthProvider.Apple
            ).filter { it.userAuthProvider in activeTypes }.toSet()
        }
}
