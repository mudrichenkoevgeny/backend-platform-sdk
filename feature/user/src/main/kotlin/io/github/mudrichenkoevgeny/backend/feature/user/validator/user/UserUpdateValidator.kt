package io.github.mudrichenkoevgeny.backend.feature.user.validator.user

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.dataOrNull
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.UpdateUserParams
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorCodes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.validator.displayname.DisplayNameValidator
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.validator.displayname.DisplayNameValidatorResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.validator.nickname.NicknameValidator
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.validator.nickname.NicknameValidatorResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates domain-level user updates (nickname and display name) against configured rules.
 *
 * Enforces character constraints, length boundaries, and global uniqueness via [UserManager].
 */
@Singleton
class UserUpdateValidator @Inject constructor(
    private val nicknameValidator: NicknameValidator,
    private val displayNameValidator: DisplayNameValidator,
    private val userManager: UserManager,
    private val authSettingsProvider: AuthSettingsProvider
) {
    /**
     * Validates nickname and display name inside [updateUserParams] against the target [user].
     *
     * @param user The current target user model.
     * @param updateUserParams The structured parameters for the intended update.
     * @return [AppResult.Success] if all parameters pass validation, or [AppResult.Error] wrapping the [UserError].
     */
    suspend fun validate(
        user: UserInternal,
        updateUserParams: UpdateUserParams
    ): AppResult<Unit> {
        val newNickname = (updateUserParams.nickname as? UpdateField.Set)?.value
        if (newNickname != null) {
            val minLength = authSettingsProvider.getUserNicknameMinLength()
            val maxLength = authSettingsProvider.getUserNicknameMaxLength()
            val isChangeAllowed = authSettingsProvider.getIsUserNicknameChangeAllowed()

            val validationResult = nicknameValidator.validate(
                nickname = newNickname,
                minLength = minLength,
                maxLength = maxLength,
                isNicknameChangeAllowed = isChangeAllowed,
                isNicknameChanging = user.nickname != null && user.nickname != newNickname
            )
            when (validationResult) {
                is NicknameValidatorResult.Fail -> {
                    val error = when (validationResult.errorCode) {
                        UserErrorCodes.NICKNAME_CHANGE_DISABLED -> UserError.NicknameChangeDisabled()
                        UserErrorCodes.NICKNAME_TOO_SHORT -> UserError.NicknameTooShort(minLength)
                        UserErrorCodes.NICKNAME_TOO_LONG -> UserError.NicknameTooLong(maxLength)
                        UserErrorCodes.INVALID_NICKNAME_CHARACTERS -> UserError.InvalidNicknameCharacters()
                        else -> UserError.InvalidNicknameCharacters()
                    }
                    return AppResult.Error(error)
                }
                is NicknameValidatorResult.Success -> {
                    val existingByNick = userManager.getUserInternalByNickname(newNickname).dataOrNull()
                    if (existingByNick != null && existingByNick.id != user.id) {
                        return AppResult.Error(UserError.NicknameAlreadyTaken(newNickname))
                    }
                }
            }
        }

        val newDisplayName = (updateUserParams.displayName as? UpdateField.Set)?.value
        if (newDisplayName != null) {
            val minLength = authSettingsProvider.getUserDisplayNameMinLength()
            val maxLength = authSettingsProvider.getUserDisplayNameMaxLength()

            val validationResult = displayNameValidator.validate(
                displayName = newDisplayName,
                minLength = minLength,
                maxLength = maxLength
            )
            if (validationResult is DisplayNameValidatorResult.Fail) {
                val error = when (validationResult.errorCode) {
                    UserErrorCodes.DISPLAY_NAME_TOO_SHORT -> UserError.DisplayNameTooShort(minLength)
                    UserErrorCodes.DISPLAY_NAME_TOO_LONG -> UserError.DisplayNameTooLong(maxLength)
                    UserErrorCodes.INVALID_DISPLAY_NAME_CHARACTERS -> UserError.InvalidDisplayNameCharacters()
                    else -> UserError.InvalidDisplayNameCharacters()
                }
                return AppResult.Error(error)
            }
        }

        return AppResult.Success(Unit)
    }
}
