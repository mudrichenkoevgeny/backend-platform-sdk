---
description: Language standards, link resolution rules, and KDoc Definition of Done
globs: "**/*.kt"
alwaysApply: true
---

# KDoc Style Guide — Basics

This document defines mandatory language and syntax standards for all public interfaces, DTOs, and infrastructure components within the `backend-platform-sdk`.

## 1. Language Standard
- **English Only:** All KDoc, README files, and API documentation must be written strictly in English. Never write documentation in other languages.

## 2. KDoc Links & Resolution
- **Simple Names Only:** Use only simple names that resolve via imports: `[Foo]` or `[SomeType.member]`.
- **No FQN in Links:** Fully qualified names in links (e.g., `[io.github.mudrichenkoevgeny.sdk.Foo]`) are strictly forbidden.
- **Import Requirement:** Every type referenced in a link must be imported in the current file to ensure the IDE can resolve the click-through.
- **Unreachable Targets:** If a type cannot be imported (e.g., it's in a module not reachable by the current one), use **plain text** without brackets.
- **No Deferred Prose:** Do not redirect the reader with phrases like "Same as [Other.CONST]". Every contract detail must be self-contained and fully described to prevent "click-hunting".

## 3. Scope Rules
- **Class vs. Member Scope:** In class-level KDoc, do not link to function parameters by name. Use `@param` only at the function/constructor level.
- **Package Documentation:** Documentation for a package (e.g., `core.audit`) must cover its subpackages (`database`, `manager`, `route`) to provide a holistic view of the feature's infrastructure.

## 4. Definition of Done (DoD)
A documentation task for a module or feature is considered complete ONLY if:
1. **Public Surface:** Every public `interface`, `class`, and `object` has a class-level KDoc summary.
2. **Members:** Every public `fun`, `val`, and `var` has its own KDoc.
3. **Contracts:** KDoc must include:
    - Summary of the operation/purpose.
    - `@param` for all parameters.
    - `@return` for non-`Unit` types.
    - `@throws` for expected domain or infrastructure exceptions.
4. **Side Effects:** For `Unit` functions (especially in Managers/Repositories), the summary must clearly state the outcome (e.g., "Persists the audit event and triggers a Redis broadcast").

## 5. Inline Comments vs. KDoc
- **No Narrative Comments:** Strictly forbidden to write comments that narrate the obvious flow of code.
- **Logic Clarity:** Prefer self-documenting code (naming and structure).
- **Constraints Only:** Use inline comments sparingly and only for non-obvious technical constraints, thread-safety notes, or complex external API quirks.

---
*Refer to `AGENTS.md` for the full list of project standards.*