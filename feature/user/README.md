# feature/user

User & authentication feature for SDK-based applications: JWT auth, login/registration flows, user profile,
sessions, user identifiers, external auth providers, and real-time WebSockets integration.

## What it provides

- **HTTP routes**:
  - Public (no JWT): auth flows and public configuration endpoints.
  - Protected (JWT required): user profile, sessions, and security endpoints.
  - Entry-point router: [UserFeatureRouter] (registers sub-routers and applies `authenticate("jwt")`).
- **JWT authentication integration**:
  - Ktor `Authentication` setup with HMAC verifier based on `UserConfig.jwtSecret`.
  - Token extraction from `Authorization: Bearer ...` or query parameter `token`.
  - User existence validation and session last-access update (when session id is present).
  - See: [AuthenticationProvider] / [JwtAuthenticationProvider] and [JwtAuthSpecs].
- **Auth settings**: available auth providers exposed via API (see [AuthSettingsRouter], [AuthSettingsProvider]).
- **External auth**:
  - Extensible verifier abstraction [ExternalAuthVerifier] with Google implementation [GoogleAuthVerifier].
  - Provider payload model: [ExternalAuthProviderData].
- **Email confirmations**: UniOne and Resend provider configs (created when required env/secret values exist).
- **Audit**: user-related audit metadata + logger integration (see [UserAuditLogger]).
- **WebSockets**:
  - JWT-protected (optional) real-time endpoint at `WebSocketContract.WS_REALTIME_PATH`
    (`io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.WebSocketContract`).
  - Entry-point router: [AuthenticatedWebSocketRouter].
  - Message handler + session listener contributions via [UserWebSocketModule].
- **Database schema**: Flyway migrations under `db/migration/feature/user/`.
- **Localization resources**: `localization/{en,ru}/error_messages.json` and `email_messages.json`.

## Environment variables

The default config factory ([UserConfigFactoryImpl]) reads:

- `JWT_SECRET_FILE` — path to a file containing JWT secret used for signing/verifying tokens.
- `ACCESS_TOKEN_VALIDITY_HOURS` — access token validity in hours.
- `REFRESH_TOKEN_VALIDITY_DAYS` — refresh token validity in days.
- `AUTH_REALM` — Ktor JWT realm.
- `ADMIN_ACCOUNTS_JSON_SECRET_FILE` — path to a JSON file with initial admin accounts to seed.
- `AVAILABLE_AUTH_PROVIDERS_PRIMARY` — comma-separated list of primary auth providers (values of `UserAuthProvider`).
- `AVAILABLE_AUTH_PROVIDERS_SECONDARY` — comma-separated list of secondary auth providers (values of `UserAuthProvider`).
- `GOOGLE_WEB_CLIENT_ID` — Google web client id used to verify Google tokens.
- `UNIONE_API_KEY_FILE` — path to UniOne API key secret file.
- `UNIONE_URL` — UniOne base URL.
- `UNIONE_FROM_EMAIL` — UniOne sender email.
- `UNIONE_FROM_NAME` — UniOne sender name.
- `UNIONE_TRACK_DOMAIN` — UniOne tracking domain.
- `UNIONE_API_SEND` — UniOne send endpoint path/URL.
- `RESEND_API_KEY_FILE` — path to Resend API key secret file.
- `RESEND_URL` — Resend base URL.
- `RESEND_FROM_EMAIL` — Resend sender email.
- `RESEND_FROM_NAME` — Resend sender name.

See: [UserEnvKeys].

## Usage

- Add dependency on `feature:user`. It depends on `core:common`, `core:database`, `core:security`, `core:audit`,
  `core:settings`, and `core:crosscutting`.
- Install [UserModules] in your Dagger application component (or include submodules selectively).
- Configure Ktor authentication using the injected [AuthenticationProvider] (typically once at startup).
- Register routers in your `routing { }` block:
  - [UserFeatureRouter] for HTTP routes.
  - [AuthenticatedWebSocketRouter] for real-time WebSocket endpoint.

### Minimal wiring (Ktor)

```kotlin
fun Application.module(appComponent: AppComponent) {
    appComponent.authenticationProvider().configureAuthentication(this)

    routing {
        appComponent.userFeatureRouter().register(this)
        appComponent.authenticatedWebSocketRouter().register(this)
    }
}
```

### Migrations

This module ships Flyway migrations in:

- `classpath:db/migration/feature/user`

Ensure your `core:database` migration locations include that path (see `core/database` README).

[UserEnvKeys]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/config/envkeys/UserEnvKeys.kt
[UserConfigFactoryImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/config/factory/UserConfigFactoryImpl.kt

[UserModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/di/UserModules.kt

[UserFeatureRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/route/UserFeatureRouter.kt

[AuthenticationProvider]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/security/authenticationprovider/AuthenticationProvider.kt
[JwtAuthenticationProvider]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/security/authenticationprovider/JwtAuthenticationProvider.kt
[JwtAuthSpecs]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/security/authenticationprovider/JwtAuthSpecs.kt

[AuthSettingsRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/route/auth/settings/AuthSettingsRouter.kt
[AuthSettingsProvider]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/provider/authsettings/AuthSettingsProvider.kt

[ExternalAuthVerifier]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/auth/verifier/ExternalAuthVerifier.kt
[GoogleAuthVerifier]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/auth/verifier/GoogleAuthVerifier.kt
[ExternalAuthProviderData]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/auth/model/ExternalAuthProviderData.kt

[UserAuditLogger]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/audit/logger/UserAuditLogger.kt

[UserWebSocketModule]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/di/module/UserWebSocketModule.kt
[AuthenticatedWebSocketRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/user/network/websocket/router/AuthenticatedWebSocketRouter.kt
