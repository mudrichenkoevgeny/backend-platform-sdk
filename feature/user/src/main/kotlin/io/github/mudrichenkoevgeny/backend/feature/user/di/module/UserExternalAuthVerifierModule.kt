package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier.ExternalAuthVerifier
import io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier.GoogleAuthVerifier
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.di.qualifiers.GoogleWebClientId
import javax.inject.Singleton

@Module
interface UserExternalAuthVerifierModule {

    @Binds
    @IntoSet
    @Singleton
    fun bindGoogleVerifier(verifier: GoogleAuthVerifier): ExternalAuthVerifier

    companion object {
        @Provides
        @Singleton
        @GoogleWebClientId
        fun provideGoogleWebClientId(userConfig: UserConfig): String? {
            return userConfig.googleWebClientId
        }
    }
}