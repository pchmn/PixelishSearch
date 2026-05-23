---
name: write-pr
description: >
  Create or update a GitHub Pull Request using the `gh` CLI. Automatically detects whether
  a PR already exists for the current branch: if yes, updates it; if no, creates one.
  PR titles follow conventional commits format (feat:, fix:, chore:, etc.) for squash-merge
  workflows. Use this skill whenever the user wants to create a PR, update a PR, modify a PR
  description, open a pull request, push changes, submit for review, or says things like
  "create a PR", "open a PR", "update the PR", "push and create PR", "submit PR", or "/write-pr".
---

# PR Skill

## Interaction Rules

- For all confirmations, choices, and decision points: use the agent's native interactive
  question/choice tool to present selectable options. Do not list options as plain text — the user
  must be able to click/select, not type.

Create or update a GitHub Pull Request. The title uses conventional commits format because the repo
uses squash-merge — the PR title becomes the final commit on `main`.

## Workflow

1. Gather context: check branch (stop if on `main`), review status/diff/log against `main`. If there
   are uncommitted changes, offer selectable options ("Commit first" / "Continue without
   committing" / "Cancel").

2. Check if a PR already exists for this branch with `gh pr view`. Tell the user which flow you're
   following (create or update).

3. **Create flow**: list open issues with `gh issue list` and present them as selectable options (
   e.g., "#12 - Fix login bug" / "#15 - Add search" / "None"). The selected issue gets a `Closes #N`
   line in the body.

4. **Update flow**: if the branch has new commits since the PR was created, regenerate title and
   body from the full diff. Otherwise ask the user what they'd like to change.

5. Analyze the full diff and commit log. Determine the type (`feat`, `fix`, `chore`, etc.), optional
   scope, and intent.

6. Generate title and body:
    - **Title**: conventional commits format, under 70 chars, imperative mood, lowercase after
      colon.
    - **Body**: `- Closes #N` (if applicable), then `## Summary` (2-4 bullets), then
      `## Test plan` (checklist).

7. Push and submit immediately — show the generated title/body inline but do not ask for
   confirmation. Display the PR URL when done.
