# core/common

Base for all SDK modules: Ktor application wiring, config, errors, routing helpers, Swagger, and logging. This module is the foundation of the server-side infrastructure and does not depend on other in-repo core modules.

## What it provides

- **Server:** [KtorServer] factory (Netty). Configures a dual-connector setup: a main API port and a management/monitoring port.
- **HTTP Configuration:** [configureHTTP] extension for `Application`. Centralized setup for CORS (environment-aware) and security headers (CSP, HSTS, XSS protection, Sniff-prevention).
- **Application Helpers:**
    - **Serialization:** [ApplicationSerialization] for JSON content negotiation.
    - **Status Pages:** [ApplicationStatusPages] for unified error handling.
    - **Rate Limiting:** [ApplicationRateLimit] plugin configuration.
    - **WebSockets:** [ApplicationWebSockets] with shared [WebSocketConfig].
- **Config & Env:**
    - **Environment:** [EnvReaderImpl] for `.env` and system variables.
    - **Path Resolution:** [PathResolverImpl] and [ResolvedPaths] for managing configuration and secrets directories.
    - **Factory:** [CommonConfigFactoryImpl] builds [CommonConfig] using [AppInfo] (provided by the app).
- **Validation & Request Handling:**
    - **DTO Validation:** [validateDto] extension uses reflection and annotations (`@RequiredField`, `@NotBlankStringField`) to validate request bodies.
    - **Request Parsing:** [validateRequest] for type-safe, validated JSON reception.
    - **Parameters:** [validatePathParameter] and [parseListingQueryParams] for robust path and query extraction.
    - **Exceptions:** [RequestHandlingException] for bridging validation logic with [AppError].
- **Errors & Localization:** [AppErrorParser] for resolving [AppError] into localized `ApiErrorResponse`. Supports multiple languages via JSON resource files.
- **Logging:** [AppLogger] with distinct qualifiers for **System** and **Business** logging.
- **Health:** [HealthCheckerManager] and [HealthCheck] contract for service readiness and liveness probes.
- **Masking:** [DataMasker] for redacting sensitive information (IP, Email, Phone) in logs and responses.
- **Pagination:** Shared models like [PageParams] and [ListingQueryParams].

## Usage

### 1. Dependency & DI
Add dependency on `core:common`. In your Dagger component, install **[CommonModules]**.
The application must provide the following dependencies to the DI graph:
- `AppInfo` (Name and version of your service).
- `BackgroundScope` (Coroutine scope for background tasks).

### 2. Localization
Place message files under `src/main/resources/localization/{lang}/error_messages.json`. The `AppErrorParser` will automatically resolve keys matching your `AppError.code`.

---

[AppError]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/error/model/AppError.kt
[AppErrorParser]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/error/parser/AppErrorParser.kt
[AppInfo]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/config/common/model/AppInfo.kt
[AppLogger]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/logs/AppLogger.kt
[ApplicationRateLimit]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/application/ratelimit/ApplicationRateLimit.kt
[ApplicationSerialization]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/application/serialization/ApplicationSerialization.kt
[ApplicationStatusPages]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/application/statuspages/ApplicationStatusPages.kt
[ApplicationWebSockets]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/application/websockets/ApplicationWebSockets.kt
[CommonConfig]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/config/common/model/CommonConfig.kt
[CommonConfigFactoryImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/config/common/factory/CommonConfigFactoryImpl.kt
[CommonModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/di/CommonModules.kt
[DataMasker]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/mask/DataMasker.kt
[EnvReaderImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/config/env/EnvReaderImpl.kt
[HealthCheck]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/healthcheck/HealthCheck.kt
[HealthCheckerManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/healthcheck/HealthCheckerManager.kt
[KtorServer]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/server/KtorServerFactory.kt
[ListingQueryParams]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/pagination/ListingQueryParams.kt
[PageParams]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/pagination/PageParams.kt
[PathResolverImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/config/pathresolver/PathResolverImpl.kt
[RequestHandlingException]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/network/request/handler/RequestHandlingException.kt
[ResolvedPaths]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/config/pathresolver/ResolvedPaths.kt
[WebSocketConfig]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/network/websocket/config/WebSocketConfig.kt
[configureHTTP]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/application/httpconfiguration/ApplicationHTTPConfiguration.kt
[parseListingQueryParams]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/network/request/handler/NetworkCallQueryListingParser.kt
[validateDto]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/network/request/handler/DtoValidator.kt
[validatePathParameter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/network/request/handler/NetworkCallPathValidator.kt
[validateRequest]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/network/request/handler/NetworkCallRequestValidator.kt