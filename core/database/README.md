# core/database

Database infrastructure for SDK-based applications: PostgreSQL (HikariCP + Exposed), Flyway migrations, and Redis (Lettuce).

## What it provides

- **Config**: [DatabaseConfig] built by [DatabaseConfigFactory] from env via [DatabaseEnvKeys].
- **PostgreSQL**:
  - Hikari DataSource creation (see [DataSourceCreator] / [HikariDatasourceCreator]).
  - Exposed database initialization and helpers.
  - Flyway migration support (see [DatabaseMigrator] / [FlywayDatabaseMigrator]).
- **Redis**:
  - Lettuce client creation (see [RedisClientCreator] / [LettuceRedisClientCreator]).
  - Redis factory + manager abstractions ([RedisFactory], [RedisManager]).
- **Health checks**: DB and Redis health checks bound into the [HealthCheck] set.
- **DI wiring**: [DatabaseModules] aggregates config + database + redis + health check modules.

## Environment variables

The default config factory ([DatabaseConfigFactoryImpl]) reads:

- `DB_URL` — JDBC URL, e.g. `jdbc:postgresql://host:5432/db`.
- `DB_USER_SECRET_FILE` — path to a file containing DB user.
- `DB_PASSWORD_SECRET_FILE` — path to a file containing DB password.
- `MIGRATION_PATHS` — comma-separated Flyway locations, e.g. `classpath:db/migration,classpath:app/migration` (optional; defaults to [DatabaseConfig.defaultMigrationPaths]).
- `REDIS_URL_SECRET_FILE` — path to a file containing Redis URL (e.g. `redis://localhost:6379`).
- `REDIS_TIMEOUT_SECONDS` — Redis timeout in seconds.

See: [DatabaseEnvKeys].

## Usage

- Add dependency on `core:database`. Depends on `core:common` and `core:observability`.
- Install [DatabaseModules] in your Dagger component.
- Provide required config via env + secret files; inject [DatabaseManager] / [RedisManager] where needed.

### Minimal wiring (Dagger)

Include [DatabaseModules] in your application component, then inject [DatabaseConfig], [DatabaseManager], [RedisManager], etc.

### Migrations

[FlywayDatabaseMigrator] uses [DatabaseConfig.migrationPaths]. Place migration scripts under the configured classpath locations
(default: `classpath:db/migration`).

[DatabaseConfig]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/config/model/DatabaseConfig.kt
[DatabaseEnvKeys]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/config/envkeys/DatabaseEnvKeys.kt
[DatabaseConfigFactory]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/config/factory/DatabaseConfigFactory.kt
[DatabaseConfigFactoryImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/config/factory/DatabaseConfigFactoryImpl.kt

[DatabaseModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/di/DatabaseModules.kt
[HealthCheck]: ../common/src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/healthcheck/HealthCheck.kt

[DataSourceCreator]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/datasource/DataSourceCreator.kt
[HikariDatasourceCreator]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/datasource/HikariDatasourceCreator.kt

[DatabaseMigrator]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/migrator/DatabaseMigrator.kt
[FlywayDatabaseMigrator]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/migrator/FlywayDatabaseMigrator.kt

[RedisClientCreator]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/redisclient/RedisClientCreator.kt
[LettuceRedisClientCreator]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/redisclient/LettuceRedisClientCreator.kt
[RedisFactory]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/factory/redis/RedisFactory.kt
[RedisManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/manager/redis/RedisManager.kt
[DatabaseManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/manager/database/DatabaseManager.kt

