package io.github.mudrichenkoevgeny.backend.core.database.di.qualifiers

import javax.inject.Qualifier

/**
 * Qualifier for the Flyway-based DatabaseMigrator implementation.
 * Used when multiple migrator bindings might exist.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DatabaseMigratorFlyway