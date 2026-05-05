# core/database

Database infrastructure for SDK-based applications: PostgreSQL (HikariCP + Exposed), Flyway migrations, and Redis (Lettuce). This module provides a robust foundation for persistent storage and caching with built-in health monitoring.

## What it provides

- **Exposed Integration:**
    - **Base Table:** [BaseTable] with `id` (UUID), `created_at`, and `updated_at` fields.
    - **Transaction Management:** [dbQuery] utility for running database operations on `Dispatchers.IO`.
    - **Extensions:**
        - [JsonbColumnExtensions]: PostgreSQL-specific JSONB containment checks (`@>`).
        - [QueryPaginationExtensions]: Type-safe [PageParams] application to Exposed queries.
        - [SqlLikePatternExtensions]: Secure SQL `LIKE` pattern building with automatic escaping.
    - **Mappers:** [CommonDatabaseMapper] to bridge domain [SortOrder] with Exposed internals.
- **Migrations:** [FlywayDatabaseMigrator] for automated schema evolution.
- **Redis Manager:** [RedisManagerImpl] provides a thread-safe, asynchronous interface for:
    - Key-value operations with expiration.
    - Atomic increments with TTL via Lua scripts.
    - Pub/Sub messaging with Coroutine-based listeners.
    - Lazy connection management and graceful shutdown.
- **Health Checks:**
    - [DatabaseHealthCheck]: Verifies JDBC connection validity.
    - [RedisHealthCheck]: Verifies Redis availability via PING.
    - Both are bound to the [HealthCheck] set with `CRITICAL` severity.
- **DI Wiring:** [DatabaseModules] aggregates all configuration, database, redis, and health check modules.

## Environment variables

The default config factory reads:

- `DB_URL` — JDBC URL, e.g. `jdbc:postgresql://host:5432/db`.
- `DB_USER_SECRET_FILE` — path to a file containing DB user.
- `DB_PASSWORD_SECRET_FILE` — path to a file containing DB password.
- `MIGRATION_PATHS` — comma-separated Flyway locations (optional; defaults to `classpath:db/migration`).
- `REDIS_URL_SECRET_FILE` — path to a file containing Redis URL (e.g. `redis://localhost:6379`).
- `REDIS_TIMEOUT_SECONDS` — Redis timeout in seconds.

## Usage

### 1. Dependency & DI
Add dependency on `core:database`. In your Dagger component, install **[DatabaseModules]**.
The module requires a `BackgroundScope` ([CoroutineScope]) to be provided in the graph for Redis Pub/Sub operations.

### 2. Database Operations
Use [dbQuery] to execute Exposed DSL operations. Subclass [BaseTable] for standard entity definitions.

### 3. Redis
Inject [RedisManager] to interact with Redis. Use `warmup()` during application startup to initialize connections eagerly if needed.

---

[BaseTable]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/table/BaseTable.kt
[CommonDatabaseMapper]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/mapper/CommonDatabaseMapper.kt
[DatabaseConfig]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/config/model/DatabaseConfig.kt
[DatabaseHealthCheck]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/healthcheck/DatabaseHealthCheck.kt
[DatabaseModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/di/DatabaseModules.kt
[FlywayDatabaseMigrator]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/migrator/FlywayDatabaseMigrator.kt
[HealthCheck]: ../common/src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/healthcheck/HealthCheck.kt
[JsonbColumnExtensions]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/extensions/JsonbColumnExtensions.kt
[PageParams]: ../common/src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/pagination/PageParams.kt
[QueryPaginationExtensions]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/extensions/QueryPaginationExtensions.kt
[RedisHealthCheck]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/healthcheck/RedisHealthCheck.kt
[RedisManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/manager/redis/RedisManager.kt
[RedisManagerImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/manager/redis/RedisManagerImpl.kt
[SqlLikePatternExtensions]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/extensions/SqlLikePatternExtensions.kt
[dbQuery]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/database/util/DatabaseUtils.kt