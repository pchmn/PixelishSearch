# Performance

Cold start is the headline metric. On a Pixel 9, release build with the baseline profile installed:

- `TotalTime` (first frame): **~120-135ms** measured via
  `adb shell am start -W -n com.pchmn.pixelishsearch/.search.MainActivity`
- Second Compose frame (TextField focus animation): ~30ms
- Fully stable screen: **~150-170ms**

## The cold start path

Five layers contribute, in order of execution:

1. **`PixelishSearchApp.onCreate`** preloads everything async at process creation:
    - `AppIndex.preload` (2-phase: hydrate from persisted cache → re-enumerate)
    - `WebSuggestionsRepository.warmUp` warms the TLS handshake to Google Suggest
    - `ContactRepository.warmUp` wakes the (out-of-process) Contacts provider
    - History repos start collecting DataStore eagerly via hot `StateFlow`s
2. **`AppIndex` persisted cache** + **Coil disk cache for icons** — the launcher list and its icons
   hydrate from disk in single-digit ms, no PackageManager call on the hot path.
3. **`BootReceiver`** retriggers the preload on `BOOT_COMPLETED` to warm DataStore page caches.
4. **`PackageReceiver`** refreshes the index on install / uninstall / update so changes appear at
   the next tap without restarting the process.
5. **`MainActivity`** uses `Theme.PixelishSearch.Transparent` + `FLAG_BLUR_BEHIND` so the search
   sheet renders directly over the launcher, no app transition.

## First-frame tuning

Three settings work together to keep the **2nd Compose frame** from triggering a costly
recomposition after the activity is drawn:

- **`AndroidManifest.xml`** declares `android:windowSoftInputMode="stateAlwaysVisible|adjustPan"`.
  `adjustPan` (instead of `adjustResize`) means the IME slide-in does **not** propagate through
  window insets to Compose. The `SearchField.LaunchedEffect { focusRequester.requestFocus() }` can
  therefore fire after the first frame without dragging in a 50+ ms `Compose:recompose` pass on the
  IME animation.
- **`BottomSheet`** constructs its `SheetState` with `initialValue = SheetValue.Expanded` (see
  `core/ui/components/BottomSheet.kt` + the colocated `rememberSheetState` helper).
  `ModalBottomSheet`'s show-from-bottom animation is a no-op at that point — the sheet renders at
  its target position from the first composition.
- The status / nav bar `WindowCompat` configuration is applied from a **`LaunchedEffect`**, not a
  `SideEffect`, so it does not run on every recomposition.

## Baseline profile

The `:benchmark` module (root-level, next to `:app`) generates a baseline profile that is packaged
into the release APK and applied at install time by `androidx.profileinstaller`. This precompiles
the startup-critical Compose / M3 runtime methods in AOT, cutting ~10-15ms off `TotalTime`.

### The `benchmarkRelease` / `nonMinifiedRelease` variants

`connectedBenchmarkReleaseAndroidTest` installs the `benchmarkRelease` variant on the device;
`generateBaselineProfile` installs `nonMinifiedRelease`. Both are auto-created by the
`androidx.baselineprofile` plugin and customised in `app/build.gradle.kts` (the `afterEvaluate`
block) to:

- **`applicationIdSuffix = ".benchmark"`** — installs as `com.pchmn.pixelishsearch.benchmark`,
  a distinct package from the production `com.pchmn.pixelishsearch`. The two coexist on the
  device, so benchmarking never overwrites (or Auto-Backup-restores) the release install's
  DataStore / Coil cache / permissions.
- **`signingConfig`** aligned with `release` — same keystore as production, AGP doesn't fall
  back to its debug key.
- **`app_name=PixelishBenchmark`** — distinct launcher label.

For realistic warm-cache cold-start numbers, install the benchmark variant once and use it a
bit before running the benchmark so its caches populate:

```bash
./gradlew :app:installBenchmarkRelease
# use the app a bit so AppIndex / Coil / DataStore caches populate
./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest
```

