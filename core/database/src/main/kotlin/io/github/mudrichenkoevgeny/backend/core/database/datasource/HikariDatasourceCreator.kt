package io.github.mudrichenkoevgeny.backend.core.database.datasource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import com.zaxxer.hikari.pool.HikariPool
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.database.di.qualifiers.DriverClassName
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import javax.inject.Inject
import javax.inject.Singleton
import javax.sql.DataSource

/**
 * Default [DataSourceCreator] implementation using HikariCP.
 *
 * Builds a [HikariDataSource] with fixed pool size, repeatable-read isolation, auto-commit off,
 * and Micrometer metrics via [PrometheusMeterRegistry]. On creation failure logs via [AppLogger]
 * and rethrows.
 */
@Singleton
public class HikariDatasourceCreator @Inject constructor(
    @param:DriverClassName private val hikariDriverClassName: String,
    private val prometheusMeterRegistry: PrometheusMeterRegistry,
    private val appLogger: AppLogger
) : DataSourceCreator {
    override fun create(
        url: String,
        user: String,
        password: String
    ): DataSource = try {
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
    } catch (e: IllegalArgumentException) {
        appLogger.logError(CommonError.Internal(e))
        throw e
    } catch (e: IllegalStateException) {
        appLogger.logError(CommonError.Internal(e))
        throw e
    } catch (e: HikariPool.PoolInitializationException) {
        appLogger.logError(CommonError.Internal(e))
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: RuntimeException) {
        appLogger.logError(CommonError.Internal(e))
        throw e
    }
}
