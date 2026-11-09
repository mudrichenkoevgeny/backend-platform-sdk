package io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.initializer

import io.ktor.server.application.Application

/**
 * Installs and configures OpenAPI/Swagger documentation for the Ktor [Application].
 *
 * Implementations typically install the smiley4 OpenApi plugin with title, version, server URL,
 * and schema generator, so that routes annotated with ktor-openapi expose a spec and Swagger UI.
 */
public interface SwaggerInitializer {
    /**
     * Applies Swagger/OpenAPI setup to the running [application] (e.g. installs OpenApi plugin).
     *
     * @param application The Ktor application to configure; usually called once at startup.
     */
    public fun initialize(application: Application)
}
