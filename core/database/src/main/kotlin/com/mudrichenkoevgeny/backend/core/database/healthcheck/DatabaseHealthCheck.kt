package com.mudrichenkoevgeny.backend.core.database.healthcheck

import com.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import com.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheck
import com.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheckSeverity
import com.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import javax.sql.DataSource

@Singleton
class DatabaseHealthCheck @Inject constructor(
    private val dataSource: DataSource
) : HealthCheck {

    override val severity: HealthCheckSeverity = HealthCheckSeverity.CRITICAL

    override suspend fun check(): AppSystemResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    if (connection.isValid(1)) {
                        AppSystemResult.Success(Unit)
                    } else {
                        throw RuntimeException("Database connection is invalid")
                    }
                }
            } catch (t: Throwable) {
                AppSystemResult.Error(CommonError.System(t))
            }
        }
    }
}