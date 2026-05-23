# Store disabled tile IDs, not enabled ones

Tile visibility is persisted as the *disabled* set
(`stringSetPreferencesKey("disabled_tile_ids")`) — empty set means all tiles
visible. We chose this over storing the enabled set so that any tile added to
`settingsTiles` in a future release is automatically visible to existing
users, with no data migration.

## Considered options

- **Set of enabled IDs** — rejected: new tiles would default to hidden for
  existing users, requiring a migration each time `settingsTiles` grows.
- **One `boolean` preference per tile** — rejected: 10+ keys in DataStore,
  and every new tile would require touching `PreferencesRepository`.
