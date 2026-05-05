---
description: Localization resource layout, naming conventions, and message types
globs: "**/src/main/resources/**/*"
alwaysApply: true
---

# Localization

This document defines the structure and naming standards for all localized resources within the SDK modules.

## 1. Directory Structure
- Localization resources must reside strictly under: `src/main/resources/localization/{language-code}/`.
- **Language Codes:** Use standard ISO codes as folder names (e.g., `en`, `ru`, `de`).
- **Hierarchy Example:**
    - `localization/en/error_messages.json`
    - `localization/ru/error_messages.json`
    - `localization/en/email_messages.json`

## 2. File Naming & Scope
- **Consistency:** Use the exact same filename for different locales within their respective folders.
- **No Suffixes:** Do not use locale suffixes in filenames (e.g., use `error_messages.json`, NOT `error_messages_en.json`).
- **No Generic Prefixes:** Avoid redundant prefixes like `app_errors_`.

## 3. Message Types

### Error Messages (`error_messages.json`)
- Contains localized strings for `ApiErrorResponse` codes.
- Use placeholders that match `publicArgs` defined in the error models.

### Email Messages (`email_messages.json`)
- Contains localized subjects and body templates for system-generated emails (e.g., MFA codes, welcome emails).
- Follow the same directory structure as error messages.
- Templates should be kept concise and compatible with the SDK's internal mailer logic.

## 4. Resource Integrity
- **Parity:** Every key present in the `en` locale must have a corresponding entry in all other supported locales.
- **JSON Format:** Ensure strict JSON syntax for all localization files to prevent runtime parsing errors.

---
*Refer to `AGENTS.md` for the full list of project standards.*