package io.github.mudrichenkoevgeny.backend.core.common.healthcheck

import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError

/**
 * Single health check that can be used to verify readiness or liveness of a dependency.
 *
 * Implementations perform a lightweight probe (for example, a ping to a database or external service)
 * and return an [AppSystemResult] describing whether the dependency is healthy.
 */
interface HealthCheck {

    /**
     * Indicates whether a failure of this check should be treated as [HealthCheckSeverity.CRITICAL]
     * (process cannot start or must fail fast) or [HealthCheckSeverity.NON_CRITICAL]
     * (reported and logged, but does not prevent serving traffic).
     */
    val severity: HealthCheckSeverity

    /**
     * Executes the health probe.
     *
     * @return [AppSystemResult.Success] when the dependency is healthy, or
     * [AppSystemResult.Error] with a [CommonError.Internal]
     * when the dependency is considered unhealthy.
     */
    suspend fun check(): AppSystemResult<Unit>
}