Note that `generateBaselineProfile` and `connectedBenchmarkReleaseAndroidTest` share the same
`.benchmark` applicationId but target different build types (`nonMinifiedRelease` vs
`benchmarkRelease`). Running one after the other triggers a variant-switch reinstall on the
benchmark package — the release install is unaffected, but the benchmark variant's accumulated
state resets. Re-prime it by using the app for a moment before measuring again.

### Regenerate the profile

```bash
# Device must be connected, screen on, USB debugging enabled
./gradlew :app:generateBaselineProfile
```

Output lands in `app/src/release/generated/baselineProfiles/`:

- `baseline-prof.txt` — general profile (all measured methods)
- `startup-prof.txt` — cold-start specific subset

Both files are versioned and packaged into the release APK automatically by the
`androidx.baselineprofile` consumer plugin applied to `:app`.

The profile is generated against `nonMinifiedRelease` (applicationId `…\.benchmark`) but applies
to `release` (applicationId `com.pchmn.pixelishsearch`) — entries are JVM method signatures
keyed by the **`namespace`**, which is identical across both variants. R8's mapping file
translates the unminified signatures captured here into their minified equivalents at release
packaging time.

Regenerate when **the cold-start path changes meaningfully**: new composable in `SearchScreen`, new
init code in `PixelishSearchApp.onCreate`, Compose / M3 version bump, etc. Otherwise the profile
ages slowly and stays valid.

### Measure the gain

```bash
./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest
```

Runs 3 startup benchmarks × 10 iterations each, reports median + min / max:

- `startupNone` — no AOT compilation (pessimistic baseline)
- `startupBaselineProfile` — AOT-compiled with our generated profile
- `startupFull` — fully AOT-compiled (optimistic ceiling)

The delta between `None` and `BaselineProfile` is the actual user-facing win attributable to the
profile.

#### Where to find the results

Results land under `benchmark/build/outputs/` once the run finishes. Three useful artifacts, all
under `connected_android_test_additional_output/benchmarkRelease/connected/<Device>/`:

- **Human-readable summary** — one `.txt` per benchmark (
  `additionaltestoutput.benchmark.message_…StartupBenchmark.startup{None,BaselineProfile,Full}.txt`)
  with the `min / median / max` line:
  ```
  timeToInitialDisplayMs   [min 155.9],   [median 172.2],   [max 186.5]
  ```
  Compare `startupNone` ↔ `startupBaselineProfile` to read the profile gain directly.
- **Full JSON** — `com.pchmn.pixelishsearch.benchmark-benchmarkData.json` contains every iteration ×
  every metric (`timeToInitialDisplayMs`, `timeToFullDisplayMs`). Useful for scripting or archiving.
- **Per-iteration Perfetto traces** — `StartupBenchmark_<variant>_iter00X_*.perfetto-trace`.
  Drag-and-drop into https://ui.perfetto.dev to inspect a single run. The `[median …](file://…)`
  links in the `.txt` summary point straight at the median iteration's trace.

Running the benchmark from Android Studio (gutter arrow next to the `StartupBenchmark` class) prints
the same summary inline in the **Run** window — handy to avoid digging through `build/outputs/`.

### Verify the profile is active on a manual install

```bash
./gradlew :app:installRelease
adb shell cmd package compile -f -m speed-profile com.pchmn.pixelishsearch
adb shell dumpsys package dexopt | grep -A 5 com.pchmn.pixelishsearch
# Expect "compilation_filter=speed-profile" in the output.
```

ProfileInstaller applies the profile lazily on first launch; the `cmd package compile` invocation
skips the wait. The shipped APK does **not** need this step — ART will eventually compile the
profile in background via `BackgroundDexOptService`.

## Quick cold-start check

For a fast eyeball check without spinning up the full macrobenchmark workflow, force-stop the
app and re-launch it a few times — `am start -W` prints `TotalTime` (frame-1 ready) and
`WaitTime` (Activity fully drawn including window animation):

```bash
PKG=com.pchmn.pixelishsearch.benchmark   # or com.pchmn.pixelishsearch for the release install
for i in 1 2 3 4 5; do
    adb shell am force-stop $PKG
    sleep 1
    adb shell am start -W -n $PKG/com.pchmn.pixelishsearch.search.MainActivity | grep TotalTime
done
```

