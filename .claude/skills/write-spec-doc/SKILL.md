---
name: write-spec-doc
description: Turn a settled clarification conversation into a spec file under docs/specs/. Synthesizes only; asks nothing new.
disable-model-invocation: true
allowed-tools: Read, Grep, Glob, Write
---

Write the feature spec for the understanding already reached in this conversation.

Takes no arguments. Everything it needs is in the session.

## Precondition

This runs after `clarify-specs`, in the same session, once the user has confirmed the
summary.

**If this session contains no settled clarification conversation, stop.** Say that there is
nothing to synthesize and that `clarify-specs` comes first. Do not reconstruct a spec from
a one-line request, from the codebase, or from a branch name. A spec invented rather than
agreed is worse than no spec, because everything downstream will treat it as settled.

## Do not re-interview

The questions are over. Do not ask anything new, do not reopen decisions, and do not
improve on what was agreed. You are writing down what was decided, not deciding.

Where something genuinely was not resolved, it goes under **Open questions** — it does not
get an answer invented for it here.

## Output

Write to `docs/specs/<slug>.md`, where `<slug>` is a short kebab-case name for the feature
taken from the conversation. If the file exists, stop and ask before overwriting.

Use exactly this structure:

```markdown
# <Feature name>

## Problem

<What is broken or missing today, and for whom. Two or three sentences.>

## Solution

<The shape of the approach, high level. What changes from the user's point of view.
A short paragraph — not a design document.>

## User stories

1. As a <user>, I want <capability>, so that <outcome>.
2. ...

<Numbered, each one independently checkable. Cover the awkward states — empty, loading,
failure, offline — as their own stories where they were discussed, not as footnotes to
the happy path.>

## Implementation decisions

- **Layers touched:** <use cases, repositories, screens — new and modified.>
- **Seams:** <where the boundary sits; whether an existing one is being reused or a new
  one introduced, and why.>
- **Platform:** <expect/actual needs, Android/iOS differences, or "common only".>
- **Data:** <persistence, auth, session handling — or "none".>
- <Other decisions reached during clarification, one per line.>

## Verification

<How each user story is demonstrated to work. Manual steps are fine and expected — state
them concretely enough that someone else could follow them. Note anything that can be
checked by compiling or by an automated test where one exists.>

## Out of scope

- <Explicitly excluded, from the boundaries discussion. One per line.>

## Open questions

- <Anything genuinely unresolved. Omit the section entirely if there are none.>
```

## After writing

Report the path and stop.

Do not create issues, do not draft tickets, do not begin implementing, and do not offer to.
Ticket breakdown is `define-tickets`, run by the user in a fresh session with the spec path
as its argument.

## Completion criteria

- [ ] A settled clarification conversation existed in this session, or the skill stopped.
- [ ] No new questions were asked and no decisions were changed.
- [ ] Every section is present, in order, with nothing invented to fill it.
- [ ] Unresolved items are under Open questions rather than answered.
- [ ] The file was written under `docs/specs/` and its path reported.
- [ ] Nothing was created on GitHub.
