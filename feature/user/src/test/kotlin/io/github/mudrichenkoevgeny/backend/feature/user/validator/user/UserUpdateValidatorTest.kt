package io.github.mudrichenkoevgeny.backend.feature.user.validator.user

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.UpdateUserParams
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorCodes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.validator.displayname.DisplayNameValidator
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.validator.displayname.DisplayNameValidatorResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.validator.nickname.NicknameValidator
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.validator.nickname.NicknameValidatorResult
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserUpdateValidatorTest {

    private val nicknameValidator = mockk<NicknameValidator>()
    private val displayNameValidator = mockk<DisplayNameValidator>()
    private val userManager = mockk<UserManager>()
    private val authSettingsProvider = mockk<AuthSettingsProvider>()

    private val validator = UserUpdateValidator(
        nicknameValidator = nicknameValidator,
        displayNameValidator = displayNameValidator,
        userManager = userManager,
        authSettingsProvider = authSettingsProvider
    )

    private val targetUserId = UserId.generate()
    private val targetUser = createTestUserInternal(id = targetUserId)

    @BeforeEach
    fun setUp() {
        coEvery { authSettingsProvider.getUserNicknameMinLength() } returns 3
        coEvery { authSettingsProvider.getUserNicknameMaxLength() } returns 20
        coEvery { authSettingsProvider.getIsUserNicknameChangeAllowed() } returns true
        coEvery { authSettingsProvider.getUserDisplayNameMinLength() } returns 2
        coEvery { authSettingsProvider.getUserDisplayNameMaxLength() } returns 50
    }

    @Test
    fun `returns Success when no nickname or display name updates provided`() = runTest {
        val result = validator.validate(targetUser, UpdateUserParams())

        assertEquals(AppResult.Success(Unit), result)
    }

    @Test
    fun `returns Error NicknameChangeDisabled when nickname validator fails with NICKNAME_CHANGE_DISABLED`() = runTest {
        val newNickname = "new_nick"
        coEvery {
            nicknameValidator.validate(
                nickname = newNickname,
                minLength = 3,
                maxLength = 20,
                isNicknameChangeAllowed = true,
                isNicknameChanging = any()
            )
        } returns NicknameValidatorResult.Fail(UserErrorCodes.NICKNAME_CHANGE_DISABLED)

        val params = UpdateUserParams(nickname = UpdateField.Set(newNickname))
        val result = validator.validate(targetUser, params)

        assertTrue(result is AppResult.Error && result.error is UserError.NicknameChangeDisabled)
    }

    @Test
    fun `returns Error NicknameTooShort when nickname validator fails with NICKNAME_TOO_SHORT`() = runTest {
        val newNickname = "a"
        coEvery {
            nicknameValidator.validate(
                nickname = newNickname,
                minLength = 3,
                maxLength = 20,
                isNicknameChangeAllowed = true,
                isNicknameChanging = any()
            )
        } returns NicknameValidatorResult.Fail(UserErrorCodes.NICKNAME_TOO_SHORT)

        val params = UpdateUserParams(nickname = UpdateField.Set(newNickname))
        val result = validator.validate(targetUser, params)

        assertTrue(result is AppResult.Error && result.error is UserError.NicknameTooShort)
    }

    @Test
    fun `returns Error NicknameTooLong when nickname validator fails with NICKNAME_TOO_LONG`() = runTest {
        val newNickname = "very_long_nickname_value"
        coEvery {
            nicknameValidator.validate(
                nickname = newNickname,
                minLength = 3,
                maxLength = 20,
                isNicknameChangeAllowed = true,
                isNicknameChanging = any()
            )
        } returns NicknameValidatorResult.Fail(UserErrorCodes.NICKNAME_TOO_LONG)

        val params = UpdateUserParams(nickname = UpdateField.Set(newNickname))
        val result = validator.validate(targetUser, params)

        assertTrue(result is AppResult.Error && result.error is UserError.NicknameTooLong)
    }

    @Test
    fun `returns Error InvalidNicknameCharacters when nickname validator fails with INVALID_NICKNAME_CHARACTERS`() = runTest {
        val newNickname = "invalid@nick"
        coEvery {
            nicknameValidator.validate(
                nickname = newNickname,
                minLength = 3,
                maxLength = 20,
                isNicknameChangeAllowed = true,
                isNicknameChanging = any()
            )
        } returns NicknameValidatorResult.Fail(UserErrorCodes.INVALID_NICKNAME_CHARACTERS)

        val params = UpdateUserParams(nickname = UpdateField.Set(newNickname))
        val result = validator.validate(targetUser, params)

        assertTrue(result is AppResult.Error && result.error is UserError.InvalidNicknameCharacters)
    }

    @Test
    fun `returns Error NicknameAlreadyTaken when nickname is taken by another user`() = runTest {
        val newNickname = "taken_nick"
        val otherUser = createTestUserInternal(id = UserId.generate())

        coEvery {
            nicknameValidator.validate(
                nickname = newNickname,
                minLength = 3,
                maxLength = 20,
                isNicknameChangeAllowed = true,
                isNicknameChanging = any()
            )
        } returns NicknameValidatorResult.Success
        coEvery { userManager.getUserInternalByNickname(newNickname) } returns AppResult.Success(otherUser)

        val params = UpdateUserParams(nickname = UpdateField.Set(newNickname))
        val result = validator.validate(targetUser, params)

        assertTrue(result is AppResult.Error && result.error is UserError.NicknameAlreadyTaken)
    }

    @Test
    fun `returns Error DisplayNameTooShort when display name validator fails with DISPLAY_NAME_TOO_SHORT`() = runTest {
        val newDisplayName = "x"
        coEvery {
            displayNameValidator.validate(
                displayName = newDisplayName,
                minLength = 2,
                maxLength = 50
            )
        } returns DisplayNameValidatorResult.Fail(UserErrorCodes.DISPLAY_NAME_TOO_SHORT)

        val params = UpdateUserParams(displayName = UpdateField.Set(newDisplayName))
        val result = validator.validate(targetUser, params)

        assertTrue(result is AppResult.Error && result.error is UserError.DisplayNameTooShort)
    }

    @Test
    fun `returns Error DisplayNameTooLong when display name validator fails with DISPLAY_NAME_TOO_LONG`() = runTest {
        val newDisplayName = "a".repeat(100)
        coEvery {
            displayNameValidator.validate(
                displayName = newDisplayName,
                minLength = 2,
                maxLength = 50
            )
        } returns DisplayNameValidatorResult.Fail(UserErrorCodes.DISPLAY_NAME_TOO_LONG)

        val params = UpdateUserParams(displayName = UpdateField.Set(newDisplayName))
        val result = validator.validate(targetUser, params)

        assertTrue(result is AppResult.Error && result.error is UserError.DisplayNameTooLong)
    }

    @Test
    fun `returns Error InvalidDisplayNameCharacters when display name validator fails with INVALID_DISPLAY_NAME_CHARACTERS`() = runTest {
        val newDisplayName = "invalid_name_\u0000"
        coEvery {
            displayNameValidator.validate(
                displayName = newDisplayName,
                minLength = 2,
                maxLength = 50
            )
        } returns DisplayNameValidatorResult.Fail(UserErrorCodes.INVALID_DISPLAY_NAME_CHARACTERS)

        val params = UpdateUserParams(displayName = UpdateField.Set(newDisplayName))
        val result = validator.validate(targetUser, params)

        assertTrue(result is AppResult.Error && result.error is UserError.InvalidDisplayNameCharacters)
    }

    @Test
    fun `returns Success when nickname and display name are valid`() = runTest {
        val newNickname = "valid_nick"
        val newDisplayName = "Valid Display Name"

        coEvery {
            nicknameValidator.validate(
                nickname = newNickname,
                minLength = 3,
                maxLength = 20,
                isNicknameChangeAllowed = true,
                isNicknameChanging = any()
            )
        } returns NicknameValidatorResult.Success
        coEvery { userManager.getUserInternalByNickname(newNickname) } returns AppResult.Success(null)
        coEvery {
            displayNameValidator.validate(
                displayName = newDisplayName,
                minLength = 2,
                maxLength = 50
            )
        } returns DisplayNameValidatorResult.Success

        val params = UpdateUserParams(
            nickname = UpdateField.Set(newNickname),
            displayName = UpdateField.Set(newDisplayName)
        )
        val result = validator.validate(targetUser, params)

        assertEquals(AppResult.Success(Unit), result)
    }
}
