# Cold-start analysis — `getResources` bottleneck

> **Status:** investigation done. **Option 1 implemented, then corrected after re-measurement**
> (branch `chore/improve-perf`; see ADR-0009).
> - *First attempt* deferred `SettingsPageIndex` + `ShortcutIndex` preloads from `MainActivity` via
>   a **double `Choreographer.postFrameCallback`**. Re-measured 2026-06-04 (Pixel 9) → **no gain**:
>   `startupBaselineProfile` median 193 ms ≈ `startupNone` 195 ms, and the trace still showed the
>   full `getResources` cascade at t ≈ 142–197 ms. Diagnosis: the transparent window draws an empty
>   frame first (vsync 1 ≈ 114 ms), so the *content* draws on vsync 2 (≈ 155–197 ms) and the inner
>   frame callback fired at the **start** of that very frame — inside the composition, not past it.
>   Plus `AppIndex` phase B (`loadLabel` per app) was never deferred and carried its own share.
> - *Correction* — trigger off the first real `ViewTreeObserver.OnDrawListener` (the content draw)
>   then `decorView.post` so the work lands *after* the frame; defer `AppIndex.refresh` (phase B) too.
> - *Second re-measurement (2026-06-04, 10:40 — `Pixel 9 - 16`) exposed two things, both now fixed:*
>   1. **Benchmark contamination (systematic, not a one-off reboot).** Every iteration of every
>      variant delivers `BOOT_COMPLETED` to the freshly-started process — `StartupMode.COLD`
>      force-stops + relaunches, and the system re-delivers it each time → `BootReceiver.refresh`
>      runs phase-B `enumerate` at t≈100–120 ms, *in* the first frame. A real tap-to-search cold
>      start never does this (BOOT_COMPLETED fires once at boot, in the background). It inflated
>      **all three** variants — not a code regression. **Fix: `BootReceiver` self-skips on the
>      `.benchmark` applicationId** (a build-time `pm disable-user` / source-set manifest override
>      doesn't survive the baselineprofile plugin's build types), so its phase-B refresh no longer
>      runs during the measured launch. (The deferred `MainActivity` refresh itself correctly lands
>      *after* TTID.)
>   2. **A real recents regression.** The blank-state recents block *does* consume the indexes at
>      the first frame (it stale-filters recent shortcuts/settings against the live index). Deferring
>      `ShortcutIndex` / `SettingsPageIndex`, which had **no disk cache**, made recently-used
>      shortcuts/settings pop in late. **Fix = Option 2 (disk cache):** both indexes now persist
>      their entries (`*IndexCacheRepository`) and hydrate (phase A) before the first frame; the
>      deferred phase B only refreshes. See ADR-0009 / ADR-0008.
> - *Clean final measurement (2026-06-04, 14:35 — `Pixel 9 - 16`, BootReceiver self-skip + font
>   warmup):* contamination gone (BOOT_COMPLETED still delivered but no `phaseB.enumerate` before
>   TTID), `compilation_filter: speed-profile` confirmed, `time_get_resources` **376 → 111 ms**.
>   `Baseline` median **176.6** (min 165.6) ≈ `None` **178.5** (min 174.0); `Full` 188.2. **The
>   profile and no-AOT now tie** — expected: the cold path is light enough (cache, no enumerate
>   contention, font pre-warmed) that AOT has almost nothing left to remove. The headline win is the
>   no-AOT floor dropping **240.7 → 178.5 ms** and `getResources` falling 70 %, *not* an AOT delta.
>   The profile still doesn't hurt (slightly better median/min) and earns its keep on slower devices.
> - Option 3 (`localeFilters`) deliberately not taken — orthogonal to the (now-resolved) bottleneck.
>
> **Source run (the trace analysis further down):** `benchmarkRelease` on Pixel 9 (tokay), 2026-06-02
> 22:12–22:15, baseline profile commit `3ddc72f`. Median iteration analysed:
> `StartupBenchmark_startupBaselineProfile_iter005`. (Numbers in that section are the *original*
> pre-fix run — kept as the diagnosis of record; the clean final numbers are the bullet above.)
> Companion doc: [`PERFORMANCE.md`](PERFORMANCE.md) (the how-to). This file is the *findings*.

