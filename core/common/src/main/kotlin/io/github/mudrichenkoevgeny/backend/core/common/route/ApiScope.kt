package io.github.mudrichenkoevgeny.backend.core.common.route

/**
 * Defines the functional scope and target audience for API endpoints and real-time communication channels.
 *
 * Categorizes routes and WebSocket connections into distinct operational boundaries, ensuring proper
 * traffic isolation, targeted message broadcasting, and alignment with deployment topology.
 */
enum class ApiScope {
    /**
     * Public-facing API and real-time streams intended for end-user applications.
     */
    OPEN,

    /**
     * Internal management API and real-time streams restricted to administrative and operational workflows.
     */
    MANAGEMENT
}