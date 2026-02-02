package io.github.mudrichenkoevgeny.backend.feature.user.di.mapkeys

import dagger.MapKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.enums.UserAuthProvider

@MapKey
annotation class AuthProviderKey(val value: UserAuthProvider)