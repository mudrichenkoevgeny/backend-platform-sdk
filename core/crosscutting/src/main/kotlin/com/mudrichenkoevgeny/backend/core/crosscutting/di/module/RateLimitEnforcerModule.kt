package com.mudrichenkoevgeny.backend.core.crosscutting.di.module

import com.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import com.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcerImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
interface RateLimitEnforcerModule {

    @Binds
    @Singleton
    fun bindRateLimitEnforcer(rateLimitEnforcerImpl: RateLimitEnforcerImpl): RateLimitEnforcer
}