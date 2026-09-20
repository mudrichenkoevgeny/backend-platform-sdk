# Backend Platform SDK Architecture

This document describes the high-level architecture and module dependency graph of the `backend-platform-sdk`.

For detailed domain models and sequence diagrams of specific runtime flows, please refer to the specialized documentation in the `docs/architecture` directory:

- **[Identity & Access Domain Models](docs/architecture/identity_and_access.md)** — Core models: Roles, Permissions, Account Statuses (with State Machine), Identifiers, and Sessions.
- **[Authentication Flows](docs/architecture/flows/authentication.md)** — Registration, Email, Phone (SMS OTP), External Providers (OAuth), and Token Refresh workflows.
- **[Account Management Flows](docs/architecture/flows/account_management_flows.md)** — Schedule Account Deletion and Restoration workflows.
- **[Security Flows](docs/architecture/flows/security.md)** — TOTP MFA Initialization, Brute-Force Protection, and Lockout mechanisms.
- **[Settings & Cache Synchronization](docs/architecture/flows/settings_and_cache.md)** — Global settings updates and multi-node Redis cache invalidation.
- **[WebSocket Synchronization](docs/architecture/flows/websockets.md)** — Stateless real-time broadcasting via Redis Pub/Sub.
- **[Audit & Masking](docs/architecture/flows/audit.md)** — Asynchronous event logging, error parsing, and PII masking.
- **[Scheduled Background Jobs](docs/architecture/flows/background_jobs.md)** — Periodic cleanup of expired sessions, permanent user deletions, and unbanning accounts.
- **[Events & Storage Infrastructure](docs/architecture/flows/events_and_storage.md)** — Kafka event pub/sub and Blob storage (S3/Local) abstractions.

---

## 1. High-Level Architecture

The platform is designed as a modular Ktor microservice architecture. It exposes multiple HTTP and WebSocket connectors (split by Open and Management APIs) and delegates to isolated features (`feature:*`) backed by reusable infrastructure modules (`core:*`). State is persisted in PostgreSQL, cached/synchronized via Redis, and background events are published to a Kafka message broker.

```mermaid
flowchart TD
    %% Clients
    Client[Mobile/Web Clients]
    Admin[Management / Admin]

    subgraph Platform API [Ktor Application]
        direction TB
        subgraph Endpoints [Ktor Routing]
            direction LR
            OpenAPI[Public Connector\n:8080]
            MgmtAPI[Management Connector\n:8081]
            WS[WebSocket Endpoints]
        end

        subgraph Feature Modules
            UserFeature[feature:user\nIAM, Auth, MFA, Sessions]
        end

        subgraph Core Modules
            Security[core:security\nArgon2, AES, MFA]
            Settings[core:settings\nSystem Settings]
            Audit[core:audit\nAudit Logger]
            Database[core:database\nExposed, Redis]
            Events[core:events\nKafka Publisher]
            Storage[core:storage\nS3 / Local]
            Observability[core:observability\nOpenTelemetry]
        end

        %% Routing to Features
        OpenAPI --> UserFeature
        MgmtAPI --> UserFeature
        WS --> UserFeature

        %% Features to Core Dependencies
        UserFeature --> Security
        UserFeature --> Settings
        UserFeature --> Audit
        UserFeature --> Database
        UserFeature --> Events
        UserFeature --> Observability
    end

    %% External Infrastructure
    subgraph Infrastructure [Data Stores & Brokers]
        PostgreSQL[(PostgreSQL\nExposed ORM)]
        Redis[(Redis\nLettuce)]
        Kafka[[Apache Kafka\nEvent Bus]]
        S3[(S3 / MinIO\nBlob Storage)]
    end

    %% Interactions
    Client -->|HTTPS| OpenAPI
    Client -->|WSS| WS
    Admin -->|HTTPS| MgmtAPI
    Admin -->|WSS| WS

    Database -.-> PostgreSQL
    Database -.->|Rate Limiting / Caching\nPubSub Sync| Redis
    Events -.-> Kafka
    Storage -.-> S3
```

---

## 2. Module Dependency Graph

The project utilizes a clear layered approach. `core:common` is the base for all modules. `feature:user` aggregates the domain logic, security constraints, and database transactions before being assembled by the `sample` host application.

```mermaid
graph TD
    %% App
    Sample["sample"]

    %% Features
    FeatureUser["feature:user"]

    %% Core
    CoreSecurity["core:security"]
    CoreAudit["core:audit"]
    CoreSettings["core:settings"]
    CoreDatabase["core:database"]
    CoreEvents["core:events"]
    CoreStorage["core:storage"]
    CoreObservability["core:observability"]
    CoreCommon["core:common"]

    %% Sample App Dependencies
    Sample --> FeatureUser
    Sample --> CoreEvents
    Sample --> CoreStorage
    Sample --> CoreObservability
    Sample --> CoreSettings
    Sample --> CoreSecurity
    Sample --> CoreAudit
    Sample --> CoreDatabase
    Sample --> CoreCommon

    %% Feature User Dependencies
    FeatureUser --> CoreSecurity
    FeatureUser --> CoreSettings
    FeatureUser --> CoreAudit
    FeatureUser --> CoreDatabase
    FeatureUser --> CoreCommon

    %% Core Security Dependencies
    CoreSecurity --> CoreSettings
    CoreSecurity --> CoreAudit
    CoreSecurity --> CoreDatabase
    CoreSecurity --> CoreCommon

    %% Settings & Audit
    CoreSettings --> CoreDatabase
    CoreSettings --> CoreCommon
    CoreAudit --> CoreDatabase
    CoreAudit --> CoreCommon

    %% Infrastructure
    CoreDatabase --> CoreObservability
    CoreDatabase --> CoreCommon
    CoreEvents --> CoreCommon
    CoreStorage --> CoreCommon
    CoreObservability --> CoreCommon
```
