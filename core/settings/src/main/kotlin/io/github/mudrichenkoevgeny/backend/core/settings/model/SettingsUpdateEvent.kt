package io.github.mudrichenkoevgeny.backend.core.settings.model

import kotlinx.serialization.Serializable

/**
 * Event published when system settings are modified.
 *
 * Used to synchronize state across multiple instances of the application or to trigger
 * invalidation of in-memory caches.
 *
 * @property senderId identifier of the node or service that initiated the change, used to prevent feedback loops
 * @property payload specific command or key associated with the update, defaults to "refresh"
 */
@Serializable
data class SettingsUpdateEvent(
    val senderId: String,
    val payload: String = "refresh"
)