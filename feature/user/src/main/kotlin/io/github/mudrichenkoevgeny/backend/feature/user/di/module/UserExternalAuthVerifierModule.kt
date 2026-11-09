package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier.ExternalAuthVerifier
import io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier.GoogleAuthVerifier
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.di.qualifiers.GoogleWebClientId
import javax.inject.Singleton

/**
 * Binds external auth verifiers used by the feature (e.g. Google).
 *
 * Uses a Dagger set multibinding for [ExternalAuthVerifier] implementations and provides provider
 * specific configuration values such as [GoogleWebClientId].
 */
@Module
public interface UserExternalAuthVerifierModule {
    @Binds
    @IntoSet
    @Singleton
    public fun bindGoogleVerifier(verifier: GoogleAuthVerifier): ExternalAuthVerifier

    @Multibinds
    public fun bindExternalAuthVerifiers(): Set<ExternalAuthVerifier>

    public companion object {
        @Provides
        @Singleton
        @GoogleWebClientId
        public fun provideGoogleWebClientId(userConfig: UserConfig): String? = userConfig.googleWebClientId
    }
}
