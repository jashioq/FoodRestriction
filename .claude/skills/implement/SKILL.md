---
name: implement
description: Implement one GitHub issue end to end on the current branch, then summarize the change. Builds what the ticket specifies and nothing more.
disable-model-invocation: true
argument-hint: "#<issue>"
allowed-tools: Read, Grep, Glob, Edit, Write, Bash
---

Implement a single GitHub issue on the current branch.

The argument is an issue reference (`#12` or `12`). If none was given, ask for it.

## 1. Understand the ticket

Fetch it: `gh issue view <number> --json number,title,body,labels`.

Read its Requirements, Verification, and Out-of-scope sections. The body is the contract —
if it says `Blocked by: #<n>`, check those are actually done before starting; if they
aren't, stop and say so.

If the ticket is ambiguous or contradicts what the code actually looks like, stop and ask.
Don't resolve a real ambiguity by guessing — a wrong guess here is expensive downstream.

## 2. Load the standards

Follow the `standards` skill before writing Kotlin, so the change matches this project's
repository / use-case / ViewModel / Result / Flow / DI conventions rather than generic
Kotlin habits.

## 3. Confirm the branch

You commit to the **current** branch. Run `git branch --show-current` and `git status`.
If you're on `main`, stop and tell the user to create and switch to a feature branch —
you do not create branches. If there are unrelated uncommitted changes, flag them.

## 4. Implement — only what the ticket asks

Build exactly what the Requirements specify. Work at the seams the spec and architecture
already imply; don't introduce a new abstraction unless the ticket calls for it.

Anything in the ticket's Out-of-scope section stays out, even if it's tempting and nearby.
Unrequested behavior isn't a bonus — it wasn't specified, wasn't agreed, and will be
flagged as scope creep in review. If you spot something worth doing that's outside the
ticket, note it for the summary; don't build it.

Iterate against the language server, not Gradle. Save the Gradle compile checks for
verification at the end.

## 5. Verify

Run the compile checks from `CLAUDE.md`:

```
./gradlew :shared:compileAndroidMain
./gradlew :shared:compileKotlinIosSimulatorArm64
```

Both must pass. If real tests cover the area, run them. Fix what you broke before
committing — a red compile is not "done".

## 6. Commit

Commit to the current branch with a message that references the issue:

```
git add <the files you changed>
git commit -m "<concise description> (#<number>)"
```

Stage only files relevant to this ticket. Do not `git add -A` over unrelated changes.

## 7. Summarize

Write a short summary, for the human who reviews next:

- what changed, by area (use case / repository / screen / DI / wiring);
- any requirement you couldn't fully meet, and why;
- anything you noticed but deliberately left out of scope;
- how to demonstrate it works, per the ticket's Verification.

Then stop. Review is `review`, run separately. Do not open a PR — that's `open-pr`.

## Bounds

This skill implements one ticket. It does not review its own work, file issues, open PRs,
create branches, or start on the next ticket. It commits only to the current branch and
never pushes or merges.

## Completion criteria

- [ ] The ticket was fetched and its blockers confirmed done.
- [ ] Standards were loaded before writing code.
- [ ] Work happened on a feature branch, not `main`.
- [ ] Only the ticket's requirements were built; out-of-scope items were left out.
- [ ] Both compile checks pass.
- [ ] A commit referencing the issue was made, staging only relevant files.
- [ ] A change summary was written; no PR was opened and no branch was created.
