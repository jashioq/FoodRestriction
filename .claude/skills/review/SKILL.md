---
name: review
description: Review the current change along two independent axes — project standards and ticket conformance. Reports findings only; fixes nothing and files nothing.
disable-model-invocation: true
argument-hint: "[#issue] [fixed-point] "
allowed-tools: Read, Grep, Glob, Bash, Task
---

Review the change between a fixed point and `HEAD` along two axes, run as two independent
reviewers whose findings are never combined.

Arguments, both optional and order-independent: an **issue reference** (`#12` or `12`) 
and a **fixed point** (commit, branch, or tag).

## 1. Resolve the fixed point

If given, verify it: `git rev-parse --verify <fixed-point>`.

If not given, default to the branch point off main: `git merge-base HEAD origin/main`.

If it doesn't resolve, stop and say so. Do not guess a different one.

## 2. Collect the diff

```
git diff --stat <fixed-point>...HEAD
git diff <fixed-point>...HEAD
```

- **Empty diff:** stop. Report that there is nothing to review. Do not review the working
  tree, uncommitted changes, or the last commit instead.
- **Over ~2000 changed lines:** stop and report the size. Ask whether to proceed anyway or
  narrow the range. A review this size produces findings nobody reads.

## 3. Resolve the ticket

In order, taking the first that yields something:

1. the issue reference passed as an argument;
2. an issue number found in the commit messages in range
   (`git log --format=%s%n%b <fixed-point>...HEAD`, looking for `#<number>`).

If you find one, fetch it: `gh issue view <number> --json number,title,body`.

If you find none, that is a normal outcome. The spec reviewer handles it. Do not
substitute the spec document, the branch name, or your own reading of the diff for a
ticket.

## 4. Read the standards

Read both files in full:

- `.claude/standards/CODING_STANDARDS.md`
- `.claude/standards/ARCHITECTURE.md`

You will paste their contents into the standards reviewer's prompt. It has no other way to
reach them — do not summarize, excerpt, or paraphrase them.

## 5. Spawn both reviewers

**Send a single message containing two Task calls**, one to `standards-reviewer` and one to
`spec-reviewer`, so they run in parallel and neither sees the other's reasoning.

Give **standards-reviewer**:
- the full text of both standards files,
- the full diff,
- the list of changed files.

Give **spec-reviewer**:
- the ticket's number, title, and body in full — or an explicit statement that no ticket
  was found,
- the full diff,
- the list of changed files.

Give each reviewer only its own axis's material. Do not pass the standards to the spec
reviewer or the ticket to the standards reviewer.

## 6. Report

Present both reports, each under its own heading, each with its own verdict:

```
# Review

`<fixed-point>`..HEAD — <n> files, +<x>/-<y>
Ticket: #<n> <title>   (or: none found)

## Standards
<the standards reviewer's report>

## Spec
<the spec reviewer's report>
```

**Do not merge, rank, or reconcile the two reports.** Do not produce a combined verdict,
an overall severity, or a single "top issues" list. Separating the axes is the entire
point of running two reviewers; collapsing them at the end throws that away. If the two
disagree, present both and say they disagree.

Add nothing of your own beyond the header line. You are not a third reviewer.

## Bounds

This skill reports. It does not act on what it finds.

- Make no edits to any file.
- Create no issues, no branches, no commits, no PRs.
- Propose no patches, and do not offer to apply fixes.

Triage of these findings is a separate step, run deliberately, in a later session.

## Completion criteria

- [ ] The fixed point resolved, or the skill stopped and said why.
- [ ] The diff was non-empty and within size, or the skill stopped and said why.
- [ ] Both standards files were read in full and pasted into the standards reviewer.
- [ ] The ticket was fetched and pasted in full, or its absence stated explicitly.
- [ ] Both reviewers were spawned from a single message.
- [ ] Both reports appear under separate headings with separate verdicts.
- [ ] No file was modified and nothing was created on GitHub.
