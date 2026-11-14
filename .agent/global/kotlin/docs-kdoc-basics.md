---
description: Language standards, link resolution rules, and KDoc Definition of Done
glob: "**/*.kt"
alwaysApply: true
---

# KDoc Style Guide — Basics

This document defines mandatory language and syntax standards for all public interfaces and documentation within the project.

## 1. Language Standard

- **English Only:** All KDoc, README files, and API documentation must be written strictly in English. No exceptions.

## 2. KDoc Links & References

- **Simple Names Only:** Use only simple names that resolve via imports: `[Foo]` or `[SomeType.member]`.
- **No FQN in Links:** Fully qualified names in links (e.g., `[full.package.Foo]`) are strictly forbidden.
- **Import Requirement:** Every type referenced in a link must be imported in the current file.
- **Avoiding Unused Imports:** If a type is referenced only in KDoc prose (e.g., `Response body: [SomePayload]`) and not in executable code, add `@see SomePayload` at the bottom of the KDoc block. This keeps the `import` statement active for linters (Ktlint/Detekt) and generates a hyperlinked `@see` reference in Dokka documentation.
- **Unreachable Targets:** If a type cannot be imported (e.g., from a consumer module), use **plain text** without brackets.
- **No Deferred Prose:** Do not redirect the reader to other KDocs. Phrasing like "Same as [Other.CONST]" is forbidden. Repeat the contract details fully to ensure each constant is self-contained.

## 3. Scope Rules

- **No Parameter Links in Class-level:** Do not link to function parameters by name in the class-level KDoc. Move such links to the function-level `@param`.
- **Package Documentation:** When documenting a package, do not create "KDoc-only" Kotlin files. Apply documentation to the primary interfaces/classes within that package instead.

## 4. Definition of Done (DoD)

A documentation task is considered complete ONLY if:
1. **Public Interfaces:** Every public `interface` has a class-level KDoc AND every public member (`fun`/`val`) has its own KDoc.
2. **Members:** Member KDoc must include a summary, `@param` for all parameters, and `@return` for non-`Unit` types.
3. **Unit Returns:** For `Unit` functions, the summary must clearly state the outcome (success/failure behavior).

## 5. Inline Comments vs. KDoc

- **No Agent-Generated Inline Comments or TODOs:** AI agents must NOT write inline implementation comments (`//` or `/* ... */`) or generate new `TODO` / `FIXME` markers unless explicitly requested by the developer. Decompose complex multi-step logic into private helper functions with self-documenting names.
- **Preserve Existing Developer TODOs:** Never delete or strip pre-existing `// TODO:` or `// FIXME:` comments left by the developer unless the task explicitly resolves that specific TODO item.
- **MANDATORY KDoc Preservation:** KDoc blocks (`/** ... */`) are structured API documentation, NOT code comments. AI agents must NEVER delete, strip, wipe, or omit KDoc documentation during refactorings, updates, or code edits. All KDoc must be kept and updated.

---
*Refer to `AGENTS.md` for the full list of project standards.*
