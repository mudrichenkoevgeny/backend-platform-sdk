package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usertotpsettings

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserTotpSettingsTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant as JavaInstant
import org.jetbrains.exposed.v1.jdbc.insert
import kotlin.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserTotpSettingsRepositoryImplTest {

    private val dataSource = createTestDataSource("user_totp_repo")
    private lateinit var repository: UserTotpSettingsRepository

    @BeforeAll
    fun setup() {
        Database.connect(dataSource)
        runBlocking {
            suspendTransaction {
                SchemaUtils.drop(UserTotpSettingsTable, UsersTable)
                SchemaUtils.create(UsersTable, UserTotpSettingsTable)
            }
        }
        repository = UserTotpSettingsRepositoryImpl()
    }

    @Test
    fun `upsertUnconfirmedSettings creates new entry if not exists`() = runBlocking {
        val userId = createTestUserInDb()
        val secret = EncryptedString("initial_secret")

        val result = suspendTransaction { repository.upsertUnconfirmedSettings(userId, secret) }

        val data = (result as AppResult.Success).data
        assertEquals(userId, data.userId)
        assertEquals(secret, data.encryptedSecret)
        assertFalse(data.isConfirmed)
    }

    @Test
    fun `upsertUnconfirmedSettings updates existing entry and resets confirmation`() = runBlocking {
        val userId = createTestUserInDb()
        val initialSecret = EncryptedString("secret_1")
        val newSecret = EncryptedString("secret_2")

        suspendTransaction {
            repository.upsertUnconfirmedSettings(userId, initialSecret)
            repository.confirmTotp(userId, listOf(EncryptedString("code1")))
            repository.upsertUnconfirmedSettings(userId, newSecret)
        }

        val result = suspendTransaction { repository.getSettingsByUserId(userId) }
        val data = (result as AppResult.Success).data!!

        assertEquals(newSecret, data.encryptedSecret)
        assertFalse(data.isConfirmed)
        assertNull(data.encryptedRecoveryCodes)
    }

    @Test
    fun `confirmTotp sets isConfirmed to true and saves recovery codes`() = runBlocking {
        val userId = createTestUserInDb()
        suspendTransaction { repository.upsertUnconfirmedSettings(userId, EncryptedString("secret")) }

        val recoveryCodes = listOf(EncryptedString("rec1"), EncryptedString("rec2"))
        val result = suspendTransaction { repository.confirmTotp(userId, recoveryCodes) }

        val data = (result as AppResult.Success).data
        assertTrue(data.isConfirmed)
        assertEquals(recoveryCodes, data.encryptedRecoveryCodes)
    }

    @Test
    fun `updateSettings modifies specific fields via UpdateField`() = runBlocking {
        val userId = createTestUserInDb()
        suspendTransaction { repository.upsertUnconfirmedSettings(userId, EncryptedString("old")) }

        val lastUsed = Instant.parse(BASE_TIME)
        val result = suspendTransaction {
            repository.updateSettings(
                userId = userId,
                encryptedSecret = UpdateField.Set(EncryptedString("new")),
                isConfirmed = UpdateField.Set(true),
                encryptedRecoveryCodes = UpdateField.Ignore,
                lastUsedAt = UpdateField.Set(lastUsed)
            )
        }

        val data = (result as AppResult.Success).data
        assertEquals(EncryptedString("new"), data.encryptedSecret)
        assertTrue(data.isConfirmed)
        assertEquals(lastUsed, data.lastUsedAt)
    }

    @Test
    fun `deleteSettings removes settings for user`() = runBlocking {
        val userId = createTestUserInDb()
        suspendTransaction { repository.upsertUnconfirmedSettings(userId, EncryptedString("secret")) }

        suspendTransaction { repository.deleteSettings(userId) }
        val result = suspendTransaction { repository.getSettingsByUserId(userId) }

        assertNull((result as AppResult.Success).data)
    }

    private suspend fun createTestUserInDb(): UserId {
        val id = UserId.generate()
        suspendTransaction {
            UsersTable.insert {
                it[UsersTable.id] = id.value
                it[UsersTable.role] = UserRole.USER
                it[accountStatus] = UserAccountStatus.ACTIVE
                it[authorityLevel] = 1
                it[permissionCodes] = emptySet()
                it[isTotpEnabled] = false
                it[createdAt] = JavaInstant.now()
            }
        }
        return id
    }

    private companion object {
        const val BASE_TIME = "2026-05-04T15:00:00Z"
    }
}