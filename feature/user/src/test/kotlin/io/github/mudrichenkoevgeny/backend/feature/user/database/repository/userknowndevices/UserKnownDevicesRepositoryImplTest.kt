package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.userknowndevices

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepositoryImpl
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserKnownDevicesTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.createTestUserDetails
import io.github.mudrichenkoevgeny.backend.feature.user.model.device.UserKnownDevice
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.hours

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserKnownDevicesRepositoryImplTest {

    private val dataSource = createTestDataSource("user_known_devices_repo")
    private lateinit var repository: UserKnownDevicesRepository
    private lateinit var userRepository: UserRepository

    @BeforeAll
    fun setup() {
        Database.connect(dataSource)
        runBlocking {
            suspendTransaction {
                SchemaUtils.drop(UserKnownDevicesTable, UsersTable)
                SchemaUtils.create(UsersTable, UserKnownDevicesTable)
            }
        }
        repository = UserKnownDevicesRepositoryImpl()
        userRepository = UserRepositoryImpl()
    }

    private suspend fun createTestUserInDb(): UserId {
        val userId = UserId.generate()
        val user = createTestUserDetails(userId)
        suspendTransaction {
            userRepository.createUser(user)
        }
        return userId
    }

    @Test
    fun `getKnownDevice returns null when not found`() = runBlocking {
        val userId = createTestUserInDb()
        val result = suspendTransaction { repository.getKnownDevice(userId, "device-123") }
        val success = result as AppResult.Success
        assertNull(success.data)
    }

    @Test
    fun `createKnownDevice persists and retrieves known device`() = runBlocking {
        val userId = createTestUserInDb()
        val deviceId = "device-abc"
        val now = kotlin.time.Clock.System.now()

        val knownDevice = UserKnownDevice(
            userId = userId,
            deviceId = deviceId,
            firstIpAddress = "127.0.0.1",
            lastIpAddress = "127.0.0.1",
            userAgent = "Mozilla/5.0",
            firstSeenAt = now,
            lastSeenAt = now
        )

        val createResult = suspendTransaction {
            repository.createKnownDevice(knownDevice)
        }
        assertTrue(createResult is AppResult.Success)

        val getResult = suspendTransaction {
            repository.getKnownDevice(userId, deviceId)
        }
        assertTrue(getResult is AppResult.Success)
        val device = (getResult as AppResult.Success).data
        assertNotNull(device)
        assertEquals(userId, device!!.userId)
        assertEquals(deviceId, device.deviceId)
        assertEquals("127.0.0.1", device.firstIpAddress)
        assertEquals("127.0.0.1", device.lastIpAddress)
        assertEquals("Mozilla/5.0", device.userAgent)
    }

    @Test
    fun `updateKnownDevice updates last ip and seen at`() = runBlocking {
        val userId = createTestUserInDb()
        val deviceId = "device-xyz"
        val now = kotlin.time.Clock.System.now()
        val updatedSeenAt = now + 1.hours

        val knownDevice = UserKnownDevice(
            userId = userId,
            deviceId = deviceId,
            firstIpAddress = "192.168.1.1",
            lastIpAddress = "192.168.1.1",
            userAgent = "Chrome",
            firstSeenAt = now,
            lastSeenAt = now
        )

        suspendTransaction {
            repository.createKnownDevice(knownDevice)
            val updateResult = repository.updateKnownDevice(
                userId = userId,
                deviceId = deviceId,
                lastIpAddress = UpdateField.Set("192.168.1.2"),
                lastSeenAt = UpdateField.Set(updatedSeenAt)
            )
            assertTrue(updateResult is AppResult.Success)
        }

        val getResult = suspendTransaction {
            repository.getKnownDevice(userId, deviceId)
        }
        val device = (getResult as AppResult.Success).data
        assertNotNull(device)
        assertEquals("192.168.1.1", device!!.firstIpAddress)
        assertEquals("192.168.1.2", device.lastIpAddress)
        assertEquals(updatedSeenAt.toEpochMilliseconds(), device.lastSeenAt.toEpochMilliseconds())
    }

    @Test
    fun `updateKnownDevice returns error when lastSeenAt is null`() = runBlocking {
        val userId = createTestUserInDb()
        val deviceId = "device-null-seen"
        val now = kotlin.time.Clock.System.now()

        val knownDevice = UserKnownDevice(
            userId = userId,
            deviceId = deviceId,
            firstIpAddress = "10.0.0.1",
            lastIpAddress = "10.0.0.1",
            userAgent = "Safari",
            firstSeenAt = now,
            lastSeenAt = now
        )

        val result = suspendTransaction {
            repository.createKnownDevice(knownDevice)
            repository.updateKnownDevice(
                userId = userId,
                deviceId = deviceId,
                lastSeenAt = UpdateField.Set(null)
            )
        }

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is CommonError.Database)
        assertEquals("lastSeenAt cannot be null", error.secretArgs?.get(CommonErrorArgs.MESSAGE))
    }

    @Test
    fun `updateKnownDevice returns error when record not found`() = runBlocking {
        val userId = createTestUserInDb()
        val result = suspendTransaction {
            repository.updateKnownDevice(
                userId = userId,
                deviceId = "non-existent-device",
                lastIpAddress = UpdateField.Set("127.0.0.1")
            )
        }

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is CommonError.Database)
    }
}