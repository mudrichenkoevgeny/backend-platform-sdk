package io.github.mudrichenkoevgeny.backend.core.database.di.qualifiers

import javax.inject.Qualifier

/**
 * Qualifier for the JDBC driver class name (e.g. `org.postgresql.Driver`) used by DataSourceCreator.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DriverClassName