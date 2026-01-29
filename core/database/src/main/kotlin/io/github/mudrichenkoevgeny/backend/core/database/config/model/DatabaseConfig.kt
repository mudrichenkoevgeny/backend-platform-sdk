package io.github.mudrichenkoevgeny.backend.core.database.config.model

data class DatabaseConfig(
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val migrationPaths: List<String> = defaultMigrationPaths,
    val redisUrl: String,
    val redisTimeoutSeconds: Long
) {

    companion object {
        val defaultMigrationPaths = listOf("classpath:db/migration")
    }
}