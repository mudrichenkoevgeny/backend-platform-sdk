package io.github.mudrichenkoevgeny.backend.core.database.datasource

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.database.di.qualifiers.DriverClassName
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import javax.inject.Inject
import javax.inject.Singleton
import javax.sql.DataSource

@Singleton
class HikariDatasourceCreator @Inject constructor(
    @param:DriverClassName private val hikariDriverClassName: String,
    private val prometheusMeterRegistry: PrometheusMeterRegistry,
    private val appLogger: AppLogger
): DataSourceCreator {

    override fun create(url: String, user: String, password: String): DataSource {
        return try {
            val config = HikariConfig().apply {
                jdbcUrl = url
                driverClassName = hikariDriverClassName
                username = user
                this.password = password

                maximumPoolSize = 10
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"

                validate()
            }
            val dataSource = HikariDataSource(config)
            dataSource.metricsTrackerFactory = MicrometerMetricsTrackerFactory(prometheusMeterRegistry)

            dataSource
        } catch (t: Throwable) {
            appLogger.logError(CommonError.System(t))
            throw t
        }
    }
}