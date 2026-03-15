package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.EmailParser
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.EmailParserImpl
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model.EmailParserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model.EmailParserConfigHolder
import javax.inject.Singleton

@Module
interface EmailParserModule {

    @Binds
    @Singleton
    fun bindEmailParser(emailParserImpl: EmailParserImpl): EmailParser

    companion object {
        @Provides
        @Singleton
        fun provideEmailParserConfig(): EmailParserConfig {
            return EmailParserConfigHolder.get()
        }
    }
}