## TL;DR

- **Outcome (implemented + measured clean):** the `getResources` contention was moved off the first
  frame (defer phase B + disk-cache all three indexes) and the device-font lookup pre-warmed. Result:
  `time_get_resources` **376 → 111 ms**, the no-AOT cold-start floor **240.7 → 178.5 ms**, and
  `Baseline ≈ None` (~177 ms) — the path is now light enough that AOT barely matters. It also fixed a
  real UX bug (recent shortcuts/settings popping in late) and made the macrobenchmark trustworthy
  (`BootReceiver` self-skips on `.benchmark`). See the status block above for the full saga.
- The single bottleneck flagged by Perfetto's `android_startup` metric was
  **`ResourcesManager#getResources`** — and it was **our code's fault**, not the framework's.
- Three index preloads (`ShortcutIndex`, `SettingsPageIndex`, `AppIndex` phase B) called
  `PackageManager` to resolve **other packages'** labels/resources, dispatched from
  `Application.onCreate`. They ran **concurrently with the first-frame composition**, stealing CPU
  and contending on ART locks. None of that data is needed for the first frame.
- **Fix = don't run those enumerations during the first frame.** Trigger them *after* the first
  frame (from `MainActivity`), not from `Application.onCreate` — plus a disk cache so the first frame
  (incl. blank-state recents) hydrates without any `getResources`.

## Benchmark results (this run)

`./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest`, 3 modes × 10 iterations,
`timeToInitialDisplayMs`:

| Variant | median | min / max | CoV |
|---|---|---|---|
| `startupNone` (no AOT, pessimistic) | **240.7 ms** | 167 / 261 | 0.15 |
| `startupBaselineProfile` (our profile, ships) | **171.5 ms** | 156 / 195 | 0.07 |
| `startupFull` (full AOT, optimistic ceiling) | 242.8 ms | 224 / 279 | 0.06 |

Notes:
- The user-facing win is **None → BaselineProfile = ‑69 ms / ‑29 %**.
- `Full` (243 ms) being *slower* than `BaselineProfile` (171 ms) is expected and a good sign: full
  AOT compiles the whole app → larger code → more page-in cost at startup, whereas the baseline
  profile is surgical (startup path only). It also ran first (coldest device) and one `None`
  iteration stalled ~36 s (38 MB trace), inflating its variance.
- `android_startup` confirms the profile is active: `compilation_filter: speed-profile`,
  `odex_status: up-to-date`.

## Cold-start timeline (iter005, t=0 = `intent_received`, first frame ≈ 161 ms)

`android_startup` reports `time_to_initial_display: 161.7 ms` and `to_first_frame: 227.1 ms`
(the macrobenchmark median for this iteration was 171.5 ms — slightly different anchor).

| Window | Phase | Owner | Cost |
|---|---|---|---|
| 0 → 58 ms | zygote fork + ActivityManager (`to_bind_application`) | OS / framework | ~58 ms — not controllable |
| 58 → 92 ms | `bindApplication` scaffolding (dex open 15 ms, class init, providers) | ART / framework | ~34 ms |
| **92.5 → 95.5 ms** | **`PixelishSearchApp.onCreate`**: `repos.construct` 1.2 ms + all `preload`/`warmUp` dispatched async (<1 ms each) | **our code** | **3.1 ms** ✅ |
| 99 → 105 ms | `activityStart` | framework | 10 ms |
| 105 → 114 ms | `MainActivity` onCreate/start/resume — `enableEdgeToEdge` 3.2 ms, `setContent` 0.2 ms | our code | ~9 ms |
| 134 → 161 ms | **first `Choreographer#doFrame`** = `SearchScreen` composition | Compose | ~29 ms |
| **≈ 161 ms** | **first frame displayed → TTID** (`reportDrawFinished`) | | |
| 164 → 221 ms | second frame (IME/insets, full display) | Compose | ~58 ms |

