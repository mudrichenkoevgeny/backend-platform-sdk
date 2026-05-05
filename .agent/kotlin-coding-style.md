---
description: Naming, when subject rules, brace requirements, and FQN/Comments ban
glob: "**/*.kt"
alwaysApply: true
---

# Kotlin Coding Style

This document defines the mandatory Kotlin syntax and formatting standards for the `shared-foundation` project.

## 1. Core Syntax Constraints

### Fully Qualified Names (FQN)
- **Strict Ban:** Do not use FQN in expressions, type annotations, or generics.
- **Resolution:** Use `import` or `typealias`.
- **Exception:** Minimal FQN is allowed only when imports are technically impossible or cause unresolvable ambiguity (justify in the task summary).

### Comments
- **Strict Ban:** Do not write or preserve comments in the code.
- **Self-Documentation:** Logic must be clear through expressive naming and clean structure. If a complex algorithm requires explanation, refactor it into smaller, well-named functions.

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
- **Domain Focus:** Use concrete names tied to the domain or operation (e.g., `policyValidationResult`), not vague placeholders like `result` or `data`.
- **Outcome Clarity:** When a variable holds the outcome of a step (especially a result-shaped type), include the outcome type in the name (e.g., `PasswordPolicyValidatorResult` -> `validationResult`).

### Property Naming (DTOs)
- **JSON Parity:** Always use `@SerialName` with constants from `*ApiFields`.
- **Snake Case vs Camel Case:** While Kotlin properties use `camelCase`, ensure they correctly map to the project's `snake_case` wire format via constants.

## 4. Expression vs Body Functions
- **Simple Mappers:** Use expression-body functions (`fun toPayload() = ...`) for simple transformations and mappers.
- **Complex Logic:** Use block-body functions (`fun process() { ... }`) for anything involving multiple steps or branching logic to ensure readability without comments.

## 5. Sealed Types and Enums
- **Exhaustiveness:** When using `when` on `sealed class` or `enum`, do not provide an `else` branch unless it is logically unavoidable. Ensure all cases are handled explicitly.

---
*Refer to `AGENTS.md` for the full list of project standards.*