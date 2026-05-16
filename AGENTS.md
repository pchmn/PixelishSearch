# AGENTS.md - PixelishSearch

PixelishSearch is a minimalist Android app that replicates the Pixel Launcher unified search UI. A transparent search Activity (launched from a homescreen widget) combines installed apps, contacts and Google web suggestions.

## Stack & UI constraints

- **Kotlin + Jetpack Compose + Material 3** (`material3:1.4.0-alpha15` for Material 3 Expressive).
- **Always use Material 3 Compose components first.** Before writing a custom component (sheet, button, list, dialog, text field, chip, etc.), check whether an existing `androidx.compose.material3.*` component does the job. Custom is only justified when no M3 component fits.
- Min SDK 31, target SDK 37. JVM target 17.
- Dynamic Color (Material You) everywhere via `dynamicLight/DarkColorScheme` — no hardcoded palette.
- Font: `GoogleSans` via `DeviceFontFamilyName("google-sans")`, applied to every text style through `appTypography()` in `core/ui/theme/Theme.kt`.
- **App icons go through Coil 3** with a custom `AppIconFetcher` (`search/apps/data/AppIconFetcher.kt`). UI code never holds a `Drawable` directly — it passes an `AppIconRequest(packageName, lastUpdateTime)` to `AsyncImage`. The `lastUpdateTime` is part of Coil's cache key so updates invalidate automatically.

## Build

```bash
./gradlew assembleDebug         # debug APK
./gradlew installDebug          # build + install on connected device
./gradlew assembleRelease       # signed release APK (requires keystore.properties)
./gradlew compileDebugKotlin    # quick verification after edits
./gradlew lint                  # Android lint
```

No tests in the project yet. Debug-build cold start performance is **not** representative — always measure on release.

## Architecture

