package com.mudrichenkoevgeny.backend.feature.user.di.module

import com.mudrichenkoevgeny.backend.core.database.table.BaseTable
import com.mudrichenkoevgeny.backend.feature.user.database.table.UserIdentifiersTable
import com.mudrichenkoevgeny.backend.feature.user.database.table.UserSessionsTable
import com.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
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