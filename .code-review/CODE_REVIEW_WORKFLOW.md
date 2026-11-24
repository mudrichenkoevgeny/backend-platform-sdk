# Code Review Workflow Guide

Universal cross-platform code review workflow for agent code generation.

## Directory Structure

- `.code-review/REVIEW_PROMPT.md` — Repository code-review prompt and checklist (versioned in Git).
- `.code-review/CODE_REVIEW_WORKFLOW.md` — Workflow guide and CLI documentation (versioned in Git).
- `.code-review/state.json` — Local SHA-256 hash cache for incremental review (ignored in Git).
- `.code-review/payload.md` — Temporary packed payload generated for reviewer chat (ignored in Git).

## Setup & Prerequisites

### 1. Installation

- **Web / Node.js Projects:** Install as devDependency:
  ```bash
  pnpm add -D @mudrichenkoevgeny/code-review
  ```
  Run via `pnpm code-review <command>` or `npx code-review <command>`.

- **Kotlin / KMP / Backend / Multi-repo Projects:**
  Run directly via `dlx` or `npx` without adding NPM dependencies:
  ```bash
  pnpm dlx @mudrichenkoevgeny/code-review <command>
  # or
  npx @mudrichenkoevgeny/code-review <command>
  ```
  Or install globally: `npm install -g @mudrichenkoevgeny/code-review`.

### 2. `.gitignore` Configuration

Ensure local state and payload files are ignored:
```gitignore
.code-review/state.json
.code-review/payload.md
```

## CLI Commands

- `code-review init`
  Initializes `.code-review/` directory, writes `.code-review/CODE_REVIEW_WORKFLOW.md`, and creates starter `REVIEW_PROMPT.md` if missing.
- `code-review prompt`
  Copies `.code-review/REVIEW_PROMPT.md` to system clipboard and writes `.code-review/payload.md`.
- `code-review pack [paths...] [--all] [--mark]`
  Packs unstaged and untracked git changes into `.code-review/payload.md` and copies to system clipboard.
  - Optional `[paths...]`: Filters changes by directory or file path prefixes.
  - `--all`: Forces packing all candidate files, ignoring cache.
  - `--mark`: Marks candidate files as reviewed without copying or writing payload.
- `code-review update-prompt`
  Scans `AGENTS.md` and `.agent/**/*.md` rule files, builds meta-prompt in `.code-review/payload.md`, and copies to clipboard.

Note: In headless or SSH environments where clipboard access is unavailable, payload text is safely saved to `.code-review/payload.md` for manual file attachment.

## Iterative Review Workflow

### Step 1: Initialize Reviewer Chat
Run `code-review prompt`. Paste clipboard (or attach `.code-review/payload.md`) into a fresh Reviewer agent chat window.
Wait for the handshake response: `Ready to review.`.

### Step 2: Pack Changes After Coding Agent Finishes
After Coding agent completes code modifications, run `code-review pack`.
Paste clipboard (or attach `.code-review/payload.md`) into the Reviewer agent chat.

### Step 3: Handle Review Verdict
- **Passed Files:** Run `git add <file>` to stage verified clean files. Staged files are automatically excluded from subsequent `code-review pack` runs.
- **Failed Files:** Copy English fix prompt (`text` code block) from Reviewer agent response into Coding agent chat.
- **Re-pack:** After Coding agent finishes fixes, run `code-review pack` again. Only newly modified unstaged files are packed.

### Step 4: Finalize
Repeat Step 3 until Reviewer agent responds with `No issues found.`.

## Keeping `REVIEW_PROMPT.md` in Sync

Whenever project standards, architecture, or `.agent/**/*.md` rules change:
1. Run `code-review update-prompt`.
2. Paste clipboard (or attach `.code-review/payload.md`) into an agent chat.
3. Save generated Markdown response to `.code-review/REVIEW_PROMPT.md`.

## Cache & Exclusions

- **Excluded from Pack:** Staged files, deleted files, `.code-review/` state, lockfiles (`pnpm-lock.yaml`),
  generated files (`routeTree.gen.ts`, `gradle-wrapper.jar`), and binary assets (`png`, `jpg`, `webp`, `woff2`, `ttf`, `otf`, `ico`, `svg`, `jar`, `pdf`, `zip`).
- **Resetting Cache:** Delete `.code-review/state.json` or pass `--all` to force packing all modified files.