Code lives in `app/src/main/java/com/pchmn/pixelishsearch/` and is organised **by feature**, with two shared packages: `core/` for **technical** primitives (theme, generic Compose components, intent helper, hot-StateFlow history base) and `common/` for **domain** primitives shared across features. Each feature owns its own `data/` (repos, DataStore delegates, intent launchers, receivers) and `ui/` (Compose screens / rows) subpackages. The top-level layout is:

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
│   └── web/{data,ui}/                # WebSuggestionsRepository, history, launcher, list/row
├── settings/                         # SettingsActivity + data/SettingsRepository + ui/SettingsScreen
├── update/data/                      # GithubReleaseApi, UpdateRepository, UpdateDataStore (init, not wired yet)
└── widget/                           # SearchWidget (AppWidgetProvider)
```

**Ultra-fast cold start** is the main architectural goal. Multiple layers contribute:

1. **`PixelishSearchApp`** (`Application`) — at process creation:
   - Constructs the three history repos (`AppHistoryRepository`, `WebSearchHistoryRepository`, `ContactHistoryRepository`) + the `HiddenAppsRepository` + the `SettingsRepository`; each `.stateIn(appScope, Eagerly, ...)` starts collecting DataStore eagerly on `Dispatchers.Default`.
   - `AppIndex.preload(this, appScope)` — async, see below.
   - `WebSuggestionsRepository.warmUp(appScope)` — fires a dummy request to warm the TLS connection to Google Suggest.
   - `ContactRepository.warmUp(this, appScope)` — fires a `LIMIT 1` query so the (out-of-process) Contacts ContentProvider is alive and the binder is warm.
   - Implements `SingletonImageLoader.Factory` to register Coil's `AppIconKeyer` + `AppIconFetcher.Factory()`.

2. **`AppIndex.preload`** runs in 2 phases on the background scope:
   - **Phase A (hydrate)** — reads the persisted cache via `AppIndexCacheRepository`, reconstructs `AppEntry` objects (the `launchIntent` is rebuilt from `ComponentName(pkg, activityClassName)` — no PM call needed), publishes into `_apps`. ~5-10ms on warm storage.
   - **Phase B (refresh)** — re-enumerates via `pm.queryIntentActivities` + `getPackageInfo`. No icon loading (Coil handles that lazily). Updates `_apps` and persists the cache only if the list differs. ~50-100ms.

3. **`BootReceiver`** (`search/apps/data/`) retriggers `AppIndex.preload` on `BOOT_COMPLETED` — mostly to warm the page cache of the DataStore files. Marginal gain since the persistent cache already makes the first post-boot tap fast.

4. **`PackageReceiver`** (`search/apps/data/`) listens to `PACKAGE_ADDED / REMOVED / REPLACED` and calls `AppIndex.refresh(...)` so installs/uninstalls show up at the next tap without waiting for the process to restart.

5. **`MainActivity`** (in `search/`) uses theme `Theme.PixelishSearch.Transparent` + `FLAG_BLUR_BEHIND` for the Pixel Search blur over the launcher. Activity is `singleTask` + `excludeFromRecents` — re-tap reuses the instance and `onNewIntent` resets the query.

### Core (`core/`)

- **`core/data/HistoryRepository<T, K>`** — abstract base; exposes `items: StateFlow<List<T>>` via `.stateIn(scope, Eagerly, emptyList())`. Each entry implements `HistoryEntry` with an exponential time-decay `score()` (14-day half-life). Subclassed per feature (`AppHistoryRepository`, `WebSearchHistoryRepository`, `ContactHistoryRepository`).
- **`core/data/LaunchIntents.kt`** — only contains `Context.launchAndDismiss(intent)`, the helper every feature-specific `*Launcher.kt` calls into. Feature-specific intent builders (Gemini, Lens, app info, pin shortcut, dialer, SMS, contact details, Google search URL) live next to their feature in `search/apps/data/AppLauncher.kt`, `search/contacts/data/ContactLauncher.kt`, `search/web/data/WebSearchLauncher.kt`.
- **`core/ui/components/`** — feature-agnostic Compose pieces: `BottomSheet` (wrapper around `ModalBottomSheet`), `EntryList` / `EntryRow` (generic list scaffolding used by web + contact lists), `AnchorBox`, and the `dropdown/` subpackage (`DropdownMenuWithArrow`, `CalloutShape`) used by the long-press menu.
- **`core/ui/theme/Theme.kt`** — Material 3 + Dynamic Color + `GoogleSans` typography. Two themes: `Theme.PixelishSearch` (opaque, used by `SettingsActivity`) and `Theme.PixelishSearch.Transparent` (used by `MainActivity`).
- The `ModalBottomSheet` lives in its own Dialog window: to make status / nav bar icons follow the theme, retrieve that window via `findDialogWindow()` and call `WindowCompat.getInsetsController(...)` from a `SideEffect`.

### Search feature (`search/`)

DataStore delegates are colocated with each sub-feature: `search/apps/data/AppDataStore.kt` declares `appHistoryDatastore`, `appIndexCacheDataStore`, `hiddenAppsDataStore`; `search/contacts/data/ContactDataStore.kt` declares `contactHistoryDataStore`; `search/web/data/WebSearchDataStore.kt` declares `searchHistoryDataStore`.

- **`search/ui/SearchScreen`** — a `BottomSheet` containing `SearchField`, an `AppList` (4 fixed slots), and either history (`WebSearchList` + `ContactRecentList`) or live results (`WebSearchList` + `ContactResultList`) depending on whether the query is blank. A gear `IconButton` sits at the bottom-right of the recents state and launches `SettingsActivity`. Long-pressing an `AppItem` opens a `DropdownMenuWithArrow` (from `core/ui/components/dropdown/`) with **App info**, **Add to home screen** and **Don't suggest app**.
- **`search/ui/SearchViewModel`** — orchestrates everything: instant local search on every keystroke, web search debounced at 90ms, `combine` of `AppIndex.apps` + `appHistory.recents` + `hiddenApps.hidden` for the default suggestions (hidden packages are filtered out before ranking). Contact recents are combined with `settings.contactSearchEnabled` so flipping the toggle clears the section; `runLocalSearch` also short-circuits the contact query when the setting is off. **No defensive `AppIndex.preload` call** — `Application.onCreate` is always run first.
- **`search/apps/`**
  - `data/AppIndex` (Kotlin `object`) — in-memory index, fuzzy search `startsWith` then `contains`, accent normalization. Exposes `apps: StateFlow<List<AppEntry>>`.
  - `data/AppEntry` — `label`, `packageName`, `launchIntent`, `lastUpdateTime`. **No `Drawable`** — icons come from Coil.
  - `data/AppIndexCacheRepository` — persists a JSON snapshot of the app list in `appIndexCacheDataStore`. Stores `activityClassName` so the launch Intent is rebuilt purely in Kotlin at restore time.
  - `data/AppIconFetcher` + `AppIconKeyer` — Coil custom fetcher that resolves `AppIconRequest(pkg, lastUpdateTime)` via `pm.getApplicationIcon(pkg)`. Cache key is `app-icon://$pkg/$lastUpdateTime`.
  - `data/AppHistoryRepository` — per-package launch counter, exposed as `recents`.
  - `data/HiddenAppsRepository` — DataStore-backed `Set<String>` of packages the user opted out of the default suggestion strip ("Don't suggest app" menu). Exposes a hot `hidden: StateFlow<Set<String>>` and `hide` / `unhide` mutators. Hidden apps stay searchable; they're only filtered out of `appRecents`.
  - `data/AppLauncher.kt` — `geminiIntent`, `lensIntent`, `launchAppInfo`, `pinAppShortcut`.
  - `data/BootReceiver`, `data/PackageReceiver` — see above.
  - `ui/AppList`, `ui/AppItem` (Coil `AsyncImage`).
