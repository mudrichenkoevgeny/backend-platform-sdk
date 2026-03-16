package io.github.mudrichenkoevgeny.backend.core.common.util

import org.h2.jdbcx.JdbcDataSource
import javax.sql.DataSource

/**
 * Creates an in-memory H2 DataSource for tests (PostgreSQL compatibility mode).
 * Intended for use in test code; the calling module must provide the H2 dependency (e.g. testImplementation).
 * Each call uses a unique DB name so test classes can use isolated instances.
 *
 * @param namePrefix Prefix for the in-memory database name; defaults to "test".
 * @return A [DataSource] backed by H2 in-memory with PostgreSQL mode.
 */
fun createTestDataSource(namePrefix: String = "test"): DataSource =
    JdbcDataSource().apply {
        setURL(
            "jdbc:h2:mem:${namePrefix}_${System.nanoTime()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
        )
        user = "sa"
        password = ""
    }
