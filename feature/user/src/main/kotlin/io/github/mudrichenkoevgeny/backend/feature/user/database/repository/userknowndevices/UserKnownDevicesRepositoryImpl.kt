package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.userknowndevices

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.model.onSet
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.common.util.toJavaInstant
import io.github.mudrichenkoevgeny.backend.core.common.util.toKotlinInstant
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserKnownDevicesTable
import io.github.mudrichenkoevgeny.backend.feature.user.model.device.UserKnownDevice
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid
import java.time.Instant as JavaInstant
import kotlin.time.Instant as KotlinInstant

/**
 * Default [UserKnownDevicesRepository] implementation backed by Exposed and [UserKnownDevicesTable].
 *
 * Performs synchronous Exposed DSL operations and maps [ResultRow] values into [UserKnownDevice] models.
 * Returns [CommonError.Database] when an operation reports no affected rows or fails to retrieve records.
 */
@Singleton
public class UserKnownDevicesRepositoryImpl @Inject constructor() : UserKnownDevicesRepository {
    override suspend fun createKnownDevice(userKnownDevice: UserKnownDevice): AppResult<UserKnownDevice> {
        return try {
            val inserted = UserKnownDevicesTable.insert { row ->
                row[id] = Uuid.random()
                row[userId] = userKnownDevice.userId.value
                row[deviceId] = userKnownDevice.deviceId.value
                row[firstIpAddress] = userKnownDevice.firstIpAddress
                row[lastIpAddress] = userKnownDevice.lastIpAddress
                row[userAgent] = userKnownDevice.userAgent
                row[firstSeenAt] = userKnownDevice.firstSeenAt.toJavaInstant()
                row[lastSeenAt] = userKnownDevice.lastSeenAt.toJavaInstant()
            }

            if (inserted.insertedCount == 0) {
                return AppResult.Error(
                    CommonError.Database(
                        "UserKnownDevice creation failed for userId=${userKnownDevice.userId.value}, " +
                            "deviceId=${userKnownDevice.deviceId.asHexDashString()}"
                    )
                )
            }

            AppResult.Success(userKnownDevice)
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error creating known device: ${e.message}"))
        }
    }

    override suspend fun updateKnownDevice(
        userId: UserId,
        deviceId: ClientDeviceId,
        lastIpAddress: UpdateField<String?>,
        lastSeenAt: UpdateField<KotlinInstant>
    ): AppResult<UserKnownDevice> {
        val lastSeenAtToSet = if (lastSeenAt is UpdateField.Set) {
            lastSeenAt.value ?: return AppResult.Error(CommonError.Database("lastSeenAt cannot be null"))
        } else {
            null
        }

        val updatedAtJavaInstant = JavaInstant.now()

        return try {
            val updatedRows = UserKnownDevicesTable.update(
                {
                    (UserKnownDevicesTable.userId eq userId.value) and
                        (UserKnownDevicesTable.deviceId eq deviceId.value)
                }
            ) { updateStatement ->
                lastIpAddress.onSet { ip ->
                    updateStatement[UserKnownDevicesTable.lastIpAddress] = ip
                }

                lastSeenAtToSet?.let { seen ->
                    updateStatement[UserKnownDevicesTable.lastSeenAt] = seen.toJavaInstant()
                }

                updateStatement[UserKnownDevicesTable.updatedAt] = updatedAtJavaInstant
            }

            if (updatedRows == 0) {
                return AppResult.Error(
                    CommonError.Database(
                        "Failed to update known device for userId=${userId.value}, deviceId=${deviceId.asHexDashString()}"
                    )
                )
            }

            getKnownDevice(
                userId,
                deviceId
            ).mapNotNullOrError(
                CommonError.Database("Failed to retrieve updated known device")
            )
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error updating known device: ${e.message}"))
        }
    }

    override suspend fun getKnownDevice(
        userId: UserId,
        deviceId: ClientDeviceId
    ): AppResult<UserKnownDevice?> = try {
        AppResult.Success(
            getKnownDeviceResultRow(
                userId,
                deviceId
            )?.toUserKnownDevice()
        )
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error retrieving known device: ${e.message}"))
    }

    private fun getKnownDeviceResultRow(
        userId: UserId,
        deviceId: ClientDeviceId
    ): ResultRow? = UserKnownDevicesTable.selectAll().where {
        (UserKnownDevicesTable.userId eq userId.value) and (UserKnownDevicesTable.deviceId eq deviceId.value)
    }.limit(1).singleOrNull()

    private fun ResultRow.toUserKnownDevice(): UserKnownDevice = UserKnownDevice(
        userId = UserId(this[UserKnownDevicesTable.userId].value),
        deviceId = ClientDeviceId(this[UserKnownDevicesTable.deviceId]),
        firstIpAddress = this[UserKnownDevicesTable.firstIpAddress],
        lastIpAddress = this[UserKnownDevicesTable.lastIpAddress],
        userAgent = this[UserKnownDevicesTable.userAgent],
        firstSeenAt = this[UserKnownDevicesTable.firstSeenAt].toKotlinInstant(),
        lastSeenAt = this[UserKnownDevicesTable.lastSeenAt].toKotlinInstant()
    )
}
