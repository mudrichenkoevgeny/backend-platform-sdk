# Scheduled Background Jobs

This document outlines the scheduled jobs that run autonomously on the `BackgroundScope` to perform system maintenance and enforce security lifecycle policies.

## 1. Periodic Maintenance (UserScheduledJobs)

The `UserScheduledJobs` component starts an infinite coroutine loop (with a configurable delay) upon application startup. It delegates maintenance tasks to the `UserManager` and `SessionManager`.

```mermaid
sequenceDiagram
    participant App as Application.module()
    participant Jobs as UserScheduledJobs
    participant UserManager
    participant SessionManager
    participant Postgres as DB (Exposed)

    App->>Jobs: start() (Triggered by ApplicationStarted event)
    
    loop Configured Interval (e.g., via background coroutine delay)
        Note over Jobs: Sleep / Delay
        
        %% 1. Clean Expired Sessions
        Jobs->>SessionManager: deleteExpiredSessionsForSystem()
        SessionManager->>Postgres: dbQuery { DELETE FROM user_sessions WHERE expires_at < NOW() }
        Postgres-->>SessionManager: Affected rows
        
        %% 2. Process Pending Deletions
        Jobs->>UserManager: deleteUsersDueForPermanentDeletionForSystem()
        UserManager->>Postgres: dbQuery { SELECT users WHERE status = PENDING_DELETION & grace_period_expired }
        Postgres-->>UserManager: Users to delete
        opt If users exist
            UserManager->>Postgres: dbQuery { DELETE FROM users ... CASCADE }
        end
        
        %% 3. Unlock Expired Lockouts
        Jobs->>UserManager: unlockExpiredAccountLockoutsForSystem()
        UserManager->>Postgres: dbQuery { UPDATE users SET status = ACTIVE WHERE status = SECURITY_HOLD & lockout_until < NOW() }
        Postgres-->>UserManager: Affected rows
    end
```