Note the **absolute class name** `com.pchmn.pixelishsearch.search.MainActivity` (not
`$PKG/.search.MainActivity`). Relative class names in `am start -n` are appended to the
component's `<package>` argument, so for the `.benchmark` applicationId variant a relative form
would resolve to `com.pchmn.pixelishsearch.benchmark.search.MainActivity` — a class that
doesn't exist. The activity is registered in the manifest under its namespace-resolved name,
which is the same for both variants.

What to ignore / look for:

- **First iteration is noisier** — page caches cold, JIT not warmed. Take median of the last
  3-4 runs.
- **Drift above the headline metric** (~120-135ms on a Pixel 9 with baseline profile) often means
  thermal throttling. Cool the device, plug to power, retry.
- For statistically rigorous numbers, use `:benchmark:connectedBenchmarkReleaseAndroidTest` — it
  runs 10× iterations, drops the kernel page cache between runs, and reports median + min/max.

## Profiling with Perfetto

`PixelishSearchApp.onCreate`, `MainActivity.onCreate`, and `AppIndex.preload` are instrumented with
`androidx.tracing`:

- `trace("…") { … }` for synchronous blocks
- `Trace.beginAsyncSection / endAsyncSection` for sections that may suspend across dispatcher
  threads (e.g. inside `AppIndex.preload`'s `scope.launch`)

Slices like `PixelishSearchApp.onCreate`, `repos.construct`, `AppIndex.phaseA.readCache`,
`MainActivity.setContent` show up directly in Perfetto / Android Studio System Trace.

### Capture a cold-start trace

[`perfetto-trace.sh`](../perfetto-trace.sh) at the repo root takes care of everything: starts a
5-second Perfetto capture with the right ftrace / atrace categories, force-stops the app, triggers a
cold start, then writes `trace.perfetto-trace` next to the script (gitignored).

```bash
# One-time setup — download Google's helper (also gitignored)
curl -O https://raw.githubusercontent.com/google/perfetto/master/tools/record_android_trace
chmod +x record_android_trace

# Capture
./perfetto-trace.sh
```

Drop the resulting `trace.perfetto-trace` into https://ui.perfetto.dev to inspect.

### What to look for

- The orange **"Activity launching:"** slice on the `Actual Timeline` row corresponds to
  `TotalTime`. Click it, then press `Z` to zoom to selection.
- On the `main thread`:
    - `bindApplication` — process startup + `Application.onCreate`. The custom
      `PixelishSearchApp.onCreate` slice sits inside it.
    - `activityStart` → `MainActivity.onCreate` → `MainActivity.setContent`. `setContent` itself
      returns in <1ms; the actual Compose work is in the `Choreographer#doFrame` that follows.
- The first **`Choreographer#doFrame`** is the dominant block (~40ms). Inside:
  `Compose:initializeView`, `Compose:recompose`, `Compose:onRemembered`, `traversal`, `measure`,
  `layout`, `draw-VRI`.
- A **second `Choreographer#doFrame`** typically follows for the TextField focus animation. If it
  grows large (>50ms), check whether `adjustResize` has been reintroduced in the manifest, or
  whether something else is triggering window-insets animations.

## Common regressions

- **Cold start jumps by 50-100ms** → check `windowSoftInputMode` in the manifest. `adjustResize`
  will re-enable the IME insets relayout.
- **Second frame becomes very large** → something is firing a Compose animation right after the
  first frame. Suspects: a new `SideEffect` (use `LaunchedEffect` instead), a `StateFlow` that emits
  its first value just after the activity is drawn, or a `Modifier.imePadding()` reintroduced
  somewhere.
- **`TotalTime` drifts up after a Compose / M3 version bump** → regenerate the baseline profile. The
  profile may reference internal method signatures that no longer exist in the new version.
- **Benchmark device shows 2× the expected `TotalTime`** → device is thermal-throttled. Let it cool,
  plug to power, retry.
