package io.github.mudrichenkoevgeny.backend.core.database.datasource

import javax.sql.DataSource

interface DataSourceCreator {
    fun create(url: String, user: String, password: String): DataSource
}