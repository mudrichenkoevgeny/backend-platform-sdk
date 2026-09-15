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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant as JavaInstant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant as KotlinInstant
import kotlin.uuid.Uuid

/**
 * Default [UserKnownDevicesRepository] implementation backed by Exposed and [UserKnownDevicesTable].
 *
 * Performs synchronous Exposed DSL operations and maps [ResultRow] values into [UserKnownDevice] models.
 * Returns [CommonError.Database] when an operation reports no affected rows or fails to retrieve records.
 */
@Singleton
class UserKnownDevicesRepositoryImpl @Inject constructor() : UserKnownDevicesRepository {

    override suspend fun createKnownDevice(
        userKnownDevice: UserKnownDevice
    ): AppResult<UserKnownDevice> {
        val inserted = UserKnownDevicesTable.insert { row ->
            row[id] = Uuid.random()
            row[userId] = userKnownDevice.userId.value
            row[deviceId] = userKnownDevice.deviceId
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
                            "deviceId=${userKnownDevice.deviceId}"
                )
            )
        }

        return AppResult.Success(userKnownDevice)
    }

    override suspend fun updateKnownDevice(
        userId: UserId,
        deviceId: String,
        lastIpAddress: UpdateField<String?>,
        lastSeenAt: UpdateField<KotlinInstant>
    ): AppResult<UserKnownDevice> {
        val lastSeenAtToSet = if (lastSeenAt is UpdateField.Set) {
            lastSeenAt.value ?: return AppResult.Error(CommonError.Database("lastSeenAt cannot be null"))
        } else null

        val updatedAtJavaInstant = JavaInstant.now()

        val updatedRows = UserKnownDevicesTable.update(
            { (UserKnownDevicesTable.userId eq userId.value) and (UserKnownDevicesTable.deviceId eq deviceId) }
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
                CommonError.Database("Failed to update known device for userId=${userId.value}, deviceId=$deviceId")
            )
        }

        return getKnownDevice(userId, deviceId).mapNotNullOrError(
            CommonError.Database("Failed to retrieve updated known device")
        )
    }

    override suspend fun getKnownDevice(
        userId: UserId,
        deviceId: String
    ): AppResult<UserKnownDevice?> {
        return AppResult.Success(
            getKnownDeviceResultRow(userId, deviceId)?.toUserKnownDevice()
        )
    }

    private fun getKnownDeviceResultRow(
        userId: UserId,
        deviceId: String
    ): ResultRow? {
        return UserKnownDevicesTable
            .selectAll()
            .where { (UserKnownDevicesTable.userId eq userId.value) and (UserKnownDevicesTable.deviceId eq deviceId) }
            .limit(1)
            .singleOrNull()
    }

    private fun ResultRow.toUserKnownDevice(): UserKnownDevice {
        return UserKnownDevice(
            userId = UserId(this[UserKnownDevicesTable.userId].value),
            deviceId = this[UserKnownDevicesTable.deviceId],
            firstIpAddress = this[UserKnownDevicesTable.firstIpAddress],
            lastIpAddress = this[UserKnownDevicesTable.lastIpAddress],
            userAgent = this[UserKnownDevicesTable.userAgent],
            firstSeenAt = this[UserKnownDevicesTable.firstSeenAt].toKotlinInstant(),
            lastSeenAt = this[UserKnownDevicesTable.lastSeenAt].toKotlinInstant()
        )
    }
}