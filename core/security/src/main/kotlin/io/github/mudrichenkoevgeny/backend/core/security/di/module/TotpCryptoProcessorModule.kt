package io.github.mudrichenkoevgeny.backend.core.security.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.security.totpcryptoprocessor.TotpCryptoProcessor
import io.github.mudrichenkoevgeny.backend.core.security.totpcryptoprocessor.TotpCryptoProcessorImpl
import javax.inject.Singleton

/**
 * Dagger bindings for TOTP cryptography processing.
 *
 * Binds [TotpCryptoProcessor] to [TotpCryptoProcessorImpl].
 */
@Module
public interface TotpCryptoProcessorModule {
    @Binds
    @Singleton
    public fun bindTotpCryptoProcessor(totpCryptoProcessorImpl: TotpCryptoProcessorImpl): TotpCryptoProcessor
}
