package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.EmailParser
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.EmailParserImpl
import javax.inject.Singleton

/**
 * Binds the email address parser used by email provider services.
 */
@Module
public interface EmailParserModule {
    @Binds
    @Singleton
    public fun bindEmailParser(emailParserImpl: EmailParserImpl): EmailParser
}
