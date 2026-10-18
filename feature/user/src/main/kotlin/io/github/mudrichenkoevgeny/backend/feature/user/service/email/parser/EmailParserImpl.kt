package io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser

import io.github.mudrichenkoevgeny.backend.feature.user.service.email.naming.EmailTemplateArgs
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.naming.EmailTemplateKeys
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model.EmailTemplate
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.localization.UserModuleStrings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Embedded-resource-based [EmailParser] implementation backed by `shared-foundation` [UserModuleStrings].
 */
@Singleton
class EmailParserImpl @Inject constructor() : EmailParser {

    override fun getTemplate(key: String, args: Map<String, Any>?, locale: String): EmailTemplate? {
        val safeArgs = args ?: emptyMap()
        val userStrings = UserModuleStrings(locale = locale)
        val templates = userStrings.emailTemplates

        val sharedTemplate = when (key) {
            EmailTemplateKeys.VERIFICATION_CODE -> {
                templates.verificationCode(code = safeArgs.requireString(EmailTemplateArgs.CODE))
            }
            EmailTemplateKeys.RESET_PASSWORD_CODE -> {
                templates.resetPasswordCode(code = safeArgs.requireString(EmailTemplateArgs.CODE))
            }
            EmailTemplateKeys.UNLOCK_ACCOUNT_CODE -> {
                templates.unlockAccountCode(code = safeArgs.requireString(EmailTemplateArgs.CODE))
            }
            EmailTemplateKeys.ALREADY_REGISTERED -> {
                templates.alreadyRegistered(
                    ipAddress = safeArgs.stringOrDefault(EmailTemplateArgs.IP_ADDRESS, EmailParser.UNKNOWN_VALUE_FALLBACK),
                    deviceName = safeArgs.stringOrDefault(EmailTemplateArgs.DEVICE_NAME, EmailParser.UNKNOWN_VALUE_FALLBACK)
                )
            }
            EmailTemplateKeys.SUCCESSFUL_REGISTRATION -> {
                templates.successfulRegistration
            }
            EmailTemplateKeys.SUCCESSFUL_LOGIN -> {
                templates.successfulLogin(
                    ipAddress = safeArgs.stringOrDefault(EmailTemplateArgs.IP_ADDRESS, EmailParser.UNKNOWN_VALUE_FALLBACK),
                    deviceName = safeArgs.stringOrDefault(EmailTemplateArgs.DEVICE_NAME, EmailParser.UNKNOWN_VALUE_FALLBACK)
                )
            }
            EmailTemplateKeys.NEW_DEVICE_LOGIN -> {
                templates.newDeviceLogin(
                    ipAddress = safeArgs.stringOrDefault(EmailTemplateArgs.IP_ADDRESS, EmailParser.UNKNOWN_VALUE_FALLBACK),
                    deviceName = safeArgs.stringOrDefault(EmailTemplateArgs.DEVICE_NAME, EmailParser.UNKNOWN_VALUE_FALLBACK),
                    userAgent = safeArgs.stringOrDefault(EmailTemplateArgs.USER_AGENT, EmailParser.UNKNOWN_VALUE_FALLBACK)
                )
            }
            EmailTemplateKeys.PASSWORD_CHANGED -> {
                templates.passwordChanged(
                    ipAddress = safeArgs.stringOrDefault(EmailTemplateArgs.IP_ADDRESS, EmailParser.UNKNOWN_VALUE_FALLBACK),
                    deviceName = safeArgs.stringOrDefault(EmailTemplateArgs.DEVICE_NAME, EmailParser.UNKNOWN_VALUE_FALLBACK)
                )
            }
            else -> return null
        }

        return EmailTemplate(
            subject = sharedTemplate.subject,
            body = sharedTemplate.body
        )
    }

    private fun Map<String, Any>.requireString(key: String): String {
        return this[key]?.toString() ?: throw IllegalArgumentException("Missing required argument: $key")
    }

    private fun Map<String, Any>.stringOrDefault(key: String, defaultValue: String): String {
        return this[key]?.toString() ?: defaultValue
    }
}