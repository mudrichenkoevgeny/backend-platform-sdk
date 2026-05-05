# feature/user

Advanced Identity & Access Management (IAM) and Authentication module. This module provides a complete infrastructure for user lifecycle, session management, and multi-factor security.

## Architecture & Layers

The module follows a strict layered architecture to ensure separation of concerns:

### 1. Managers (Business Logic Orchestration)
Managers act as the primary coordination layer between Use Cases and multiple Repositories or Services.
*   **UserManager**: Core user profile and status management.
*   **AuthManager**: High-level authentication logic, including login and registration flows.
*   **SessionManager**: Lifecycle of user sessions (creation, validation, termination).
*   **IdentifierManager**: Management of user identities (Email, Phone, OAuth).
*   **TotpManager**: Security orchestration for 2FA/TOTP.
*   **WebSocketManager**: Real-time connection management and session-aware broadcasting.

### 2. Repositories (Data Abstraction)
Standardized interfaces for data access, allowing for easy testing and storage swapping.
*   **UserRepository**: Operations on the core user profile.
*   **UserIdentifierRepository**: Mapping between users and their identifiers (Email, Phone, etc.).
*   **UserSessionRepository**: Storage and retrieval of opaque refresh tokens and session metadata.
*   **UserTotpSettingsRepository**: Persistence for encrypted 2FA secrets and recovery codes.

### 3. Database & Tables (Persistence)
Built on top of Exposed, defining the schema and constraints.
*   **UsersTable**: Primary user records with role and status information.
*   **UserIdentifiersTable**: Linking users to multiple login methods.
*   **UserSessionsTable**: Session tracking with support for expiration and revocation.
*   **UserTotpSettingsTable**: Secure storage for TOTP-related data.
*   **UserDbConstraints**: Centralized definition of database-level constraints (e.g., uniqueness of identifiers).

### 4. Authentication Infrastructure
*   **Verifiers**: `GoogleAuthVerifier` and general `ExternalAuthVerifier` for OAuth2 validation.
*   **Security Providers**: `JwtTokenProvider` for issuing tokens and `RefreshTokenProvider` for secure hash-based session storage.
*   **Challenge Service**: `AuthenticationChallengeService` for step-up authentication.

## Use Case Catalog

### 1. Management (Privileged Operations)
*   **User Admin**: `ManagementCreateUser`, `ManagementUpdateUser`, `ManagementDeleteUser`, `ManagementGetUsers`, `ManagementGetUser`.
*   **Security Admin**: `ManagementDisableTotp` (Force reset).
*   **Session Control**: `ManagementGetSessions`, `ManagementGetSession`, `ManagementDeleteSession`, `ManagementDeleteAllSessions`.
*   **Identifier Control**: `ManagementGetIdentifiers`, `ManagementGetIdentifier`, `ManagementDeleteIdentifier`.
*   **System Settings**: `GetManagementAuthSettings`, `UpdateAuthSettings`.

### 2. Open (Client & Self-Service)
*   **Auth & Lifecycle**:
    *   **Login**: `LoginByEmail`, `LoginByPhone`, `LoginByExternalAuthProvider`, `LoginByTotp`, `LoginByTotpRecoveryCode`.
    *   **Registration**: `RegisterByEmail`, `SendRegistrationConfirmationToEmail`.
    *   **Recovery**: `ResetPassword`, `SendResetPasswordConfirmation`.
    *   **Session**: `RefreshToken`, `Logout`, `GetSessions`, `DeleteSession`, `DeleteAllOtherSessions`, `ReauthenticateSession` (Step-up).
*   **Security (2FA/TOTP)**: `SetupTotp`, `EnableTotp`, `DisableTotp`, `GetRecoveryCodes`, `RegenerateRecoveryCodes`.
*   **Profile**: `GetUser`, `RestoreUser`, `ScheduleUserDeletion`.
*   **Identifiers**: `AddUserIdentifierEmail`, `AddUserIdentifierPhone`, `AddUserIdentifierExternalAuthProvider`, `DeleteUserIdentifier`, `GetIdentifiers`, `IdentifierEmailChangePassword`.
*   **Configuration**: `GetUserConfiguration`, `GetAuthSettings`.

## System Initialization & Seeding
The module includes system-level use cases for bootstrapping the environment:
*   **Admin Seeding**: `SeedAdminAccountsUseCase` automatically creates initial admin accounts based on `AdminList` configuration.
*   **Auth Settings Seeding**: `SeedAuthSettingsUseCase` ensures default security policies are present in the database.

