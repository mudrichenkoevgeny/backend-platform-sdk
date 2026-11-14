---
description: Naming, when subject rules, brace requirements, and FQN/Comments ban
glob: "**/*.kt"
alwaysApply: true
---

# Kotlin Coding Style Basics

This document defines the mandatory Kotlin syntax and formatting standards for the project.

## 1. Core Syntax Constraints

### Fully Qualified Names (FQN)
- **Strict Ban:** Do not use FQN in expressions, type annotations, or generics.
- **Resolution:** Use `import` or `import ... as ...` aliases.
- **Exceptions:** Permitted inside `@Deprecated(..., replaceWith = ReplaceWith(...))` import parameters, or when imports are technically impossible or cause unresolvable ambiguity (justify in the task summary).

### Implementation Comments vs. KDoc Documentation
- **No Agent-Generated Inline Comments or TODOs:** AI agents must NOT write inline implementation comments (`//` or `/* ... */`) or generate new `TODO` / `FIXME` markers unless explicitly requested by the developer. Logic must be self-documenting through expressive naming and clean decomposition of complex steps into private helper functions.
- **Preserve Existing Developer TODOs:** Never delete pre-existing `// TODO:` or `// FIXME:` comments left by the developer unless the current task explicitly resolves that specific TODO item.
- **MANDATORY KDoc Preservation:** KDoc blocks (`/** ... */`) are structured API documentation and are strictly MANDATORY. AI agents must **NEVER** delete, strip, wipe, or omit existing KDoc documentation blocks during refactoring, updates, or code edits. All public interfaces, classes, functions, and properties must retain and maintain their KDoc documentation.

### Single Declaration Per File
- **Strict Requirement:** Every public `interface`, `class`, or `object` must reside in its own dedicated file matching its declaration name.
- **Strict Ban:** Do not place an interface and its implementation class, or multiple public top-level classes/interfaces/objects, in the same file.
- **Permitted Exceptions Only:**
  - Direct subtypes (`data class`, `data object`, `value class`) declared inside or alongside their parent `sealed interface` / `sealed class` hierarchy.
  - Top-level extension functions and constants directly bound to a primary declaration in the same file (e.g., `String.toXxxOrNull()` and `String.toXxxOrThrow()` for a `value class`).

### Trailing Commas
- **Strict Ban:** Do not write trailing commas at the end of argument, parameter, or entry lists.

### Non-Null Assertion Operator (`!!`)
- **Strict Ban:** The non-null assertion operator (`!!`) is strictly forbidden in production code.
- **Allowed Alternatives:**
  - Handle absence gracefully using safe calls (`?.`), Elvis operators (`?:`), safe casts (`as?`), or explicit `if`/`when` smart-casts.
  - Use `requireNotNull(...)` or `checkNotNull(...)` with descriptive messages only when asserting invariants or unrecoverable programming errors, never as a substitute for error handling.
- **Exceptions:** Allowed in test code (`*Test.kt`, `src/commonTest/`, `src/test/`) when asserting non-null values where failure is intended to fail the test.

## 2. Control Flow

### `when` Expressions
- **No Invocations in Subject:** Do not put function or suspend calls inside `when (...)` parentheses.
- **Preceding Variable:** Assign the return value to a `val` on the preceding line, then use `when` only on that variable.
- **Forbidden Pattern:** Do not use `when (val x = someFunction())`. Always extract the invocation to its own `val` first.

### Braces for `if` and `return`
- **Block Body Requirement:** Do not write single-line early exits like `if (condition) return`.
- **Mandatory Braces:** Always use a block body `{ ... }` for `if` statements, even if they only perform a `return` or `return@label`.
- **Formatting:** Put the `return` statement on its own line inside the braces.

## 3. Naming Conventions

### Outcome Variable Naming
- **Domain Focus:** Use concrete names tied to the domain or operation (e.g., `validationResult`), not vague placeholders like `result` or `data`.
- **Outcome Clarity:** When a variable holds the outcome of a step (especially a result-shaped type), include the outcome type in the name (e.g., `PasswordPolicyValidatorResult` -> `validationResult`).

### Property Naming (DTOs)
- **JSON Parity:** Always use `@SerialName` with constants from `*ApiFields`.
- **Snake Case vs Camel Case:** While Kotlin properties use `camelCase`, ensure they correctly map to the project's `snake_case` wire format via constants.

## 4. Expression vs Body Functions

- **Simple Mappers:** Use expression-body functions (`fun toPayload() = ...`) for simple transformations and mappers.
- **Complex Logic:** Use block-body functions (`fun process() { ... }`) for anything involving multiple steps or branching logic to ensure readability without comments.

## 5. Sealed Types and Enums

- **Exhaustiveness:** When using `when` on `sealed class`, `sealed interface`, or `enum`, do not provide an `else` branch unless it is logically unavoidable. Ensure all cases are handled explicitly.
- **Value Property for Wire Values:** All domain enums and extensible interfaces representing serialized constants expose a `.value` property for wire string representation (replacing legacy `.serialName` getters/calls).

## 6. Companion Objects & Value Classes

### Companion Objects
- **Placement:** Place `companion object` blocks at the bottom of the class body (after member methods/properties), unless the companion object acts as a parameter parser interface (e.g., `public companion object : NavigationScreenParamsParser<P>`).
- **Visibility:** Always specify explicit visibility for companion objects in public classes (`public companion object { ... }`).
- **Constant Extraction:** Constants extracted from class logic or rule constructors (e.g., Detekt `RULE_DESCRIPTION`, test constants `TEST_*`) must be declared inside a `public companion object` using `public const val`.

### Inline Value Classes
- **Domain Identifiers & Wrappers:** Use `@JvmInline value class` for strongly-typed domain identifiers (`UserId`, `FileId`, `AuditEventId`, `PermissionCode`, `EncryptedString`).
- **Standard API Requirements:** Every domain ID value class representing a serialized identifier or token must meet the following standard API requirements:
  1. **Serialization & Underlying Property:** Annotated with `@Serializable` and exposing a public `.value` underlying property (e.g., `value class UserId(val value: String)`).
  2. **Canonical String Representation:** Provide `asString()` (or `asHexDashString()`) for obtaining the string representation.
  3. **Companion Generator:** Provide `generate()` companion method for generating a new random instance (when applicable, e.g., UUID generation).
  4. **Extension Parsers:** Provide `String.toXxxOrNull()` and `String.toXxxOrThrow()` extension functions in the same file for safe and strict parsing from raw strings.

---
*Refer to `AGENTS.md` for the full list of project standards.*
