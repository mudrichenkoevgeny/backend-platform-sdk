# Account Management Flows

This document outlines the workflows for account deletion and restoration within the `backend-platform-sdk`.

*For detailed information on User Roles, Statuses, Identifiers, and Sessions, see the [Identity & Access Domain Models](../identity_and_access.md).*

## 1. Schedule Account Deletion (MFA Step-up)

Demonstrates a destructive action that requires MFA confirmation (Step-up authentication) if TOTP is enabled on the account. It transitions the `UserAccountStatus` to `PENDING_DELETION`.

```mermaid
sequenceDiagram
    participant Client
    participant DeleteUseCase as ScheduleUserDeletionUseCase
    participant Challenge as AuthenticationChallengeService
    participant AuthManager
    participant Session as SessionManager
    participant Postgres as DB (Exposed)

    Client->>DeleteUseCase: POST /api/v1/user/delete
    DeleteUseCase->>Challenge: assertMfaVerification(userId)
    
    Challenge->>Postgres: dbQuery { getUserById }
    Postgres-->>Challenge: User details
    
    alt User has TOTP enabled & missing Step-up Token
        Challenge-->>DeleteUseCase: Error(MfaConfirmationRequired)
        DeleteUseCase-->>Client: 401 Unauthorized (mfaToken for STEP_UP)
    else Step-up Token Valid (or MFA disabled)
        Challenge-->>DeleteUseCase: Success
        
        DeleteUseCase->>Postgres: dbQuery { lock account -> PENDING_DELETION }
        Postgres-->>DeleteUseCase: Updated UserDetails
        
        DeleteUseCase->>Session: deleteAllUserSessions(userId)
        Session->>Postgres: dbQuery { delete sessions }
        
        DeleteUseCase-->>Client: 200 OK
    end
```

## 2. Restore User Account

If a user logs back in during the grace period (while in `PENDING_DELETION`), they are prompted to restore their account, which aborts the scheduled deletion.

```mermaid
sequenceDiagram
    participant Client
    participant RestoreUseCase as RestoreUserUseCase
    participant UserManager as UserManagerImpl
    participant Postgres as DB (Exposed)

    Client->>RestoreUseCase: POST /api/v1/user/restore
    RestoreUseCase->>UserManager: restoreUser(userId)
    
    UserManager->>Postgres: dbQuery { get user }
    Postgres-->>UserManager: UserDetails (status = PENDING_DELETION)
    
    UserManager->>Postgres: dbQuery { update status -> ACTIVE }
    Postgres-->>UserManager: Updated UserDetails
    
    UserManager-->>RestoreUseCase: Success
    RestoreUseCase-->>Client: 200 OK (UserDetails restored)
```
