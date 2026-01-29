package io.github.mudrichenkoevgeny.backend.core.observability.application

import io.github.mudrichenkoevgeny.backend.core.common.constants.NetworkConstants
import io.github.mudrichenkoevgeny.backend.core.common.constants.TracingConstants
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.observability.metrics.MetricsConstants
import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.routing.Routing
import io.ktor.util.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import org.slf4j.MDC

const val ROUTING_ATTRIBUTE_KEY = "Routing"
const val DEFAULT_ROUTE_PATH = "unknown_route"
const val UNKNOWN_ERROR_MESSAGE = "Unknown error"

fun Application.configureObservability(
    telemetryProvider: TelemetryProvider,
    appLogger: AppLogger
) {
    install(MicrometerMetrics) {
        registry = telemetryProvider.prometheusMeterRegistry
    }

    setupTracing(telemetryProvider, appLogger)
}

private fun Application.setupTracing(
    telemetryProvider: TelemetryProvider,
    appLogger: AppLogger
) {
    val tracer = telemetryProvider.tracer
    val meter = telemetryProvider.meter

    val requestCounter = meter
        .counterBuilder(MetricsConstants.HTTP_REQUESTS_TOTAL)
        .setDescription(MetricsConstants.HTTP_REQUESTS_TOTAL_DESCRIPTION)
        .setUnit(MetricsConstants.COUNTER_UNIT)
        .build()

    val requestLatency = meter
        .histogramBuilder(MetricsConstants.HTTP_REQUEST_LATENCY_MS)
        .setDescription(MetricsConstants.HTTP_REQUEST_LATENCY_MS_DESCRIPTION)
        .setUnit(MetricsConstants.LATENCY_UNIT)
        .build()

    val errorCounter = meter
        .counterBuilder(MetricsConstants.HTTP_REQUEST_ERRORS_TOTAL)
        .setDescription(MetricsConstants.HTTP_REQUEST_ERRORS_TOTAL_DESCRIPTION)
        .setUnit(MetricsConstants.COUNTER_UNIT)
        .build()

    intercept(ApplicationCallPipeline.Monitoring) {
        val routingKey = AttributeKey<Routing>(ROUTING_ATTRIBUTE_KEY)

        val routePath = call.attributes.getOrNull(routingKey)
            ?.toString()
            ?.substringAfter(" /")
            ?: call.request.uri.takeIf { it.isNotBlank() }
            ?: DEFAULT_ROUTE_PATH

        val method = call.request.httpMethod.value

        val span = tracer.spanBuilder(call.request.uri)
            .setSpanKind(SpanKind.SERVER)
            .startSpan()

        val traceId = span.spanContext.traceId
        val contextWithSpan = Context.current().with(span)
        val startTimeNs = System.nanoTime()

        var isThrowable = false
        try {
            MDC.put(TracingConstants.TRACE_ID_KEY, traceId)
            call.response.headers.append(NetworkConstants.TRACE_HEADER_NAME, traceId)

            withContext(contextWithSpan.asContextElement()) {
                proceed()
            }
        } catch (t: Throwable) {
            appLogger.logError(CommonError.System(t))
            isThrowable = true
            span.recordException(t)
            span.setStatus(StatusCode.ERROR, t.message ?: UNKNOWN_ERROR_MESSAGE)
            throw t
        } finally {
            val durationMs = (System.nanoTime() - startTimeNs) / 1_000_000

            val responseStatus = call.response.status()?.value
            val statusCode = responseStatus ?: if (isThrowable) {
                500
            } else {
                200
            }

            val attributes = Attributes.builder()
                .put(MetricsConstants.ATTR_ENDPOINT, routePath)
                .put(MetricsConstants.ATTR_METHOD, method)
                .put(MetricsConstants.ATTR_STATUS_CODE, statusCode.toLong())
                .build()

            requestCounter.add(MetricsConstants.COUNTER_VALUE, attributes)
            requestLatency.record(durationMs.toDouble(), attributes)

            if (isThrowable || statusCode >= 400) {
                errorCounter.add(MetricsConstants.COUNTER_VALUE, attributes)
            }

            span.end()
            MDC.remove(TracingConstants.TRACE_ID_KEY)
        }
    }
}