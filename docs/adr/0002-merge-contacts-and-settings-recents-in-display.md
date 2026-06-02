# Merge contact and settings-page recents in the blank-state display only

Contact entries and Settings pages keep separate `HistoryRepository`s and
separate result sections in query mode, but the blank state fuses their
recent-use records into a single block capped at 2, ranked by
`HistoryEntry.score()` (usage × 14-day exponential decay, tiebreaker
`lastUsedEpochMillis` DESC). This mirrors the Pixel Launcher blank state and
keeps that surface compact — every other category (apps, web) still gets its
own block.

The fusion happens in `SearchViewModel` only; the two repositories stay typed
and colocated with their feature, each one continuing to feed its own
category's result ranking when a query is active (via `scoreOf` hooks on
`ContactRepository.search` and `SettingsPageIndex.search`). Stale settings
history entries (whose `ComponentName` no longer resolves in
`SettingsPageIndex.entries`) are filtered at display time, not deleted — if the
page reappears after a rollback, the entry resurfaces with its score intact.

## Update (ADR-0008)

App shortcuts joined as a **third** fused source on the same terms: a separate
typed `ShortcutHistoryRepository` feeding its own `scoreOf` in query mode, fused
only at display via a third `RecentEntity.Shortcut` variant, capped at the same 2,
with the same stale-filtering (entries whose key is no longer in `ShortcutIndex`
are hidden, not deleted). The fused block is now contacts + settings pages +
shortcuts.

## Considered options

- **One block per category in the blank state** — rejected: adds a 4th distinct
  section to an already dense surface, and diverges from the Pixel UX we mirror.
- **Single `RecentEntityRepository` with a sealed type** — rejected: would lose
  the typed per-feature `scoreOf` signal used to rank results in query mode,
  and breaks the "one repository per feature" pattern.
- **Active cleanup of orphan settings history entries** — rejected as
  destructive: a temporary disappearance (OS rollback, ROM swap) would wipe
  usage signal that could otherwise be reused.