Our own startup code is **negligible** (3.1 ms + 4.1 ms). The eager-async preload architecture
(AGENTS.md) is validated: `warmUp`/`preload` cost only their dispatch, `AppIndex.phaseA.hydrate` /
`phaseB.enumerate` run on background threads.

Main thread is **I/O- and contention-bound, not CPU-bound**. Over the 227 ms `to_first_frame`
window the main thread is only *running* 73.6 ms; the rest is waiting:
`interruptible_sleep 85.6 ms`, `uninterruptible_sleep 26.2 ms` (of which 25.7 ms I/O),
`runnable 11 ms`. The baseline profile's job is to keep the *running* slice small (AOT) — it does.

## Root cause: cross-package `getResources` during the first frame

`android_startup` `slow_start_reason: "Time spent in ResourcesManager#getResources"`
(`time_get_resources` = 376 ms cumulative across 3 threads, expected ≤130 ms → WARNING).

The 3 `getResources` slices run on threads `tid 27446 / 27449 / 27470`, all named
**`DefaultDispatch`** (kotlinx `Dispatchers.IO`/`Default` share one elastic pool), spanning
**t≈97 → 161.5 ms** — i.e. they finish *exactly* at the first frame. The trace shows them loading
**other packages'** resource tables, e.g.:

```
27446  133.2   27.67  ResourcesManager#getResources
27446  133.3   17.20    LoadApkAssets(/system_ext/priv-app/SettingsGoogle/Settings…)   ← Settings .arsc
27446  150.5    0.70    LoadApkAssets(/data/resource-cache/…@overlay@Settings…)        ← RRO overlays
27446  151.2    1.06    LoadApkAssets(/data/resource-cache/…@overlay@Glanceable…)
27446  153.0    1.45    LoadApkAssets(/data/resource-cache/…@overlay@Wildlife…)        … (cascade)
```

…plus visible **lock contention** between the workers during this window:
`monitor contention with owner DefaultDispatcher-worker-20/-7`, `Lock contention on InternTable
lock`, `runtime shutdown lock`, `thread list lock`. They fight each other *and* steal CPU/locks
from the main thread composing the first frame.

### The three contributors (our code)

| Index | Call | File:line | What it loads |
|---|---|---|---|
| **`ShortcutIndex`** | `getResourcesForApplication(pkg)` + `info.loadLabel(pm)` | `search/shortcuts/data/ShortcutIndex.kt:128`, `:134` | the **full `.arsc` of every launcher app that declares `android.app.shortcuts`** (Chrome, Gmail, Maps, Phone…) |
| **`SettingsPageIndex`** | `getApplicationLabel(getApplicationInfo(SETTINGS_PKG))` + `info.loadLabel(pm)` per sub-page | `search/settings/data/SettingsPageIndex.kt:93`, `:104` | the **Settings `.arsc` + its whole RRO overlay cascade** = the 27 ms `LoadApkAssets(SettingsGoogle)` above |
| **`AppIndex`** (phase B) | `ri.loadLabel(pm)` per installed app | `search/apps/data/AppIndex.kt:139` | each installed app's resources, for its label |

All three are dispatched from `PixelishSearchApp.onCreate`
(`PixelishSearchApp.kt:107` and `:114`; `AppIndex.preload` at `:77`).

### Why it hurts TTID even though it's "off the first-frame path"

The data is genuinely **not needed** for the first frame:
- `ShortcutIndex.search` / `SettingsPageIndex.search` return `emptyList()` below `MIN_QUERY_LENGTH = 2`.
- `AppIndex.search` returns `emptyList()` on a blank query.

