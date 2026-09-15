package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.userknowndevices

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.device.UserKnownDevice
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlin.time.Instant

/**
 * Persistence API for tracking known devices used by users for security monitoring and login notifications.
 */
interface UserKnownDevicesRepository {

    /**
     * Persists a new known device record.
     *
     * @param userKnownDevice known device model to create
     * @return created known device or an error
     */
    suspend fun createKnownDevice(userKnownDevice: UserKnownDevice): AppResult<UserKnownDevice>

    /**
     * Updates selected fields of an existing known device.
     *
     * @param userId user identifier
     * @param deviceId unique device identifier
     * @param lastIpAddress last seen IP address update
     * @param lastSeenAt last seen timestamp update
     * @return updated known device or an error
     */
    suspend fun updateKnownDevice(
        userId: UserId,
        deviceId: String,
        lastIpAddress: UpdateField<String?> = UpdateField.Ignore,
        lastSeenAt: UpdateField<Instant> = UpdateField.Ignore
    ): AppResult<UserKnownDevice>

    /**
     * Retrieves a known device record by user ID and device ID.
     *
     * @param userId user identifier
     * @param deviceId unique device identifier
     * @return known device when found, `null` when missing, or an error
     */
    suspend fun getKnownDevice(userId: UserId, deviceId: String): AppResult<UserKnownDevice?>
}