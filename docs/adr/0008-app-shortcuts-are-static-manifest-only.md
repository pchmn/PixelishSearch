# App shortcuts are read from the manifest, static-only, without the launcher role

App shortcut search mirrors the Contacts feature (a `shortcut_search_enabled`
Preference, a per-keystroke search, a `ShortcutHistoryRepository`, participation
in the fused blank-state Recents strip). What is distinctive — and worth
recording so a future reader doesn't read it as an oversight — is the *source*
and its limits.

- **Manifest parsing, not `LauncherApps.getShortcuts`.** The supported API
  returns all three shortcut kinds (static, dynamic, pinned) but requires
  `hasShortcutHostPermission()` — i.e. being the default launcher. PixelishSearch
  is a transparent search surface, not a launcher, and we want this category **on
  by default with no permission prompt**. So we read only the *static* shortcuts
  every app declares in an `android.app.shortcuts` XML referenced by a launcher
  activity: `queryIntentActivities(ACTION_MAIN + CATEGORY_LAUNCHER, GET_META_DATA)`
  → `activityInfo.loadXmlMetaData(...)` → parse, resolving labels/icons against the
  *target* app's resources. This needs no role and no manifest change (the
  `ACTION_MAIN/CATEGORY_LAUNCHER` query is already declared). **Dynamic and pinned
  shortcuts are out of scope** — there is no way to read them without the launcher
  role.

- **We fire the parsed Intent ourselves, so we filter to launchable.** Not being
  the shortcut host, we also can't use `LauncherApps.startShortcut`; we launch the
  `<intent>` (the last one, for back-stack shortcuts) directly. The system launches
  manifest shortcuts with elevated privilege, so a parsed shortcut whose target
  activity is non-exported would throw on our `startActivity`. We therefore
  **filter at index time to only launchable shortcuts** (resolvable exported
  component, or a resolvable action) and wrap the launch in a try/catch. Typed
  `<extra>`s in the `<intent>` are not reconstructed (rare, and complex to
  type-decode) — a known v1 limitation.

- **Stateful, like a Contact entry — not stateless like a Calendar event.** An App
  shortcut is replayable (manifest-declared, the tap always does the same thing),
  so unlike a Calendar event (ADR-0007) it gets a `ShortcutHistoryRepository`,
  frequency re-ranks typed results (ADR-0006), and recently-used shortcuts join the
  fused blank-state Recents strip (ADR-0002, now a third source) — matching the
  native Pixel Launcher, which surfaces e.g. "Downloads · Files" there. The history
  entry is self-sufficient for display (key `(packageName, shortcutId)` +
  `shortLabel` + `iconResId`); the launch Intent is re-resolved from `ShortcutIndex`
  at tap time, and entries whose key is absent from the live index are filtered at
  display (stale-filtering, like Settings pages).

- **The index is disk-cached so recents survive the cold-start deferral.** Because
  recent shortcuts only render once their key is present in the live `ShortcutIndex`
  (the stale-filter above), and the expensive `shortcuts.xml` re-parse is deferred
  past the first frame for startup (ADR-0009), `ShortcutIndex` persists its resolved
  entries to a `ShortcutIndexCacheRepository` (DataStore JSON, the built launch
  Intent stored via `Intent.toUri`). Cold start hydrates the index from that cache
  before the first frame, so previously-seen recent shortcuts show immediately, and
  the deferred re-parse only refreshes it. (Originally documented as "no disk cache"
  on the assumption shortcuts never render at first frame; that overlooked the
  blank-state Recents strip - see ADR-0009.)

## Revisit when

- Users ask why a known dynamic or pinned shortcut (a conversation, a recent
  document) doesn't appear — at which point becoming an optional shortcut host (a
  launcher-role opt-in) would be the only way to source them.
