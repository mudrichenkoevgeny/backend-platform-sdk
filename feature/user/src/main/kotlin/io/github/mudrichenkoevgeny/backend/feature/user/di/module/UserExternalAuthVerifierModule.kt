package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier.ExternalAuthVerifier
import io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier.GoogleAuthVerifier
import javax.inject.Singleton

@Module
interface UserExternalAuthVerifierModule {

    @Binds
    @IntoSet
    @Singleton
    fun bindGoogleVerifier(verifier: GoogleAuthVerifier): ExternalAuthVerifier
}