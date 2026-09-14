package io.github.mudrichenkoevgeny.backend.core.security.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.security.lockout.LockoutManager
import io.github.mudrichenkoevgeny.backend.core.security.lockout.LockoutManagerImpl
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaService
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaServiceImpl
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpServiceImpl
import javax.inject.Singleton

/**
 * Binds core security services for MultiFactor Authentication, One-Time Passwords, and Lockout Management.
 *
 * These services provide the foundation for identity verification and secure
 * sensitive operations across the application.
 */
@Module
interface SecurityServicesModule {

    @Binds
    @Singleton
    fun bindOtpService(otpServiceImpl: OtpServiceImpl): OtpService

    @Binds
    @Singleton
    fun bindMfaService(mfaServiceImpl: MfaServiceImpl): MfaService

    @Binds
    @Singleton
    fun bindLockoutManager(lockoutManagerImpl: LockoutManagerImpl): LockoutManager
}