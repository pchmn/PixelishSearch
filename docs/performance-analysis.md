# Cold-start analysis — `getResources` bottleneck

> **Status:** investigation done, fix **not yet implemented**. Pick an option below and
> implement, then re-measure.
> **Source run:** `benchmarkRelease` on Pixel 9 (tokay), 2026-06-02 22:12–22:15, freshly
> regenerated baseline profile (commit `3ddc72f`). Median iteration analysed:
> `StartupBenchmark_startupBaselineProfile_iter005`.
> Companion doc: [`PERFORMANCE.md`](PERFORMANCE.md) (the how-to). This file is the *findings*.

## TL;DR

- The new baseline profile works: **cold start `None → BaselineProfile` = 240.7 ms → 171.5 ms
  (‑29 %)**, with variance collapsing (CoV 0.15 → 0.07).
- The single bottleneck flagged by Perfetto's `android_startup` metric is
  **`ResourcesManager#getResources`** — and it is **our code's fault**, not the framework's.
- Three index preloads (`ShortcutIndex`, `SettingsPageIndex`, `AppIndex` phase B) call
  `PackageManager` to resolve **other packages'** labels/resources, dispatched from
  `Application.onCreate`. They run **concurrently with the first-frame composition**, stealing CPU
  and contending on ART locks. None of that data is needed for the first frame.
- **Fix = don't run those enumerations during the first frame.** Trigger them *after* the first
  frame (from `MainActivity`), not from `Application.onCreate`. Estimated **~10–25 ms** off TTID +
  lower variance — **must be measured**, not guaranteed.

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

### Option 1 — Defer past the first frame (recommended)

Move `ShortcutIndex.preload` + `SettingsPageIndex.preload` **out of `Application.onCreate`** and
trigger them from `MainActivity` once the first frame is drawn. Also reuse `AppIndex`'s
already-resolved labels inside `ShortcutIndex` instead of calling `loadLabel` again (drops one
resource-loading vector). Optionally defer `AppIndex` phase-B refresh too (phase-A cache already
feeds the UI — see caveat below).

- **Targets exactly the contention** shown in the trace. Minimal diff. Low risk.
- Readiness is safe: first frame ≈ 161 ms; typing 2 chars takes >300 ms, so kicking the preloads
  off right after the first frame leaves ample lead time.
- **Deferral mechanism** (pick one): `MainActivity.reportFullyDrawn()`, or
  `window.decorView.post { }` after `setContent`, or `Choreographer.postFrameCallback`, or a
  `LifecycleEventObserver` firing on `ON_RESUMED`. Keep ownership on `appScope`
  (`PixelishSearchApp.backgroundScope`) — the indexes are `object`s, just pass the scope.
- **Caveat (`AppIndex` phase B):** phase A (persisted cache) hydrates the app list cheaply and *is*
  effectively needed the moment the user types; phase B only *refreshes* via `loadLabel`. Deferring
  phase B is safe-ish but more delicate than the other two — treat as a separate, measured step.
- **Convention note:** AGENTS.md documents preloads/warmups lining up symmetrically in
  `Application.onCreate`. Moving two of them to a post-first-frame trigger departs from that — update
  AGENTS.md / PERFORMANCE.md (and consider an ADR) when implementing.

### Option 2 — Defer + disk cache

Option 1 **plus** persist the resolved entries of `ShortcutIndex` and `SettingsPageIndex` to disk,
mirroring `AppIndex` phase A (`AppIndexCacheRepository`). Cold start then hydrates from cache
instantly and performs **zero `getResourcesForApplication`** on the cold path; the (deferred)
re-enumeration only runs in the background and rewrites the cache when it changes.

- Biggest win on warm cold-starts; also cuts total CPU/I/O every launch (not just moves it).
- More code: serialize `Intent` (`Intent.toUri` / `parseUri`), `ComponentName`
  (`flattenToString`), `iconResId` (int), `lastUpdateTime` (gates invalidation). `SettingsPageEntry`
  (`label` + `ComponentName`) is trivial; `ShortcutEntry` (built `Intent`) is the work.
- Invalidation already has a hook: `PackageReceiver` triggers `refresh` on install/remove/update.
- These indexes are currently documented as "no disk cache" — that decision would change; update
  their KDoc + ADR-0008 (shortcuts).

### Option 3 — Defer + disk cache + `localeFilters`

Option 2 **plus** trim the app's own locales via
`android { androidResources { localeFilters += listOf("en","fr","es","de","it") } }` (AGP 8.x;
older `resConfigs`). AndroidX deps pull in dozens of locales (`values-ur`, `-ta`, `-ky`, …) that
bloat **our** `resources.arsc`.

- Most thorough, but **smallest marginal gain** here: the dominant `LoadApkAssets` in the trace are
  *other apps'* `.arsc` (Settings, Chrome…), not ours. This shrinks our own resource parse + APK
  size — worth doing, but orthogonal to the headline bottleneck.

## How to re-measure after a fix

```bash
./gradlew :app:installBenchmarkRelease     # install benchmark variant
# use the app a bit so AppIndex / Coil / DataStore caches populate (warm cold-start)
./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest
```

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

- How much does Option 1 alone actually move TTID? (Measure: defer Shortcut + Settings, re-run.)
- Does deferring `AppIndex` phase B regress "app results ready on first keystroke"? (Phase A cache
  should cover it; verify on a cold device.)
- Is the second frame (164→221 ms, IME/insets) worth attacking next, or is TTID the only headline?
