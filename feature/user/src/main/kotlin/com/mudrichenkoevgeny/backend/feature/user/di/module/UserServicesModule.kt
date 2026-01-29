package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailServiceImpl
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.OtpServiceImpl
import io.github.mudrichenkoevgeny.backend.feature.user.service.phone.PhoneService
import io.github.mudrichenkoevgeny.backend.feature.user.service.phone.PhoneServiceImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
interface UserServicesModule {

    @Binds
    @Singleton
    fun bindOtpService(otpServiceImpl: OtpServiceImpl): OtpService

    @Binds
    @Singleton
    fun bindEmailService(emailServiceImpl: EmailServiceImpl): EmailService

    @Binds
    @Singleton
    fun bindPhoneService(phoneServiceImpl: PhoneServiceImpl): PhoneService
}