So this is **not a data dependency** — it's pure resource contention happening in the worst
possible window. The user physically cannot type 2 characters before 161 ms, so the work has no
reason to run *before* the first frame.

## Improvement options

### Option 1 — Defer past the first frame (recommended) — ✅ implemented (ADR-0009)

Move all three `getResources` vectors **out of `Application.onCreate`** and trigger them from
`MainActivity` once the first *content* frame is drawn: `ShortcutIndex.preload`,
`SettingsPageIndex.preload`, **and `AppIndex.refresh` (phase B)**. `AppIndex` keeps its cheap phase A
(cache hydrate, no `loadLabel`) eager from `Application.onCreate`, so the typed UI still has the app
list immediately. `ShortcutIndex` also reuses `AppIndex`'s already-resolved labels instead of calling
`loadLabel` again (drops one resource-loading vector).

- **Targets exactly the contention** shown in the trace. Minimal diff. Low risk.
- Readiness is safe: first frame ≈ 161–197 ms; typing 2 chars takes >300 ms, so kicking the work off
  right after the first frame leaves ample lead time.
- **Deferral mechanism — use the first *content draw*, not a vsync count.** The original double
  `Choreographer.postFrameCallback` was measured to fire too early: the transparent window draws an
  empty frame first (vsync 1), so the inner callback lands at the *start* of the content frame
  (vsync 2), inside the composition. The robust signal is `ViewTreeObserver.OnDrawListener` — act on
  its first `onDraw()` (the real content draw) and from there `decorView.post {}` so the work runs
  *after* the frame completes. (`reportFullyDrawn()` / a `LifecycleEventObserver` on `ON_RESUMED` are
  even earlier and were rejected.) Keep ownership on `appScope` (`PixelishSearchApp.backgroundScope`)
  — the indexes are `object`s, just pass the scope.
- **Caveat (`AppIndex` phase B), now accepted:** phase A (persisted cache) hydrates the app list
  cheaply and feeds the typed UI; phase B only *refreshes* via `loadLabel`. Deferring phase B means
  the **first-ever launch** (empty cache) has no app results until phase B finishes (a few hundred ms
  after the first frame, still ahead of the user typing 2 chars). If that ever proves perceptible,
  run phase B eagerly *only* on an empty cache.
- **Convention note:** AGENTS.md documents preloads/warmups lining up symmetrically in
  `Application.onCreate`. Moving these to a post-first-frame trigger departs from that — AGENTS.md,
  this doc, and ADR-0009 are updated to match.

### Option 2 — Defer + disk cache — ✅ implemented (ADR-0009/0008)

Option 1 **plus** persist the resolved entries of `ShortcutIndex` and `SettingsPageIndex` to disk,
mirroring `AppIndex` phase A (`AppIndexCacheRepository`). Cold start then hydrates from cache
instantly and performs **zero `getResourcesForApplication`** on the cold path; the (deferred)
re-enumeration only runs in the background and rewrites the cache when it changes.

This turned out to be **not optional**: deferring these two indexes without a cache regressed the
blank-state recents (recent shortcuts/settings stale-filter against the live index, which was empty
until the deferred re-parse — visible pop-in). So the cache is what *lets* the deferral keep recents
intact.

- Biggest win on warm cold-starts; also cuts total CPU/I/O every launch (not just moves it).
- Implemented as `ShortcutIndexCacheRepository` + `SettingsPageIndexCacheRepository` (DataStore JSON,
  same shape as `AppIndexCacheRepository`): `ShortcutEntry` serializes its built `Intent` via
  `Intent.toUri` / `parseUri` (+ `iconResId`, `lastUpdateTime`); `SettingsPageEntry` flattens its
  `ComponentName`. Coil still resolves icons on demand from `iconResId` / `packageName`.
