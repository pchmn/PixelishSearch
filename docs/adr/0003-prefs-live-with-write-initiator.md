# Persist preferences with the feature that initiates writes

A persisted user preference lives in the data layer of the feature that
initiates its primary write path, not the feature that owns the underlying
concept. `disabledTileIds` and `contactSearchEnabled` are written only from
the Preferences UI, so they live in `preferences/data/PreferencesRepository`.
Hidden apps (Recents exclusion — see `CONTEXT.md`) are written primarily
from the Search UI long-press, so `HiddenAppsRepository` stays in
`search/apps/data/` even when the Preferences UI later exposes an "unhide"
screen consuming the same repo.

Cross-feature consumption flows in one direction: the Preferences UI is
transverse by nature and imports repos from other features when it needs to
display or manage them — never the reverse. `TilesScreen` already follows
this pattern by reading `settingsTiles` defined in `search/settings/data/`.

## Considered options

- **Centralize all prefs in `PreferencesRepository`** — rejected: forces
  `search/apps` writers to import from `preferences/data/`, reversing the
  established direction; `PreferencesRepository` becomes a god-object
  decoupled from the Preferences *UI* that gave it its name.
- **Strict feature-first (every concept's pref lives with that feature)** —
  rejected: scatters single-purpose Settings-UI-only repos across features
  (`contactSearchEnabled` would need a one-key repo in
  `search/contacts/data/`), multiplying DataStore files and obscuring that
  those toggles are Settings artifacts.
