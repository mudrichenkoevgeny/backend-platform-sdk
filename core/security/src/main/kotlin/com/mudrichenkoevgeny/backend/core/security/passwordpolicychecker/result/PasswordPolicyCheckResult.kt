package com.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.result

import com.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.enums.PasswordPolicyFailReason
import com.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.model.PasswordPolicy

sealed class PasswordPolicyCheckResult {
    object Success : PasswordPolicyCheckResult()
    data class Fail(
        val reasons: List<PasswordPolicyFailReason>,
        val passwordPolicy: PasswordPolicy
    ) : PasswordPolicyCheckResult()
}
