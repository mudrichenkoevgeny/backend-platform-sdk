package io.github.mudrichenkoevgeny.backend.core.audit.domain.model

import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType

/**
 * Row-level visibility predicate for listing audit events from the database.
 *
 * Passed into `AuditEventRepository.getEventsList` in this module; the repository turns it into SQL so callers cannot fetch
 * actor types or user roles they are not allowed to see.
 *
 * **Semantics** (how [allowedActorTypes] and [allowedUserRoles] combine):
 * - Every [AuditActorType] in [allowedActorTypes] **except** [AuditActorType.USER] contributes one branch:
 *   “`actor_type` is that enum value”.
 * - If [AuditActorType.USER] is in [allowedActorTypes] and [allowedUserRoles] is not empty, a branch is added:
 *   “`actor_type` is USER **and** `actor_user_role` is one of [allowedUserRoles]” (role strings are whatever the app
 *   persisted on the event, e.g. foundation `UserRole` wire values).
 * - Those branches are combined with **OR**. If no branch is produced (e.g. both sets empty), the query matches **no rows**.
 *
 * Higher layers (typically `AuditManagerImpl` in `feature/auditapi`) build this from the current user’s permission codes;
 * feature modules rarely construct it by hand unless they implement a custom audit listing path.
 */
data class AuditAccessFilter(
    /** Actor kinds the caller may see. USER rows still require a matching [allowedUserRoles] entry (see class KDoc). */
    val allowedActorTypes: Set<AuditActorType>,
    /** Allowed `actor_user_role` values when USER events are permitted; ignored if USER is not in [allowedActorTypes]. */
    val allowedUserRoles: Set<String>
)
