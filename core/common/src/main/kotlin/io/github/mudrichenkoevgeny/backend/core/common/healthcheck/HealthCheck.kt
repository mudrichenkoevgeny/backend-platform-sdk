package io.github.mudrichenkoevgeny.backend.core.common.healthcheck

import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult

interface HealthCheck {
    val severity: HealthCheckSeverity
    suspend fun check(): AppSystemResult<Unit>
}