package io.github.mudrichenkoevgeny.backend.core.security.di.module

import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasherImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
interface PasswordHasherModule {

    @Binds
    @Singleton
    fun bindPasswordHasher(passwordHasherImpl: PasswordHasherImpl): PasswordHasher
}