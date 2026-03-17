package io.github.mudrichenkoevgeny.backend.core.crosscutting.di

import io.github.mudrichenkoevgeny.backend.core.crosscutting.di.module.RateLimitEnforcerModule
import dagger.Module

/**
 * Dagger aggregation module for cross-cutting infrastructure.
 *
 * Currently, includes:
 * - [RateLimitEnforcerModule] (rate limit enforcement + audit logging)
 */
@Module(
    includes = [
        RateLimitEnforcerModule::class
    ]
)
interface CrosscuttingModules