- **`search/contacts/`**
  - `data/ContactRepository` (object) — live search via `ContentResolver` (`READ_CONTACTS` permission), plus `warmUp(context, scope)`.
  - `data/ContactHistoryRepository` — contact-action history.
  - `data/ContactLauncher.kt` — `launchContactDetails`, `launchSms`, `launchDialer`.
  - `data/UriSerializer.kt` — kotlinx-serialization adapter for `android.net.Uri`.
  - `ui/ContactResultList`, `ui/ContactResultRow`, `ui/ContactRecentList`, `ui/ContactRecentRow`, `ui/ContactAvatar`.
  - `utils/ContactUtils.kt`.
- **`search/web/`**
  - `data/WebSuggestionsRepository` (object) — Google Suggest API via Ktor + `LruCache`, plus `warmUp(scope)`.
  - `data/WebSearchHistoryRepository` — query history.
  - `data/WebSearchLauncher.kt` — `launchGoogleSearch`.
  - `ui/WebSearchList`, `ui/WebSearchRow`.

### Settings (`settings/`)

- **`SettingsActivity` + `ui/SettingsScreen`** — opaque activity (regular `Theme.PixelishSearch`) with a Material 3 `LargeTopAppBar`, "General" section header, and a single rounded `ListItem` "Search contacts" with a trailing `Switch`. Effective state = `contactSearchEnabled && hasReadContacts`. Toggling on requests `READ_CONTACTS` if missing; the launcher's `granted` callback flips the setting on success. On API 33+ an "Appearance" section also appears with a `LanguagePreference` row that opens an `AlertDialog` of radio buttons (System / EN / FR / ES / DE / IT) and writes through `LocaleManager.applicationLocales`.
- **`data/SettingsRepository`** — DataStore-backed user preferences (`settingsDataStore`). Currently only stores `contactSearchEnabled: StateFlow<Boolean>` (default false). The toggle gates contact search and contact recents in `SearchViewModel`.

### Update (`update/`)

