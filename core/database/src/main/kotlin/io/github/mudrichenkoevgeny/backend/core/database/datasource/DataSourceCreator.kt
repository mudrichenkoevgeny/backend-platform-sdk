package io.github.mudrichenkoevgeny.backend.core.database.datasource

import javax.sql.DataSource

/**
 * Creates a JDBC [DataSource] from connection parameters.
 * Implementations typically use a connection pool (e.g. HikariCP).
 */
interface DataSourceCreator {

    /**
     * Builds a configured [DataSource] for the given JDBC URL and credentials.
     *
     * @param url JDBC URL (e.g. `jdbc:postgresql://host:5432/db`).
     * @param user Database username.
     * @param password Database password.
     * @return configured [DataSource] ready for use.
     */
    fun create(url: String, user: String, password: String): DataSource
}