- Invalidation hook: `PackageReceiver` triggers `ShortcutIndex.refresh`; every cold start's deferred
  `refresh` also self-heals the caches.
- Reversed the "no disk cache" note in `ShortcutIndex`'s KDoc + ADR-0008.

### Option 3 — Defer + disk cache + `localeFilters`

Option 2 **plus** trim the app's own locales via
`android { androidResources { localeFilters += listOf("en","fr","es","de","it") } }` (AGP 8.x;
older `resConfigs`). AndroidX deps pull in dozens of locales (`values-ur`, `-ta`, `-ky`, …) that
bloat **our** `resources.arsc`.

- Most thorough, but **smallest marginal gain** here: the dominant `LoadApkAssets` in the trace are
  *other apps'* `.arsc` (Settings, Chrome…), not ours. This shrinks our own resource parse + APK
  size — worth doing, but orthogonal to the headline bottleneck.

## How to re-measure after a fix

First decide whether your fix touched the **cold-start path** (anything running before/during the
first frame: `PixelishSearchApp.onCreate`, `MainActivity`, `SearchScreen` composition, a Compose/M3
bump). That decides whether the baseline profile must be regenerated.

**If the cold-start path changed** — regenerate so `startupBaselineProfile` measures what ships:

```bash
./gradlew :app:generateBaselineProfile     # installs nonMinifiedRelease, rewrites
                                           # app/src/release/generated/baselineProfiles/*.txt
```

**Then measure (always):**

```bash
./gradlew :app:installBenchmarkRelease     # installs benchmarkRelease
# use the app a bit so AppIndex / Coil / DataStore caches populate (warm cold-start)
./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest
```

Three gotchas:

- **`BootReceiver` self-skips on `.benchmark` — that's load-bearing for the numbers.**
  `StartupMode.COLD` re-delivers `BOOT_COMPLETED` to the process every iteration, which would fire
  `BootReceiver.refresh` → phase-B `enumerate` *during* the first frame (a real tap-to-search never
  does this). It inflates TTID uniformly across all variants. `BootReceiver` therefore early-returns
  when `packageName.endsWith(".benchmark")`. (A `setupBlock` `pm disable-user` and a
  `src/benchmarkRelease/` manifest override were both tried first and don't work — the package isn't
  always installed when the shell runs, and the baselineprofile plugin's build types don't expose a
  mergeable manifest source set.) If you measure another way, replicate the skip, and check a trace
  for `broadcastReceiveComp: …BOOT_COMPLETED` on the app process if numbers look uniformly slow. Also
  prefer a settled, cool device — background load still adds noise.
- `connectedBenchmarkReleaseAndroidTest` **consumes** the committed profile, it does not generate
  one. `StartupBenchmark` uses `CompilationMode.Partial()` (= `UseIfAvailable`): with no profile
  packaged, `startupBaselineProfile` silently falls back to no-AOT and reads identical to
  `startupNone` — no error, just no gain.
- `generateBaselineProfile` (`nonMinifiedRelease`) and `connectedBenchmarkReleaseAndroidTest`
  (`benchmarkRelease`) share the `.benchmark` applicationId but differ in build type. Running
  measure after generate triggers a variant-switch reinstall that **resets the benchmark package's
  caches** — so warm the app *after* `installBenchmarkRelease`, not before.

Compare `startupNone` vs `startupBaselineProfile` medians in
`benchmark/build/outputs/connected_android_test_additional_output/benchmarkRelease/connected/<Device>/`
(the `.txt` summaries + `…-benchmarkData.json`). **The JSON is overwritten each run** — archive the
median if you want a true before/after.

## How to reproduce this trace analysis

No Perfetto tooling is committed; fetch the official helpers (same pattern as `perfetto-trace.sh`).

