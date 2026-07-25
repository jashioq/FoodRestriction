---
name: clarify-specs
description: Interrogate a feature idea until you and the user share the same understanding of it. Asks questions; writes nothing.
disable-model-invocation: true
argument-hint: "[what you want to build]"
allowed-tools: Read, Grep, Glob
---

Interview the user relentlessly about what they want to build, until you both understand
it the same way.

Walk down each branch of the decision tree, resolving dependencies between decisions one
at a time. Ask between one and five questions per message and wait for the answer. Suggest one
recommended answer with each question and a one-line reason. You can ask either multiple choice
questions or text input questions.

If a fact can be found by exploring the repository, look it up rather than asking. The
decisions, though, are theirs: put each one to them and wait.

When the questions run out, summarize the shared understanding and suggest `write-spec-doc` as the
the next step, but do not run anything yourself.
