---
name: standards
description: The project's coding and architecture standards. Consult before writing or changing Kotlin in this repo — repository/use-case/ViewModel patterns, Result and Flow conventions, DI wiring, layering, networking, config and secure storage.
allowed-tools: Read
---

Load this project's standards, then follow them for the code you are about to write.

Read both files in full:

- `.claude/standards/CODING_STANDARDS.md` — per-file rules: the repository pattern, `Result`
  at suspend boundaries, reads as `Flow`, use-case shape, DTO placement, ViewModel and
  Compose conventions.
- `.claude/standards/ARCHITECTURE.md` — structure: layering, source sets and `expect`/`actual`,
  Koin module split, networking, config and secrets, secure storage.

These describe how this codebase does things, which is not always how Kotlin is done in
general. Where your default instinct and a documented rule disagree, the document wins.

Apply them to the change at hand — don't recite them back. If the change runs against a
standard for a real reason, say so explicitly rather than following it silently or breaking
it silently.
