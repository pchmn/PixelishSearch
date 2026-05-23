# Rename top-level `settings/` to `preferences/`

The codebase had two packages literally named `settings/`: `search/settings/`
(Android system Tiles and Settings pages exposed in search) and a top-level
`settings/` (PixelishSearch's own user-controlled config — contact toggle,
tile visibility, language picker). Both produced classes prefixed `Settings*`
in the same module, and CONTEXT.md used "Settings" in two senses without
distinguishing them ("user-controlled in the app Settings" vs. "a sub-page
of the Android Settings app").

Going forward, **Setting** always refers to the Android system Settings app
(scope of `search/settings/`), and **Preference** refers to a PixelishSearch
user-controlled value persisted in DataStore (scope of `preferences/`). The
top-level package and everything inside it — package path, `*Activity`,
`*Repository`, `*Screen`, DataStore file and delegate, the field on
`PixelishSearchApp`, the `R.string.*` keys — rename to `preferences*`. The
displayed UX label of the screen stays "Settings" (Android convention); only
the internal identifiers change. `search/settings/` is untouched.

The codebase was already drifting this way before the rename: UI rows were
named `PreferenceRow` and `SwitchPreference`, `SettingsRepository`'s KDoc
read "User preferences.", and ADR-0003 is titled
`prefs-live-with-write-initiator`. The rename catches the code up with the
language already in use.

## Considered options

- **Rename `search/settings/` to `system_settings/` instead** — rejected: in
  everyday Android talk "Settings" canonically refers to the system app, so
  qualifying every reference as "system" is more friction than renaming the
  smaller, more recent top-level package.
- **Keep both as `settings/` and disambiguate via import paths** — rejected:
  the collision already produces human confusion (this ADR's instigating
  question), and the glossary-level overload in CONTEXT.md would remain
  regardless of import paths.
