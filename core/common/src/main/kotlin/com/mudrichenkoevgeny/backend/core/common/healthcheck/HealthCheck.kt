package com.mudrichenkoevgeny.backend.core.common.healthcheck

import com.mudrichenkoevgeny.backend.core.common.result.AppSystemResult

interface HealthCheck {
    val severity: HealthCheckSeverity
    suspend fun check(): AppSystemResult<Unit>
}