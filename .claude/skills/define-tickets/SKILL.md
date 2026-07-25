---
name: define-tickets
description: Break a spec into vertical-slice tickets and, once the user approves them, publish them as GitHub issues.
disable-model-invocation: true
argument-hint: "<path to spec file>"
allowed-tools: Read, Grep, Glob, Bash
---

Break the spec at the given path into implementation tickets, get them approved, then
publish them as GitHub issues.

Read the spec first. If the path is missing or unreadable, stop and say so — do not go
looking for a different spec or reconstruct one.

Then read `.claude/standards/ARCHITECTURE.md` and enough of the codebase to slice against
how this project is actually built — the layers each story touches, the seams that already
exist, the pattern a new screen or use case would follow. A breakdown written without
looking at the code is a guess.

## Slicing

Every ticket is a **vertical slice**: it cuts through whatever layers it needs and ends in
something demonstrable. "Add the repository method" is not a ticket. "Scan a barcode and
show the product name on screen" is, even with the allergen check stubbed out.

The test is the spec's Verification section — if you cannot say how a slice is
demonstrated, it is not a slice yet.

The first ticket is usually a tracer bullet: the thinnest path from entry point to visible
result, with everything hard stubbed. Later tickets replace the stubs one at a time.

Where a change would be awkward to make, make it easy first: a preparatory ticket that
reshapes the existing code without changing behavior, then the slice that uses it. Where a
mechanical change touches many files at once, split it into expand, migrate, contract
rather than one enormous slice.

Prefer few, thick tickets over many thin ones. Each will be implemented in its own session
with no memory of the others.

## Propose, then wait

Present the full breakdown before creating anything: each ticket's title, the slice it
delivers, and what it is blocked by.

Then raise your own doubts — one at a time, waiting for each answer. Where two tickets
might be one, where one might be two, where the ordering could be wrong, where a slice
cannot obviously be demonstrated. Give your recommendation with each.

**Publish nothing until the user explicitly approves the breakdown.** Their silence, their
answer to your last question, and your own confidence are not approval.

## Ticket format

Each issue body, written so that an agent opening it in a fresh session with no other
context can implement it:

```markdown
<One paragraph: what this slice delivers and what it looks like when it works.>

Spec: `<path to the spec file>`
Blocked by: #<n>, #<n>          (omit the line entirely if nothing blocks it)

## Requirements

- <specific, checkable, drawn from the spec's user stories>

## Verification

<How to demonstrate this works, concretely enough to follow.>

## Out of scope for this ticket

- <What belongs to a later slice, or to no slice at all.>
```

The body is the contract the change is reviewed against later, so requirements must be
stated here rather than left implicit in the spec. Out of scope is not optional — it is
what makes scope creep detectable.

## Publishing

Create issues in dependency order, blockers first, so their numbers exist before anything
references them:

```
gh issue create --title "<title>" --body "<body>" --label ready-for-agent
```

Capture each issue's number from the returned URL and substitute it into the `Blocked by:`
lines of the tickets that depend on it. Never publish a ticket whose blockers do not yet
have real numbers.

When done, report the issue numbers and titles in dependency order.

## Bounds

This skill produces tickets. It does not start on them.

Create issues and nothing else — no branches, no commits, no PRs, no edits to any file
including the spec. If the spec turns out to be wrong or incomplete, say so and stop;
fixing it means going back to `clarify-specs`, not patching it here.
