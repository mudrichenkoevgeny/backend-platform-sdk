package io.github.mudrichenkoevgeny.backend.core.database.di

import io.github.mudrichenkoevgeny.backend.core.database.datasource.DataSourceCreator
import io.github.mudrichenkoevgeny.backend.core.database.di.qualifiers.DriverClassName
import io.github.mudrichenkoevgeny.backend.core.database.manager.database.DatabaseManager
import dagger.Component
import javax.inject.Singleton

// todo do we need that ?
@Singleton
@Component(modules = [DatabaseModule::class, TestDatabaseDriverModule::class])
interface TestDatabaseComponent {
    @DriverClassName
    fun driverClassName(): String

    fun dataSourceCreator(): DataSourceCreator
    fun databaseFactory(): DatabaseManager
}