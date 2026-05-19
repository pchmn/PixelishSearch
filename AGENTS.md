# AGENTS.md - PixelishSearch

PixelishSearch is a minimalist Android app that replicates the Pixel Launcher unified search UI. A
transparent search Activity (launched from a homescreen widget) combines installed apps, contacts
and Google web suggestions.

## Stack & UI constraints

- **Kotlin + Jetpack Compose + Material 3** (`material3:1.4.0-alpha15` for Material 3 Expressive).
- **Always use Material 3 Compose components first.** Before writing a custom component (sheet,
  button, list, dialog, text field, chip, etc.), check whether an existing
  `androidx.compose.material3.*` component does the job. Custom is only justified when no M3
  component fits.
- Min SDK 31, target SDK 37. JVM target 17.
- Dynamic Color (Material You) everywhere via `dynamicLight/DarkColorScheme` — no hardcoded palette.
- Font: `GoogleSans` via `DeviceFontFamilyName("google-sans")`, applied to every text style through
  `appTypography()` in `core/ui/theme/Theme.kt`.
- **App icons go through Coil 3** with a custom `AppIconFetcher` (
  `search/apps/data/AppIconFetcher.kt`). UI code never holds a `Drawable` directly — it passes an
  `AppIconRequest(packageName, lastUpdateTime)` to `AsyncImage`. The `lastUpdateTime` is part of
  Coil's cache key so updates invalidate automatically.

## Build

```bash
./gradlew assembleDebug         # debug APK
./gradlew installDebug          # build + install on connected device
./gradlew assembleRelease       # signed release APK (requires keystore.properties)
./gradlew installRelease        # build + install release on connected device
./gradlew compileDebugKotlin    # quick verification after edits
./gradlew lint                  # Android lint
```

No unit/instrumentation tests in `:app`. The `:benchmark` module hosts macrobenchmark + baseline
profile generation — see [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md) for the regeneration /
measurement workflow. Debug-build cold start performance is **not** representative — always measure
on release.

## Architecture

The repository has two Gradle modules:

```
PixelishSearch/
├── app/                              # The Android app — described in detail below
└── benchmark/                        # Macrobenchmark + baseline profile generator (see Performance section)
```

`:app`'s code lives in `app/src/main/java/com/pchmn/pixelishsearch/` and is organised **by feature
**, with two shared packages: `core/` for **technical** primitives (theme, generic Compose
components, intent helper, hot-StateFlow history base) and `common/` for **domain** primitives
shared across features. Each feature owns its own `data/` (repos, DataStore delegates, intent
launchers, receivers) and `ui/` (Compose screens / rows) subpackages. The top-level layout is:

```
pixelishsearch/
├── PixelishSearchApp.kt              # Application: preloads index, warmups, Coil setup
├── core/                             # Shared *technical* code (no domain knowledge)
│   ├── data/                         # LaunchIntents (launchAndDismiss), HistoryRepository
│   └── ui/
│       ├── components/               # BottomSheet, EntryList, EntryRow, AnchorBox, dropdown/
│       └── theme/                    # Theme.kt (Material 3 + Dynamic Color + Google Sans)
├── common/                           # Shared *domain* code reused across features (currently empty)
├── search/                           # Search activity + feature-specific data/ui
│   ├── MainActivity.kt
│   ├── ui/                           # SearchScreen, SearchField, SearchViewModel
│   ├── apps/{data,ui}/               # AppIndex & cache, history, hidden, icon fetcher, launchers, receivers
│   ├── contacts/{data,ui,utils}/     # ContactRepository, history, launchers, list/row composables
│   ├── settings/{data,ui}/           # Quick-toggle tiles + curated Settings pages (resolved via PM) — static lists + launchers
│   └── web/{data,ui}/                # WebSuggestionsRepository, history, launcher, list/row
├── settings/                         # SettingsActivity + data/SettingsRepository + ui/SettingsScreen
├── update/                           # Self-update from GitHub Releases (UpdateActivity + data/ + ui/UpdateScreen)
└── widget/                           # SearchWidget (AppWidgetProvider)
```

**Ultra-fast cold start** is the main architectural goal. The strategy stacks:

- **Eager state in `PixelishSearchApp.onCreate`** — DataStore-backed repos use
  `.stateIn(appScope, Eagerly, ...)` so the search VM doesn't pay a decode cost when it subscribes;
  expensive one-shots (`AppIndex.preload`, repository `warmUp`s for the contacts ContentProvider
  binder and Google Suggest TLS) run async on `appScope`. Coil is wired up here too.
- **App index** hydrates from a persisted cache (no PM call needed) then refreshes from
  `PackageManager` in the background. `BootReceiver` and `PackageReceiver` keep it fresh across
  reboots and install / remove / replace.
- **`MainActivity`** uses `Theme.PixelishSearch.Transparent` + `FLAG_BLUR_BEHIND`, declared
  `singleTask + excludeFromRecents` so re-tap reuses the instance (`onNewIntent` resets the query).
- **First-frame tuning** — manifest sets `windowSoftInputMode="stateAlwaysVisible|adjustPan"` (IME
  doesn't trigger a Compose relayout) and `BottomSheet` starts at `initialValue = SheetValue.Expanded`
  (no show-from-bottom animation). A baseline profile from `:benchmark` ships in release builds via
  `androidx.profileinstaller`. See [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md).

## Conventions

- **Feature-first packaging.** New code goes under the owning feature; promote to `core/`
  (technical, reusable in another Android app) or `common/` (domain, PixelishSearch-specific) only
  once a second feature needs it.
- **`data/` vs `ui/` per feature.** Repos, DataStore delegates, receivers and launchers under
  `<feature>/data/`; Composables and their `ViewModel` under `<feature>/ui/`; pure
  Android-independent helpers under `<feature>/utils/`.
- **DataStore delegates colocated.** `Context.xxxDataStore` extensions go in
  `<feature>/data/<Feature>DataStore.kt`, never a global file.
- **Intent launchers.** Generic `launchAndDismiss` (start + close-without-animation) lives in
  `core/data/LaunchIntents.kt`; feature-specific intent builders go in
  `<feature>/data/<Feature>Launcher.kt` and always call through it.
- **Preload / warmup pattern.** Expensive one-shot startup work exposes
  `fun preload/warmUp(scope: CoroutineScope, ...)` and encapsulates its own `launch(Dispatchers.X)`.
  Callers in `PixelishSearchApp.onCreate` line up symmetrically. `UpdateChecker.check` uses the same
  shape but runs from `MainActivity` instead — `Application.onCreate` may not re-run for days.
- **No DI framework.** Singletons are Kotlin `object`s; stateful repos are classes constructed in
  `PixelishSearchApp.onCreate` and exposed as `lateinit var` properties.
- **Hot DataStore flows.** `HistoryRepository<T>.items` is a `StateFlow` started eagerly so the
  search VM doesn't pay a decode cost when it subscribes.
- **i18n.** English default in `res/values/strings.xml` + per-locale overrides
  (`values-{fr,es,de,it}/`); in-app language picker uses `LocaleManager.setApplicationLocales`
  (API 33+).

## Post-Modification Verification

After any code change, run:

```bash
./gradlew compileDebugKotlin
```

Skip for changes that only touch docs / markdown / license files.

Also check whether `AGENTS.md` or `README.md` need updating to reflect your changes (new modules,
moved files, changed patterns, etc.).