---

## Usage

### 1. Dependency & DI
The module is highly modularized via Dagger. Depending on your needs, you can install the full `UserModules` or granular modules:
*   `UserRepositoriesModule`, `UserManagersModule`, `UserServicesModule`, `UserConfigModule`, etc.

**Requirements**: The app must provide a `BackgroundScope` for `UserScheduledJobs` (e.g., account deletion cleanup).

### 2. Registration (Ktor)
Register the routers in your Ktor application:

```kotlin
routing {
    // Public/Client routes
    val openRouter = appComponent.openCoreUserRouter()
    openRouter.register(this)

    // Admin/Management routes
    val managementRouter = appComponent.managementCoreUserRouter()
    managementRouter.register(this)

    // Real-time WebSockets
    val wsRouter = appComponent.authenticatedWebSocketRouter()
    wsRouter.register(this)
}
```

### 3. Configuration & Localization
*   **Environment**: Uses `UserEnvKeys` for JWT secrets, token validity, and 3rd-party provider keys.
*   **Config Factory**: `UserConfigFactoryImpl` builds the configuration object from environment and system properties.
*   **Localization**: Supports localized error messages and email templates via `UserAuditErrorParser` and `EmailParser`.

---

## Architecture Overview

The module follows a strict **Clean Architecture** approach:
*   **Routers**: Handle HTTP/WS entry points and delegate to Use Cases.
*   **Use Cases**: Atomic business operations (e.g., [LoginByEmailUseCase], [ScheduleUserDeletionUseCase]).
*   **Managers**: Orchestrate complex logic across multiple repositories.
*   **Security Providers**: Encapsulate JWT logic and hashing.

---

[UserManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/manager/user/UserManager.kt
[AuthManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/manager/auth/AuthManager.kt
[SessionManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/manager/session/SessionManager.kt
[IdentifierManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/manager/identifier/IdentifierManager.kt
[TotpManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/manager/totp/TotpManager.kt
[UserRepository]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/database/repository/user/UserRepository.kt
[UserIdentifierRepository]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/database/repository/useridentifier/UserIdentifierRepository.kt
[UserSessionRepository]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/database/repository/usersession/UserSessionRepository.kt
[UserTotpSettingsRepository]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/database/repository/usertotpsettings/UserTotpSettingsRepository.kt
[UsersTable]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/database/table/UsersTable.kt
[UserIdentifiersTable]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/database/table/UserIdentifiersTable.kt
[UserSessionsTable]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/database/table/UserSessionsTable.kt
[UserTotpSettingsTable]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/database/table/UserTotpSettingsTable.kt
[UserDbConstraints]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/database/UserDbConstraints.kt
[OpenCoreUserRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/route/open/OpenCoreUserRouter.kt
[ManagementCoreUserRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/route/management/ManagementCoreUserRouter.kt
[AuthenticatedWebSocketRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/network/websocket/router/AuthenticatedWebSocketRouter.kt
[JwtTokenProvider]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/security/tokenprovider/JwtTokenProvider.kt
[RefreshTokenProvider]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/security/refreshtokenprovider/RefreshTokenProvider.kt
[JwtAuthenticationProvider]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/security/authenticationprovider/JwtAuthenticationProvider.kt
[AuthenticationChallengeService]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/service/authenticationchallenge/AuthenticationChallengeService.kt
[EmailParser]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/service/email/parser/EmailParser.kt
[ResendEmailService]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/service/email/resend/ResendEmailService.kt
[UniOneEmailService]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/service/email/unione/UniOneEmailService.kt
[PhoneService]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/service/phone/PhoneService.kt
[UserScheduledJobs]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/scheduled/UserScheduledJobs.kt
[UserModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/di/UserModules.kt
[UserEnvKeys]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/config/envkeys/UserEnvKeys.kt
[UserConfigFactoryImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/config/factory/UserConfigFactoryImpl.kt
[UserAuditErrorParser]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/audit/error/UserAuditErrorParser.kt
[OpenCoreUserRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/route/open/OpenCoreUserRouter.kt
[ManagementCoreUserRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/route/management/ManagementCoreUserRouter.kt
[WebSocketManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/network/websocket/manager/WebSocketManager.kt