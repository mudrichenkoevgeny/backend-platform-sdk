---
description: SDK bootstrap lifecycle, Flyway migration paths, error argument pools, and core abstractions
globs: "**/*.kt"
alwaysApply: true
---

# Architecture and Infrastructure Standards — backend-platform-sdk

This document defines internal bootstrapping invariants, storage subpaths, and error modeling specific to the `backend-platform-sdk` repository.

## 1. SDK Server Bootstrap & Graceful Shutdown
- **Server Bootstrap:** Ktor instances are created using `KtorServer.create(commonConfig)`. Consuming applications provide the root Dagger module to bind application-specific routers and services into the SDK graph.
- **Graceful Shutdown:** Configured via `KTOR_SHUTDOWN_GRACE_PERIOD_MS` and `KTOR_SHUTDOWN_TIMEOUT_MS`. `AppShutdownHookImpl` handles releasing database pools and Redis connections during `ApplicationStopping`.

## 2. Exposed Tables & Flyway Paths
- **Migration Subpaths:** Flyway scripts for SDK modules must reside under:
  - Core modules: `db/migration/core/<module>/`
  - Feature modules: `db/migration/feature/<feature>/`
- **Table KDoc Contract:** Every Exposed table definition must state:
  `Schema is created by a Flyway migration in [db/migration/<module>/<feature>/].`
  `The host app must include this path in its Flyway migration locations.`

## 3. Error Modeling, Argument Pools & Masking
- **StatusPages Pipeline:** Centralized exception handling maps exceptions to HTTP envelopes using `AppErrorParser` in `core/common`.
- **Result Envelopes:** Use cases return `[AppResult.Success]` on success and map domain failures to `[AppError]`.
- **Argument Pools:**
  - `publicArgs`: Parameters destined for client-side localized string formatting.
  - `secretArgs`: Internal diagnostic values strictly forbidden from being exposed to clients.
  - Every domain error variant must document which constructor parameters populate `publicArgs` versus `secretArgs`.
- **Data Masking:** Sensitive data (PII, credentials, tokens) in logs and audit events must be processed using redaction utilities from `core/common`.

## 4. Packaging Prefix
- Base namespace: `io.github.mudrichenkoevgeny.backend.[module].[layer]`.
