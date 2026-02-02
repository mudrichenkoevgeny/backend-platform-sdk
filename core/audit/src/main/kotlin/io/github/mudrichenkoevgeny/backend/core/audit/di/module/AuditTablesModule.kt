package io.github.mudrichenkoevgeny.backend.core.audit.di.module

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.core.audit.database.table.AuditEventsTable
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable

@Module
class AuditTablesModule {

    @Provides
    @IntoSet
    fun bindAuditEventsTable(): BaseTable = AuditEventsTable
}