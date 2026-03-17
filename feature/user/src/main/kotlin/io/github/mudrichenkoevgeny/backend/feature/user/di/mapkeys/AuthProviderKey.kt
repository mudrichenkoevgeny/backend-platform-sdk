package io.github.mudrichenkoevgeny.backend.feature.user.di.mapkeys

import dagger.MapKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider

@MapKey
/**
 * Map key for binding provider-specific auth implementations keyed by [UserAuthProvider].
 *
 * Used for Dagger multibindings where the selected auth provider is resolved at runtime.
 */
annotation class AuthProviderKey(val value: UserAuthProvider)