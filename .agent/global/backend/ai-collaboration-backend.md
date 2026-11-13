---
description: Backend architectural rules, Dual-Connector pattern, routing, and Dagger DI
alwaysApply: true
---

# AI Collaboration & Workflow Standards — Backend Platform Additions

This document defines backend-specific architectural and workflow requirements for Kotlin/Ktor microservices and backend SDKs. It builds upon `ai-collaboration-basics.md`.

## 1. Architectural Workflow & Dual-Connector Ports
- **Dual-Connector Architecture:** All backend services must support port separation:
  - **Public API:** Default connector for client-facing and external routes.
  - **Management/Internal API:** Dedicated management connector strictly for operational endpoints (health, readiness, metrics, internal management).
- **Route Registration:** To add an endpoint, implement `BaseRouter`, wire it through the appropriate feature router, and document it using standard KDoc.
- **Foundational Isolation:** Core base infrastructure (`core/common`) must remain a pure leaf/root without internal dependencies on higher-level infrastructure or feature modules.
- **Dagger DI Wiring:** Every module must provide its own explicit Dagger module (`*Module.kt`) for dependency injection wiring.
