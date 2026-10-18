---
description: Centralized localization architecture using shared-foundation, modular error parsers, and HTTP locale extraction
globs: "**/*.kt"
alwaysApply: true
---

# Localization Architecture

This document defines the localization standards and architecture for the backend SDK modules.

## 1. Embedded Multiplatform Strings (`shared-foundation`)
- **No Local JSON Dictionaries:** The backend repository MUST NOT store local JSON translation files (`error_messages.json`, etc.) or hardcoded string dictionaries.
- **Embedded Bundles:** All localized string templates and error descriptions (`en`, `ru`) across all modules (`core/common`, `core/security`, `feature/user`) are embedded directly into `shared-foundation` library binaries (`0.0.70+`).
- **Type-Safe Module Strings:** Use the generated module string facades:
  - `CommonModuleStrings(locale = locale).errors` for `CommonErrorCodes`
  - `SecurityModuleStrings(locale = locale).errors` for `SecurityErrorCodes`
  - `UserModuleStrings(locale = locale).errors` for `UserErrorCodes`
  - `UserModuleStrings(locale = locale).emailTemplates` for type-safe email templates

## 2. Modular Error Parser Architecture (Chain of Responsibility)
- **Modular Parsers:** Each module provides its own `AppErrorSpecificParser` implementation:
  - `CommonAppErrorSpecificParser` handles `CommonErrorCodes`.
  - `SecurityErrorParser` handles `SecurityErrorCodes`.
  - `UserErrorParser` handles `UserErrorCodes`.
- **Strict Module Isolation:** Each specific parser ONLY handles error codes belonging to its module using its type-safe `*ModuleStrings` facade.
- **Root Resolver:** `AppErrorParserResolver` delegates sequentially to registered `AppErrorSpecificParser` implementations in the DI graph. If unhandled, it falls back to `CommonErrorParser` (`UNKNOWN` error).
- **Automatic Bundle Registration:** Instantiating `*ModuleStrings` in each parser's `getApiErrorResponse` method automatically registers the module's embedded resources in `LocalizationRegistry`.

## 3. HTTP Request Locale Resolution (`requestLocale`)
- **Extraction Helper:** Use `ApplicationCall.requestLocale()` to extract the user's preferred locale from the `Accept-Language` HTTP header.
- **Quality Factor Parsing:** Uses Ktor `request.acceptLanguageItems()` to respect HTTP quality factors (`q=`).
- **Fallback:** Defaults to `CommonLocales.DEFAULT` (`"en"`) if `Accept-Language` is omitted or invalid.

## 4. Email Templates (`UserEmailTemplates`)
- **Shared Templates:** Email subjects and HTML body templates are resolved type-safely via `UserModuleStrings(locale = locale).emailTemplates`.
- **Formatting:** Placeholder substitution in templates is performed via `LocalizationFormatter.format(template, args)`.

---
*Refer to `AGENTS.md` for the full list of project standards.*