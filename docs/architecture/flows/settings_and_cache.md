# Settings & Cache Synchronization

This document illustrates how global system settings are updated and synchronized across all active Ktor instances using Redis Pub/Sub, ensuring high availability and instant configuration reloads.

## 1. Settings Update & Broadcast

When an administrator updates a setting, the change is saved to the database. To avoid stale in-memory caches on other server nodes, a cache invalidation event is broadcast over Redis Pub/Sub.

*Note: The sequence below illustrates `GlobalSettings`. The exact same pattern is utilized for `SecuritySettings` and `AuthSettings`. Each settings domain has its own Use Cases and Caching Providers, but they all rely on Redis to broadcast invalidation events across the cluster.*

```mermaid
sequenceDiagram
    participant Admin
    participant UpdateUseCase as UpdateGlobalSettingsUseCase
    participant DBManager as SystemSettingsManagerImpl
    participant Postgres as DB (Exposed)
    participant Redis as Redis (Pub/Sub)
    participant Cache1 as SystemSettingsServiceImpl (Node 1)
    participant Cache2 as SystemSettingsServiceImpl (Node 2)

    Admin->>UpdateUseCase: PUT /api/v1/management/settings/global (new settings)
    
    %% Save to DB
    UpdateUseCase->>DBManager: saveSettings(settings)
    DBManager->>Postgres: dbQuery { UPSERT system_settings }
    Postgres-->>DBManager: Success
    DBManager-->>UpdateUseCase: Success
    
    %% Broadcast Invalidation
    UpdateUseCase->>Cache1: broadcastSettingsUpdate()
    Cache1->>Redis: publish "system_settings_updates"
    UpdateUseCase-->>Admin: 200 OK
    
    %% Nodes React to Broadcast
    par Node 1 (Local)
        Redis-->>Cache1: receive pub/sub event
        Cache1->>DBManager: getAllSettings()
        DBManager->>Postgres: SELECT * FROM system_settings
        Postgres-->>DBManager: List<SystemSetting>
        DBManager-->>Cache1: Update in-memory ConcurrentHashMap
    and Node 2 (Remote)
        Redis-->>Cache2: receive pub/sub event
        Cache2->>DBManager: getAllSettings()
        DBManager->>Postgres: SELECT * FROM system_settings
        Postgres-->>DBManager: List<SystemSetting>
        DBManager-->>Cache2: Update in-memory ConcurrentHashMap
    end
```
