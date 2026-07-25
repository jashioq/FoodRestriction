---
name: open-pr
description: Open a pull request from the current branch, resolving conflicts against the base. Opens the PR; never merges it.
disable-model-invocation: true
argument-hint: "[base-branch, default main]"
allowed-tools: Read, Grep, Glob, Bash
---

Open a pull request from the current branch. The optional argument is the base branch;
default `main`.

## 1. Check the branch is PR-ready

- `git branch --show-current` — if it's the base branch itself, stop; there's nothing to
  PR. You do not create branches.
- `git status` — if there are uncommitted changes, stop and report them. Whoever ran
  `implement` should commit first; this skill doesn't commit work for them.
- `git log <base>..HEAD` — confirm there are commits to propose. If the branch is even with
  base, stop and say so.

## 2. Resolve conflicts against the base

```
git fetch origin
git merge origin/<base>
```

If it's clean, continue. If it conflicts, resolve the conflicts — reading enough of both
sides to resolve them correctly, not just mechanically — then commit the merge. If a
conflict is genuinely ambiguous and you can't tell which side is right, stop and ask rather
than guessing; a wrong conflict resolution is easy to miss in review.

## 3. Push the branch

```
git push -u origin HEAD
```

Plain push only. Never force-push (it's blocked anyway) — if a plain push is rejected,
something's wrong with the branch state; stop and report it rather than forcing.

## 4. Open the PR

Gather the commits (`git log <base>..HEAD`) and the tickets they reference, and open:

```
gh pr create --base <base> --title "<title>" --body "<body>"
```

The body: a short paragraph on what the PR does and why, the issues it closes
(`Closes #<n>` for each fully-delivered ticket), and a one-line note of anything a reviewer
should look at first. Keep it informative and short — this is a summary, not a spec.

## 5. Report and stop

Report the PR URL. **Do not merge it** — the user reviews and merges manually. Do not
approve it, and do not enable auto-merge.

## Bounds

This skill opens exactly one PR from an existing branch. It does not create branches, write
or amend implementation code, force-push, merge, or file issues. Its only writes to the
repo are a conflict-resolution merge commit and the push.

## Completion criteria

- [ ] Current branch is not the base and has commits ahead of it.
- [ ] No uncommitted changes were present, or the skill stopped.
- [ ] The base was merged in and any conflicts resolved (or the skill stopped to ask).
- [ ] The branch was pushed with a plain push.
- [ ] A PR was opened against the base with a summary body and `Closes #` links.
- [ ] The PR URL was reported and the PR was left unmerged.
