package io.github.mudrichenkoevgeny.backend.core.database.di

import io.github.mudrichenkoevgeny.backend.core.database.di.qualifiers.DriverClassName
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class TestDatabaseDriverModule {

    @Provides
    @Singleton
    @DriverClassName
    fun provideDataSourceDriverClassName(): String = "org.h2.Driver"
}