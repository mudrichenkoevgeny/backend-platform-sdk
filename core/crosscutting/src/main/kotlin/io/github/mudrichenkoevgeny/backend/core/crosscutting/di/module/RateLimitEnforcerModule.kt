package io.github.mudrichenkoevgeny.backend.core.crosscutting.di.module

import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcerImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
interface RateLimitEnforcerModule {

    @Binds
    @Singleton
    fun bindRateLimitEnforcer(rateLimitEnforcerImpl: RateLimitEnforcerImpl): RateLimitEnforcer
}