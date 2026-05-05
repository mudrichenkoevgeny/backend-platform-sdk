package io.github.mudrichenkoevgeny.backend.core.security.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.security.aescryptor.AesCryptor
import io.github.mudrichenkoevgeny.backend.core.security.aescryptor.AesCryptorImpl
import javax.inject.Singleton

/**
 * Dagger bindings for AES encryption and decryption.
 *
 * Binds [AesCryptor] to [AesCryptorImpl] for secure data persistence.
 */
@Module
interface AesCryptorModule {

    @Binds
    @Singleton
    fun bindAesCryptor(aesCryptorImpl: AesCryptorImpl): AesCryptor
}