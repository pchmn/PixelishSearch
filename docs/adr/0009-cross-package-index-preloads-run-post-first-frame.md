# Index preloads that resolve other packages' resources run post-first-frame, not from `Application.onCreate`

The cold-start convention (AGENTS.md) is that every expensive one-shot startup
task is dispatched async from `PixelishSearchApp.onCreate`, where the callers
"line up symmetrically". Each of the three feature indexes is now **split in two
phases**: a cheap phase A (hydrate from a disk cache, no PackageManager) stays
eager in `Application.onCreate`, while the expensive phase B (`AppIndex.refresh`,
`SettingsPageIndex.refresh`, `ShortcutIndex.refresh` — the part that resolves
*other* packages' resources) runs from `MainActivity`, kicked off only once the
first content frame is drawn. This records why.

- **The problem: cross-package `getResources` contends with the first frame.** A
  `benchmarkRelease` startup trace (Pixel 9, baseline profile active —
  `docs/performance-analysis.md`) flagged `ResourcesManager#getResources` as the
  single `slow_start_reason`. Three index paths resolve *other* packages' labels
  / resources via `PackageManager`: `SettingsPageIndex` loads the Settings
  `.arsc` + its whole RRO overlay cascade ≈ 27 ms; `ShortcutIndex` calls
  `getResourcesForApplication` on every launcher app declaring
  `android.app.shortcuts`; and **`AppIndex` phase B** calls `loadLabel` on every
  launcher app, which loads each app's resources (Chrome, Settings, Play, …).
  Dispatched from `Application.onCreate`, that work ran on the
  `Dispatchers.IO`/`Default` pool **concurrently with the first-frame
  composition**, stealing CPU and contending on ART locks (`InternTable`, thread
  list) right up to the first frame.

- **The *search* path doesn't need it at the first frame; the *recents* path
  does.** `SettingsPageIndex.search` / `ShortcutIndex.search` return `emptyList()`
  below a 2-char query and `AppIndex.search` is empty on a blank query — and the
  user can't type 2 chars before the first frame (>300 ms vs ≈161–197 ms). So for
  *search*, deferring the re-scan is pure contention avoidance, free. **But the
  blank-state Recents block renders on the first frame and consumes the indexes
  directly:** `SearchViewModel` filters recent shortcuts / settings pages to those
  whose key is still present in `ShortcutIndex.entries` / `SettingsPageIndex.entries`
  (stale-filtering, ADR-0008/0002). A *naively* deferred index is empty at the
  first frame, so recently-used shortcuts/settings **vanish until the deferred
  re-parse finishes** — a visible "pop-in" latency (observed in testing). The fix
  is the phase split: phase A hydrates `_entries` from a **disk cache** before the
  first frame (no `getResources`), so recents render immediately; phase B's
  authoritative re-scan only refreshes the cache afterwards. `AppIndex` already
  had this cache; `ShortcutIndex` + `SettingsPageIndex` gained one
  (`*IndexCacheRepository`, DataStore JSON) — this is **Option 2** below, no
  longer optional.

- **The mechanism: a `ViewTreeObserver.OnDrawListener` on the first *content*
  draw, then `post`.** The first attempt — a double `Choreographer.postFrameCallback`
  — was measured to fire too early. The transparent window emits an empty frame
  first (vsync 1 ≈ 114 ms, no `Record View#draw()`); the actual Compose content
  composes + draws on vsync 2 (≈ 155–197 ms). Two nested frame callbacks land the
  inner one at the *start* of vsync 2, i.e. **inside** the heavy composition
  frame, so the deferred work still raced the first frame (a re-measured trace
  still showed the full `getResources` cascade at t ≈ 142–197 ms, TTID
  unimproved). A vsync count is off-by-one and fragile to how many empty frames
  precede content. Instead we add an `OnDrawListener` and act on its first
  `onDraw()` — the frame that *actually draws* the content — and from there
  `decorView.post {}` so the work runs *after* that draw completes, not at its
  start (the listener is removed in the same post; it can't be removed mid-draw).
  Ownership stays on `appScope` (`PixelishSearchApp.backgroundScope`), so the
  work outlives the Activity — the indexes are `object`s, we just pass the scope.
  This mirrors `UpdateChecker`, the pre-existing precedent for "runs from
  `MainActivity`, not `Application.onCreate`".

- **Recreation is gated at the call site, not per-index.** `MainActivity.onCreate`
  can re-run on activity recreation, where the singleton indexes are already
  populated and a second phase-B re-scan would be wasted. Rather than a
  `@Volatile loaded` flag inside each index (the previous approach), `MainActivity`
  simply gates the whole post-first-frame trigger on `savedInstanceState == null`.
  This covers all three `refresh`es uniformly; the `refresh` functions themselves
  stay flag-free so `PackageReceiver` / `BootReceiver` can always force a re-scan.

- **`ShortcutIndex` reuses `AppIndex`'s labels.** `AppIndex` already resolves
  each launcher activity's label (same `loadLabel` source). `ShortcutIndex`
  reads those from `AppIndex.apps.value` and only falls back to `loadLabel` for
  packages absent from the snapshot (e.g. `AppIndex` not yet hydrated), dropping
  one per-app resource-loading vector. `getResourcesForApplication` itself
  remains — the target app's `.arsc` is still required to resolve shortcut
  labels/icons — but it no longer races the first frame.

This combines **Option 1** (defer past the first frame, all three `getResources`
vectors) and **Option 2** (disk cache so cold start hydrates without
`getResources`) of `docs/performance-analysis.md`. The `localeFilters` (Option 3)
follow-up was deliberately *not* taken yet.

## Revisit when

- The first-ever launch (empty caches) still shows no app/shortcut/settings
  results or recents until the deferred phase B finishes (a few hundred ms after
  the first frame, still ahead of the user typing 2 chars). If that one-time
  cold path proves perceptible, run phase B eagerly *only* when the cache is
  empty, keeping the deferral for warm starts.
- A measured cold-start regression turns out to be `BootReceiver` /
  `PackageReceiver` `refresh` firing during the first frame. Those run phase B
  eagerly (no first frame to protect) — correct in normal use, but `StartupMode.COLD`
  re-delivers `BOOT_COMPLETED` to the process *every iteration*, so `BootReceiver`
  would re-scan during startup and inflate TTID uniformly (a real tap-to-search
  never does this). `BootReceiver` therefore early-returns when the applicationId
  ends with `.benchmark` (see `docs/performance-analysis.md`).
