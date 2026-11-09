package io.github.mudrichenkoevgeny.backend.core.security.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasherImpl
import javax.inject.Singleton

/**
 * Dagger bindings for password hashing.
 *
 * Binds [PasswordHasher] to [PasswordHasherImpl].
 */
@Module
public interface PasswordHasherModule {
    @Binds
    @Singleton
    public fun bindPasswordHasher(passwordHasherImpl: PasswordHasherImpl): PasswordHasher
}
