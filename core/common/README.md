# core/common

Base for all SDK modules: Ktor server setup, config, error handling, routing, Swagger, WebSockets, logging. Does not depend on other in-repo core modules.

## What it provides

- **Server:** `KtorServer.create(commonConfig) { module(applicationModule) }`, Netty, default headers, CORS.
- **Config:** Env-based config, `CommonConfig`, `PathResolver`, `SwaggerConfig`. App provides `AppInfo` (name, version).
- **Errors:** `AppErrorParser`, status pages, `CommonError`; optional `AppErrorParserConfigHolder` for runtime overrides.
- **Routing:** `BaseRouter`, helpers (`onPort`, `respondResult`). Feature routers implement `BaseRouter` and call `register(route)`.
- **Swagger/OpenAPI:** `SwaggerInitializer`; schema generation and Swagger UI wiring.
- **WebSockets:** Public WebSocket support and config.
- **Logging:** `AppLogger` (system/business loggers).
- **HTTP client:** `HttpClientProvider` for outbound calls.
- **Health:** `HealthCheckerManager`, `HealthCheck` contract.
- **Result types:** `AppResult`, `AppSystemResult`.
- **Shared models:** e.g. `UserId`, `UserSessionId`, `UserDeviceId`, `UserIdentifierId` (value classes with parse/generate).

## Usage

- Add dependency on `core:common`. Most other core and feature modules already depend on it.
- Install [CommonModules] in your Dagger component. The app must provide [BackgroundScope] (and optionally path resolver, error parser config) in the application component; common wires the rest.
- Bootstrap: create `CommonConfig` (from `CommonConfigFactory`), then `KtorServer.create(commonConfig) { module(yourApplicationModule) }`. Register feature routers inside your application module’s `routing { }`.
- Localization: place message files under `src/main/resources/localization/{language-code}/` (e.g. `error_messages.json`).

[CommonModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/di/CommonModules.kt
[BackgroundScope]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/di/qualifiers/BackgroundScope.kt
