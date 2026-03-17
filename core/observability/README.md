# core/observability

OpenTelemetry and Micrometer/Prometheus: tracing, metrics, and Ktor integration.

## What it provides

- **Config:** [ObservabilityConfig] (service name, telemetry endpoint, metric interval). [ObservabilityConfigFactory] builds it from env via [ObservabilityEnvKeys].
- **TelemetryProvider** — single entry point: OpenTelemetry (tracer, meter), Prometheus meter registry. Binds JVM metrics (memory, GC, threads, processor, classloader) to the registry. [warmup] initializes the tracer.
- **Application bootstrap:** [configureObservability] installs MicrometerMetrics and a request interceptor: server span per request, HTTP request/latency/error metrics, trace id in MDC and response header, error logging and span status.
- **Metrics route:** [installMetricsEndpoint] registers a Prometheus scrape endpoint (e.g. `/metrics`) on a route; [installRegistry] installs only MicrometerMetrics with the provider’s registry when you do not need the full tracing setup.

## Usage

- Add dependency on `core:observability`. Depends on `core:common`.
- Install [ObservabilityModules] in your Dagger component. The module provides [ObservabilityConfig], [ObservabilityConfigFactory], and [TelemetryProvider]. Ensure the OpenTelemetry global is set before using [TelemetryProvider] (e.g. by your app or an OpenTelemetry agent).
- In your application module: call [configureObservability] with the injected [TelemetryProvider] and an [AppLogger] (from `core:common`); optionally call [installMetricsEndpoint] on a route to expose `/metrics`. Call [warmup] after the SDK is ready if you need to ensure the tracer is initialized.

Used by applications that need request metrics, distributed tracing, and a Prometheus scrape endpoint.

[ObservabilityConfig]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/observability/config/model/ObservabilityConfig.kt
[ObservabilityConfigFactory]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/observability/config/factory/ObservabilityConfigFactory.kt
[ObservabilityEnvKeys]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/observability/config/envkeys/ObservabilityEnvKeys.kt
[ObservabilityModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/observability/di/ObservabilityModules.kt
[TelemetryProvider]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/observability/telemetry/TelemetryProvider.kt
[configureObservability]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/observability/application/ApplicationObservability.kt
[installMetricsEndpoint]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/observability/metrics/route/MetricsRoute.kt
[installRegistry]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/observability/metrics/ApplicationInstallRegistry.kt
[warmup]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/observability/telemetry/TelemetryProvider.kt
