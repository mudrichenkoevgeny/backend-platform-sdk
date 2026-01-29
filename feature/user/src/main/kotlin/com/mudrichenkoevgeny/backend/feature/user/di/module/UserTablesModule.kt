package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserIdentifiersTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserSessionsTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
class UserTablesModule {

    @Provides
    @IntoSet
    fun bindUserSessionsTable(): BaseTable = UserSessionsTable

    @Provides
    @IntoSet
    fun provideUsersTable(): BaseTable = UsersTable

    @Provides
    @IntoSet
    fun provideUserIdentifiersTable(): BaseTable = UserIdentifiersTable
}