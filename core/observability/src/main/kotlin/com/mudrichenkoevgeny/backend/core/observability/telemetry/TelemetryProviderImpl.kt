package com.mudrichenkoevgeny.backend.core.observability.telemetry

import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.observability.config.model.ObservabilityConfig
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryProviderImpl @Inject constructor(
    observabilityConfig: ObservabilityConfig,
    private val appLogger: AppLogger
) : TelemetryProvider {

    override val openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get()
    override val tracer: Tracer = openTelemetry.getTracer(observabilityConfig.telemetryServiceName)
    override val meter: Meter = openTelemetry.getMeter(observabilityConfig.telemetryServiceName)
    override val prometheusMeterRegistry: PrometheusMeterRegistry = PrometheusMeterRegistry(
        PrometheusConfig.DEFAULT
    )

    init {
        JvmMemoryMetrics().bindTo(prometheusMeterRegistry)
        JvmGcMetrics().bindTo(prometheusMeterRegistry)
        JvmThreadMetrics().bindTo(prometheusMeterRegistry)
        ProcessorMetrics().bindTo(prometheusMeterRegistry)
        ClassLoaderMetrics().bindTo(prometheusMeterRegistry)
    }

    override fun warmup() {
        tracer.spanBuilder(SPAN_WARMUP).startSpan().end()
    }

    companion object {
        const val SPAN_WARMUP = "warmup"
    }
}