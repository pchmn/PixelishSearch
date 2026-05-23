---
name: write-skill
description: >
  Create new skills in the project's .agents/skills/ directory. Use when the user wants to create (or update)
  a skill from scratch, scaffold a new skill, or says things like "create a skill", "update a skill", "make a skill
  for X", "new skill", or "/write-skill". Skills are created in .agents/skills/ — the shared
  directory that any AI agent (Claude Code, OpenCode, Cursor, etc.) can discover.
---

# Skill Creator

Create new skills in `.agents/skills/`.

## Context

Skills live in `.agents/skills/<skill-name>/SKILL.md` with YAML frontmatter (name + description).
This format is agent-agnostic — any AI coding agent that reads markdown can use them. Check existing
skills in `.agents/skills/` first to understand conventions and avoid duplicates.

## Workflow

1. **Understand intent.** Extract what you can from conversation history before asking. Key things
   to clarify: what the skill does, when it should trigger, expected output, whether it needs
   bundled resources.

2. **Create the skill directory and `SKILL.md`.** Only add subdirectories if actually needed:
    - `scripts/` — deterministic code that shouldn't be rewritten each time
    - `references/` — docs loaded into context (schemas, API docs). For large files (>10k words),
      include grep patterns in SKILL.md so the agent can find what it needs.
    - `assets/` — files used in output, not loaded into context (templates, images)

3. **Write SKILL.md.**

   Frontmatter:
    - `name`: kebab-case, max 64 chars
    - `description`: max 1024 chars, no angle brackets. Include what the skill does AND when to
      trigger it — be "pushy" to combat undertriggering (e.g., "Create dashboards. Use whenever the
      user mentions dashboards, data visualization, metrics, or wants to display any kind of
      data.").

   Body — writing principles:
    - **Only instruct what the LLM wouldn't do naturally.** A skill that says "run git status"
      wastes tokens. A skill that says "never push to main" adds real value. The test: would a
      capable LLM already do this without being told? If yes, skip it.
    - **Keep it concise.** Numbered workflow steps. No bash command blocks for standard tools. Aim
      for 30-80 lines of body depending on complexity.
    - **Include domain-specific knowledge** the LLM can't infer: project conventions, API patterns,
      template structures, safety rules.
    - **Use `references/` for overflow** — code examples, schemas, large templates. Keep SKILL.md as
      the concise orchestrator.
    - **Minimize confirmations.** Operational skills (git, PR, deploy): 0-1 prompts in the happy
      path. Interactive skills (PRD, code review): ask only for genuine decisions, not rubber-stamp
      approvals.

   **Every skill MUST include** the following section at the top of the body (after the title),
   before any skill-specific content:

   ```markdown
   ## Interaction Rules

   - For all confirmations, choices, and decision points: use the agent's native interactive question/choice tool to present selectable options. Do not list options as plain text — the user must be able to click/select, not type.
   ```

4. **Validate**: frontmatter exists, name is kebab-case, description has no angle brackets and is
   under 1024 chars. Verify the `## Interaction Rules` section is present.

5. **Suggest 2-3 test prompts** — realistic things a user would say to trigger the skill.
