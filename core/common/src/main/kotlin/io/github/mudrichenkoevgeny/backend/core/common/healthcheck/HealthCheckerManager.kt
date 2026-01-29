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

@Singleton
class HealthCheckerManager @Inject constructor(
    private val healthChecks: Set<@JvmSuppressWildcards HealthCheck>,
    private val appLogger: AppLogger
) {
    fun verifyCriticalHealth() {
        runBlocking {
            val criticalResult = runCriticalChecks()
            if (criticalResult is AppSystemResult.Error) {
                val systemError = criticalResult.systemError
                appLogger.logError(systemError)
                throw systemError.throwable
            }
        }
    }

    suspend fun checkNonCriticalHealth() {
        val nonCriticalErrors = runNonCriticalChecks()
        nonCriticalErrors.forEach { systemError ->
            appLogger.logError(systemError)
        }
    }

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
        } catch (_: CancellationException) { }

        AppSystemResult.Success(Unit)
    }

    private suspend fun runNonCriticalChecks(): List<CommonError.System> {
        val systemErrors = mutableListOf<CommonError.System>()
        coroutineScope {
            healthChecks
                .filter { it.severity == HealthCheckSeverity.NON_CRITICAL }
                .map { healthCheck ->
                    async {
                        val result = healthCheck.check()
                        if (result is AppSystemResult.Error) {
                            systemErrors += result.systemError
                        }
                    }
                }.awaitAll()
        }
        return systemErrors
    }
}