Self-update module — currently scaffolded and **not wired into `PixelishSearchApp` yet**. Reuse the preload/warmup pattern when integrating it.

- **`data/GithubReleaseApi`** — Ktor client against `https://api.github.com/repos/pchmn/PixelishSearch/releases/latest`, plus `parseVersion` / `compareVersions` helpers.
- **`data/UpdateRepository`** — DataStore-backed (`updateDataStore`) `available: StateFlow<UpdateInfo?>` representing a downloaded-and-ready-to-install APK (kept in the app's external files dir for FileProvider exposure).

### Widget

`SearchWidget` (`AppWidgetProvider`) simply launches `MainActivity`. Layout in `res/layout/widget_search_bar.xml`, config in `res/xml/widget_info.xml`.

## Conventions

- **Feature-first packaging.** New code goes under the owning feature (`search/<sub>/`, `settings/`, `update/`, `widget/`). Only promote something to a shared package once a second feature reaches for it — premature shared utilities are worse than two near-duplicate functions. When you do promote:
  - `core/` for **technical / infrastructure** code with no domain knowledge — themes, generic Compose components, intent helpers, DataStore-backed base classes, networking adapters. Should be reusable in a different app without modification.
  - `common/` for **domain / business** code shared across features — domain models, business rules, scoring formulas, entity-level types. Specific to PixelishSearch's concepts even if used by several features.
  - If a piece could plausibly land in either, ask: "would I copy this into an unrelated Android app?". Yes → `core/`. No → `common/`.
- **`data/` vs `ui/` split per feature.** Repositories, DataStore delegates, `BroadcastReceiver`s, and intent launchers live under `<feature>/data/`. Composables and their `ViewModel` live under `<feature>/ui/`. Pure helpers without Android dependencies can sit in `<feature>/utils/` (cf. `search/contacts/utils/ContactUtils.kt`).
- **DataStore delegates are colocated with their feature.** Add new `Context.xxxDataStore` extensions in a `<feature>/data/<Feature>DataStore.kt` file rather than a global file.
- **Intent launchers split between `core/` and features.** The generic `launchAndDismiss` (start + close-without-animation) lives in `core/data/LaunchIntents.kt`; feature-specific intent builders (Gemini, dialer, Google search, etc.) live in `<feature>/data/<Feature>Launcher.kt` and call through `launchAndDismiss`.
- **Preload / warmup pattern** for expensive one-shot startup work : the function takes `(scope: CoroutineScope, ...)` and encapsulates its own `launch(Dispatchers.X)`. Callers in `PixelishSearchApp.onCreate` line up symmetrically:
  ```kotlin
  AppIndex.preload(this, appScope)
  WebSuggestionsRepository.warmUp(appScope)
  ContactRepository.warmUp(this, appScope)
  ```
- **No DI framework.** Singletons are Kotlin `object`s, stateful repos are classes constructed in `PixelishSearchApp.onCreate` and exposed as `lateinit var` properties.
- **DataStore flows are hot.** Every `HistoryRepository<T>.items` is a `StateFlow` started eagerly so the search VM doesn't pay a decode cost when it subscribes.
- **i18n.** All user-facing strings live in `res/values/strings.xml` (English default) with translations in `values-fr/`, `values-es/`, `values-de/`, `values-it/`. Supported locales are declared in `res/xml/locale_config.xml` and referenced from `<application android:localeConfig=...>`, which exposes the native per-app language picker in Android 13+ system settings. The in-app picker in `SettingsScreen` uses `LocaleManager.setApplicationLocales` (API 33+); on 31/32 the row is hidden and the app follows the system locale.

## Post-Modification Verification

After any code change, run:

```bash
./gradlew compileDebugKotlin
```

Skip for changes that only touch docs / markdown / license files.

Also check whether `AGENTS.md` or `README.md` need updating to reflect your changes (new modules, moved files, changed patterns, etc.).
