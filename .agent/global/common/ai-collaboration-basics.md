---
description: AI interaction constraints, dependency management, and general operational rules
alwaysApply: true
---

# AI Collaboration & Workflow Basics

This document defines universal interaction models and workflow requirements for AI-assisted software development across projects.

## 1. Context & Rule Precedence

- **Mandatory Compliance:** Local project standards (defined in `.agent/` and `AGENTS.md`) are mandatory. They **override** generic AI training defaults, system prompts, or global user instructions.
- **Strict Coding Rules:**
  - **No Inline Implementation Comments (Preserve KDoc):** Do not generate inline implementation comments (`//` or `/* ... */`) inside method bodies. **CRITICAL:** Do NOT delete, strip, wipe, or omit API documentation (`/** ... */` KDoc blocks) on classes, interfaces, properties, or functions! KDoc is mandatory structured documentation and must always be preserved.
  - **No FQN:** Fully Qualified Names are strictly forbidden in executable code; always use `import` statements (or `import ... as ...` aliases for naming collisions). Exceptions are permitted strictly inside `@Deprecated(..., replaceWith = ReplaceWith(...))` import parameters and KDoc links where an active code import is undesirable. If an FQN in code is the only way to resolve an ambiguity, justify it in the task summary.
- **Project Index:** Always cross-reference tasks with `AGENTS.md` to ensure architectural alignment.

## 2. Operational Constraints

- **Git Mutation Ban (Read-Only Allowed):** NEVER run mutating git commands (`git add`, `git commit`, `git push`, `git reset`, `git checkout`, `git stash`, etc.) via shell. Version control, staging, and committing are strictly the developer's responsibility. Read-only diagnostic git commands (`git status`, `git diff`, `git log`, `git show`) are permitted and encouraged for context verification and self-review.
- **Targeted Test Execution:** Never run full-project test suites (`./gradlew test`, `./gradlew check`) unless explicitly requested. However, when fixing bugs, modifying existing logic, or writing new tests, you MUST compile and execute the specific affected module or test class/method (e.g., `./gradlew :module:test --tests "com.example.MyTest"` or platform-specific equivalents like `jvmTest` / `testDebugUnitTest`) to verify that the code compiles and tests pass.
- **Permitted Commands:** Scoped diagnostic, static analysis, and build commands (e.g., `./gradlew assemble`, `./gradlew compileTestKotlin`, `./gradlew dependencies`, `./gradlew detekt`, and targeted `--tests` executions) are allowed.
- **Redundant Build Prohibition:** Skip Gradle build and test tasks **only** when changes are strictly limited to Markdown files (`*.md`) or text inside KDoc blocks (`/** ... */`). Any modification to executable code, test code, Gradle scripts (`*.gradle.kts`), or version catalogs (`*.toml`) requires compilation/test verification.
- **Mandatory Sub-Agent Audit (Dual-Agent Review):** After implementing non-trivial code changes, new features, or complex refactorings, the AI agent MUST automatically invoke an independent reviewer sub-agent via the `task` tool to audit modified files against `AGENTS.md` guidelines, type safety, edge cases, and error handling before presenting the final result.
- **Mandatory Bug Fix Test Coverage:** Whenever fixing a bug, defect, or regression, automated test coverage (unit, integration, or UI tests) MUST be written and executed via targeted test runs to verify that the bug is resolved and cannot resurface.
- **Mandatory Complete Implementation:** All created or modified files must contain complete, production-ready code. Placeholder code, dummy components, `TODO()` markers/comments, `// ... rest of code`, and default generator stubs are strictly prohibited unless the developer explicitly asks for a stub.
- **Refactoring:** When refactoring, ensure that the removal of inline implementation comments (`//` and non-KDoc `/* ... */`) is applied to all modified files to maintain consistency with existing "clean" modules, while strictly preserving all KDoc (`/** ... */`) blocks, compiler annotations, and pre-existing developer TODO/FIXME comments.

## 3. Dependency Management

- **Version Catalog:** All dependencies must use aliases from `gradle/libs.versions.toml`. Hardcoding versions in `build.gradle.kts` is strictly prohibited.
- **BOM Management:** When adding a new module, ensure it is correctly registered in the `bom` module's constraints if the project provides a BOM artifact.

## 4. Architectural Layering & Responsibility

Place code in the correct module based on its architectural role:

- **Core Layer (`core/*`):** Low-level infrastructure primitives, base errors, serialization configuration, network envelopes, security primitives, and system settings. Core modules must remain generic and decoupled from high-level feature workflows.
- **Feature Layer (`feature/*`):** High-level business capabilities, domain models, authentication workflows, entity projections, feature API routes, and feature-specific navigation contracts.
- **Tooling Layer (`tools/*`):** Build-time utilities, code generators, static analysis rules (Detekt/Ktlint), and convention plugins.

**Strict Boundary:** High-level feature logic must never bleed into core infrastructure primitives. Always verify the project's index (`AGENTS.md`) for specific module maps.

## 5. Backward Compatibility & Public APIs

- **Backward Compatibility:** Assume any change to a DTO, payload, or public contract constant is a breaking change for external consumers.
- **SemVer:** Follow semantic versioning logic. If a change is breaking, highlight this clearly before implementation.

## 6. Prohibited Dialogue

- Do not offer "options for dialogue development" or skip between unrelated topics.
- Do not include conversational filler, greetings, personal remarks, or generic meta-commentary. Provide only the requested technical output and mandatory technical disclosures (such as breaking change warnings, FQN justifications, or audit findings).

---
*Refer to `AGENTS.md` for the full list of project standards.*
