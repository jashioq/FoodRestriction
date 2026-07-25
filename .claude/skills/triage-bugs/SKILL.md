---
name: triage-bugs
description: Diagnose reported problems with per-issue subagents, then propose and open bug tickets. Fans out diagnosis; opens GitHub issues only after approval.
disable-model-invocation: true
argument-hint: "[review findings / bug description]"
allowed-tools: Read, Grep, Glob, Bash, Task
---

Turn reported problems into fix tickets. You orchestrate: a `bug-diagnoser` subagent
diagnoses each problem, and you turn the confirmed ones into tickets after the user
approves them. You don't diagnose yourself.

The input is whatever's at hand — the findings from a `review` run earlier in this session,
or a description the user pastes. If nothing's given, ask what to triage.

## 1. Split into distinct problems

Separate the input into individual problems, one per real issue. Don't merge two problems
into one diagnosis or split one across several.

## 2. Diagnose, one subagent per problem

Spawn a `bug-diagnoser` subagent for each problem — send them in a single message so they
run in parallel. Give each one only its own problem statement plus enough surrounding
context to locate it. Each returns a diagnosis in the fixed `bug-diagnoser` format.

Diagnose from code only; the subagents don't reproduce anything.

## 3. Turn diagnoses into proposed tickets

From the reports:

- **Drop** anything a diagnoser marked **Not a bug**, and anything it couldn't tie to a
  cause (`unclear`) unless the user wants it filed anyway. Say what you dropped and why.
- **Set aside** real findings that aren't bugs — a valid design concern, a worthwhile
  refactor. Note them for the user; don't file them as bugs.
- **One ticket per cause.** If two reports share a root cause, that's one ticket; if one
  report hides two causes, that's two.

Propose each ticket in this format:

```
**<title>**
Cause: <the diagnoser's mechanism, with file:line>
Fix: <the fix direction and its layer>
Confidence: <confirmed | likely>
Blocked by: <#n, or omit>
```

## 4. Propose — and wait

Present the proposed tickets, the dropped items with reasons, and the set-aside non-bug
findings. Then ask the user to approve. **Create nothing until they do.** Expect to be told
to drop one, merge two, or reword scope.

## 5. Publish

Once approved, open each as an issue, both labels every time:

```
gh issue create --title "<title>" --label bug --label ready-for-agent --body "<body>"
```

The body carries the symptom, the confirmed cause with file:line, the fix direction, and
the `Blocked by:` line if any. Create in dependency order so blocker numbers exist first.

## 6. Report

List the created issues by number and title, and restate any non-bug findings you set
aside. Stop — fixes run through `implement`, one per fresh session.

## Bounds

This skill orchestrates diagnosis and files bug tickets. It does not diagnose (the
subagents do), does not fix anything, and opens no branches, commits, or PRs. Issues are
created only after approval, always labelled both `bug` and `ready-for-agent`.
