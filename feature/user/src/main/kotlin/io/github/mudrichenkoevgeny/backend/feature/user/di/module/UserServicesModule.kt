package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.network.httpclient.HttpClientProvider
import io.github.mudrichenkoevgeny.backend.core.common.network.httpclient.HttpClientSettings
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeServiceImpl
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.EmailParser
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.ResendEmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.unconfigured.UnconfiguredEmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.UniOneEmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.phone.PhoneService
import io.github.mudrichenkoevgeny.backend.feature.user.service.phone.PhoneServiceImpl
import javax.inject.Singleton

/**
 * Binds feature services (phone, email).
 *
 * Email service binding is selected at runtime based on [UserConfig]:
 * - [ResendEmailService] when [UserConfig.resendConfig] is configured
 * - [UniOneEmailService] when [UserConfig.uniOneConfig] is configured
 * - otherwise [UnconfiguredEmailService]
 */
@Module
interface UserServicesModule {

    @Binds
    @Singleton
    fun bindAuthenticationChallengeService(
        authenticationChallengeServiceImpl: AuthenticationChallengeServiceImpl
    ): AuthenticationChallengeService

    @Binds
    @Singleton
    fun bindPhoneService(phoneServiceImpl: PhoneServiceImpl): PhoneService

    companion object {

        @Provides
        @Singleton
        fun provideEmailService(
            userConfig: UserConfig,
            httpClientProvider: HttpClientProvider,
            emailParser: EmailParser,
            unconfiguredEmailService: UnconfiguredEmailService
        ): EmailService {
            return when {
                userConfig.resendConfig != null -> ResendEmailService(
                    config = userConfig.resendConfig,
                    httpClient = httpClientProvider.create(
                        HttpClientSettings(baseUrl = userConfig.resendConfig.url)
                    ),
                    emailParser = emailParser
                )
                userConfig.uniOneConfig != null -> UniOneEmailService(
                    config = userConfig.uniOneConfig,
                    httpClient = httpClientProvider.create(
                        HttpClientSettings(baseUrl = userConfig.uniOneConfig.url)
                    ),
                    emailParser = emailParser
                )
                else -> unconfiguredEmailService
            }
        }
    }
}