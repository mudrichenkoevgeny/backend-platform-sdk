package io.github.mudrichenkoevgeny.backend.core.common.healthcheck

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Coordinates execution of registered [HealthCheck]s and reports failures.
 *
 * Critical checks are executed during startup via [verifyCriticalHealth]; if any of them fails,
 * the corresponding [CommonError.Internal] is logged and its underlying throwable is rethrown
 * to prevent the service from running in a degraded state. Non‑critical checks are executed
 * by [checkNonCriticalHealth] and only logged.
 */
@Singleton
class HealthCheckerManager @Inject constructor(
    private val healthChecks: Set<@JvmSuppressWildcards HealthCheck>,
    private val appLogger: AppLogger
) {

    /**
     * Runs all [HealthCheck]s with [HealthCheckSeverity.CRITICAL].
     *
     * If any critical check returns [AppSystemResult.Error], logs it and throws the underlying
     * exception, causing startup to fail fast.
     */
    fun verifyCriticalHealth() {
        runBlocking {
            val criticalResult = runCriticalChecks()
            if (criticalResult is AppSystemResult.Error) {
                val systemError = criticalResult.internalError
                appLogger.logError(systemError)
                throw systemError.throwable
            }
        }
    }

    /**
     * Runs all [HealthCheck]s with [HealthCheckSeverity.NON_CRITICAL] and logs any failures.
     */
    suspend fun checkNonCriticalHealth() {
        val nonCriticalErrors = runNonCriticalChecks()
        nonCriticalErrors.forEach { systemError ->
            appLogger.logError(systemError)
        }
    }

    /**
     * Executes all critical checks concurrently and short‑circuits on the first failure.
     */
    private suspend fun runCriticalChecks(): AppSystemResult<Unit> = coroutineScope {
        val criticalChecks = healthChecks.filter { it.severity == HealthCheckSeverity.CRITICAL }
        val deferredList = criticalChecks.map { check ->
            async {
                check.check()
            }
        }

        try {
            deferredList.forEach { deferred ->
                val result = deferred.await()
                if (result is AppSystemResult.Error) {
                    deferredList.forEach { it.cancel() }
                    return@coroutineScope result
                }
            }
        } catch (_: CancellationException) {
        }

        AppSystemResult.Success(Unit)
    }

    /**
     * Executes non‑critical checks concurrently and collects internal errors.
     */
    private suspend fun runNonCriticalChecks(): List<CommonError.Internal> {
        val internalErrors = mutableListOf<CommonError.Internal>()
        coroutineScope {
            healthChecks
                .filter { it.severity == HealthCheckSeverity.NON_CRITICAL }
                .map { healthCheck ->
                    async {
                        val result = healthCheck.check()
                        if (result is AppSystemResult.Error) {
                            internalErrors += result.internalError
                        }
                    }
                }.awaitAll()
        }
        return internalErrors
    }
}