```bash
# 1. Open a trace in the Perfetto UI (served from localhost; processed client-side, no upload)
curl -fsSL -o /tmp/open_trace_in_ui https://raw.githubusercontent.com/google/perfetto/master/tools/open_trace_in_ui
chmod +x /tmp/open_trace_in_ui
python3 /tmp/open_trace_in_ui -i "<path>/StartupBenchmark_startupBaselineProfile_iter005_*.perfetto-trace"

# 2. Query a trace with SQL (downloads the trace_processor binary on first run)
curl -fsSL -o /tmp/trace_processor https://get.perfetto.dev/trace_processor
chmod +x /tmp/trace_processor

# The startup metric (TTID, slow_start_reason, task-state breakdown, compilation_filter):
python3 /tmp/trace_processor "<trace>" --run-metrics android_startup

# The call tree on the getResources worker threads (replace upid/tids/anchor ts from the metric):
python3 /tmp/trace_processor "<trace>" -q query.sql
```

Useful `query.sql` (identify process/threads, then dump the call tree on the worker threads):

```sql
-- benchmark process + its threads
SELECT p.upid, p.pid, t.utid, t.tid, t.name, t.is_main_thread
FROM thread t JOIN process p ON t.upid = p.upid
WHERE p.name = 'com.pchmn.pixelishsearch.benchmark';

-- call tree on the getResources threads during the first-frame window
-- (anchor = event_timestamps.intent_received from --run-metrics android_startup)
SELECT t.tid,
  printf('%6.1f', (s.ts - <intent_received_ts>)/1e6) AS t_ms,
  printf('%6.2f', s.dur/1e6) AS dur_ms, s.depth AS d, s.name AS slice
FROM slice s
JOIN thread_track tt ON s.track_id = tt.id
JOIN thread t ON tt.utid = t.utid
WHERE t.upid = <upid> AND t.tid IN (<getResources tids>)
  AND s.ts BETWEEN <intent_received_ts> + 90000000 AND <intent_received_ts> + 165000000
ORDER BY t.tid, s.ts;
```

## Open questions for next session

- How much does the **corrected** Option 1 move TTID? The first attempt (double
  `postFrameCallback`) showed no gain because it fired inside the content frame; the `OnDrawListener`
  version + phase-B deferral now need a clean re-measurement. **Control conditions:** warm the app
  after `installBenchmarkRelease`, discard `iter000`, and compare min / trimmed mean — the 2026-06-04
  run was too noisy (`None` 240→195, `Full` 242→183 both shifted, an impossible "code" change) to
  trust a single median.
- Confirm on a re-measured trace that the `getResources` cascade now lands **after** TTID (it should
  start at the first `onDraw`, ≈ TTID, not at t≈142 ms).
- Does deferring `AppIndex` phase B regress "app results ready on first keystroke" on a **fresh
  install** (empty cache)? Phase A covers warm starts; verify the first-ever launch on a cold device.
- **IME readiness.** Trace finding: the soft keyboard isn't *requested* until ~100 ms after TTID
  because `MSG_WINDOW_FOCUS_CHANGED` is queued on the main thread behind a heavy ~71 ms post-TTID
  composition frame (text layout / `StaticLayout` ≈ 10 ms, `applyChanges` 8, `measure` 6.5, `draw`
  8.6). The IME config is already correct (`.onPlaced { requestFocus() }` + `stateAlwaysVisible`);
  the lever is lightening that frame. **Experiment in place:** `warmUpGoogleSans` (Theme.kt) pre-
  resolves the device font from `Application.onCreate` to pull the `DeviceFontFamilyName` lookup out
  of that frame. *To verify:* compare the post-TTID `doFrame` duration and the
  `IMMS.startInputOrWindowGainedFocus` timestamp with/without the warmup (trace, all processes).
  Expectation is modest — much of the gap is the framework focus handshake + Gboard's own draw
  (pessimistic in-benchmark since Gboard is cold; warm in real use).
- Is the rest of the second frame (IME/insets) worth attacking, or is the font warmup enough?
