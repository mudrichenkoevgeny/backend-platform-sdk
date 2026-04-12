# core/common

Base for all SDK modules: Ktor application wiring, config, errors, routing helpers, Swagger, logging. Does not depend on other in-repo core modules.

## What it provides

- **Server:** [KtorServer] factory (Netty, default headers, CORS). The app supplies `Application.module { }` for routing and feature installs.
- **Application helpers:** JSON/content negotiation ([ApplicationSerialization]), unified status pages ([ApplicationStatusPages]), optional [ApplicationRateLimit], WebSockets plugin install ([configureWebSockets]) with shared [WebSocketConfig] (session routing and handlers live in `feature/user`).
- **Config:** Env reader ([EnvReaderImpl]), `.env` and secrets dir via [ResolvedPaths], [CommonConfig], path resolver, [SwaggerConfig]. App provides [AppInfo] (name, version).
- **Errors:** [AppErrorParser], status pages integration, [CommonError]; optional [AppErrorParserConfigHolder] for runtime overrides.
- **Routing:** [BaseRouter], `Route` port helpers, [ApplicationCall] respond extensions for results.
- **Swagger/OpenAPI:** [SwaggerInitializer]; schema generation and Swagger UI wiring.
- **Logging:** [AppLogger] (system/business loggers).
- **HTTP client:** [HttpClientProvider] / settings for outbound calls.
- **Health:** [HealthCheckerManager], [HealthCheck] contract.
- **Result types:** [AppResult], [AppSystemResult] and small extension helpers.
- **Masking:** [DataMasker] for logs, audit metadata, and API responses (email, phone, id, IP, partial/full string rules).
- **Pagination:** [PageParams], [ListingQueryParams], [PagedResult] `mapItems` extension (sort types from shared foundation).
- **Validation:** Ktor `ApplicationCall` query parameter parsing ([ApplicationCallQueryParameterExtensions], [ListingQueryParameterExtensions]) and network field validators.
- **Network:** shared HTTP header names ([CommonNetworkHttpHeaders]).

Domain identifiers and user/session models are not defined here; they live in shared foundation or feature modules (for example `feature/user`).

## Usage

- Add dependency on `core:common`. Most other core and feature modules already depend on it.
- Install [CommonModules] in your Dagger component. The app must provide [BackgroundScope] (and optionally path resolver, error parser config) in the application component; common wires the rest.
- Bootstrap: create `CommonConfig` (from `CommonConfigFactory`), then `KtorServer.create(commonConfig) { module(yourApplicationModule) }`. Register feature routers inside your application module’s `routing { }`.
- Localization: place message files under `src/main/resources/localization/{language-code}/` (e.g. `error_messages.json`).

[AppErrorParser]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/error/parser/AppErrorParser.kt
[AppErrorParserConfigHolder]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/error/model/AppErrorParserConfigHolder.kt
[AppInfo]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/config/common/model/AppInfo.kt
[AppLogger]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/logs/AppLogger.kt
[AppResult]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/result/AppResult.kt
[AppSystemResult]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/result/AppSystemResult.kt
[ApplicationCallQueryParameterExtensions]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/validation/ApplicationCallQueryParameterExtensions.kt
[ApplicationRateLimit]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/application/ratelimit/ApplicationRateLimit.kt
[ApplicationSerialization]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/application/serialization/ApplicationSerialization.kt
[ApplicationStatusPages]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/application/statuspages/ApplicationStatusPages.kt
[BackgroundScope]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/di/qualifiers/BackgroundScope.kt
[BaseRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/routing/BaseRouter.kt
[CommonConfig]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/config/common/model/CommonConfig.kt
[CommonError]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/error/model/CommonError.kt
[CommonModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/di/CommonModules.kt
[CommonNetworkHttpHeaders]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/network/contract/CommonNetworkHttpHeaders.kt
[configureWebSockets]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/application/websockets/ApplicationWebSockets.kt
[DataMasker]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/mask/DataMasker.kt
[EnvReaderImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/config/env/EnvReaderImpl.kt
[HealthCheck]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/healthcheck/HealthCheck.kt
[HealthCheckerManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/healthcheck/HealthCheckerManager.kt
[HttpClientProvider]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/network/httpclient/HttpClientProvider.kt
[KtorServer]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/server/KtorServerFactory.kt
[ListingQueryParameterExtensions]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/validation/ListingQueryParameterExtensions.kt
[ListingQueryParams]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/pagination/ListingQueryParams.kt
[PageParams]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/pagination/PageParams.kt
[PagedResult]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/pagination/PagedResultExtensions.kt
[ResolvedPaths]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/config/pathresolver/ResolvedPaths.kt
[SwaggerConfig]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/config/swagger/model/SwaggerConfig.kt
[SwaggerInitializer]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/documentation/swagger/initializer/SwaggerInitializer.kt
[WebSocketConfig]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/network/websocket/config/WebSocketConfig.kt
