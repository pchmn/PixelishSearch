# AGENTS.md - PixelishSearch

PixelishSearch is a minimalist Android app that replicates the Pixel Launcher unified search UI. A transparent search Activity (launched from a homescreen widget) combines installed apps, contacts and Google web suggestions.

## Stack & UI constraints

- **Kotlin + Jetpack Compose + Material 3** (`material3:1.4.0-alpha15` for Material 3 Expressive).
- **Always use Material 3 Compose components first.** Before writing a custom component (sheet, button, list, dialog, text field, chip, etc.), check whether an existing `androidx.compose.material3.*` component does the job. Custom is only justified when no M3 component fits.
- Min SDK 31, target SDK 37. JVM target 17.
- Dynamic Color (Material You) everywhere via `dynamicLight/DarkColorScheme` — no hardcoded palette.
- Font: `GoogleSans` via `DeviceFontFamilyName("google-sans")`, applied to every text style through `appTypography()` in `ui/theme/Theme.kt`.
- **App icons go through Coil 3** with a custom `AppIconFetcher` (`data/AppIconFetcher.kt`). UI code never holds a `Drawable` directly — it passes an `AppIconRequest(packageName, lastUpdateTime)` to `AsyncImage`. The `lastUpdateTime` is part of Coil's cache key so updates invalidate automatically.

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

Code lives in `app/src/main/java/com/pchmn/pixelishsearch/`.

**Ultra-fast cold start** is the main architectural goal. Multiple layers contribute:

1. **`PixelishSearchApp`** (`Application`) — at process creation:
   - Constructs the three history repos (`AppHistory`, `WebSearchHistory`, `ContactHistory`) + the `HiddenAppsRepository`; each `.stateIn(appScope, Eagerly, ...)` starts collecting DataStore eagerly on `Dispatchers.Default`.
   - `AppIndex.preload(this, appScope)` — async, see below.
   - `WebSuggestRepository.warmUp(appScope)` — fires a dummy request to warm the TLS connection to Google Suggest.
   - `ContactRepository.warmUp(this, appScope)` — fires a `LIMIT 1` query so the (out-of-process) Contacts ContentProvider is alive and the binder is warm.
   - Implements `SingletonImageLoader.Factory` to register Coil's `AppIconKeyer` + `AppIconFetcher.Factory()`.

2. **`AppIndex.preload`** runs in 2 phases on the background scope:
   - **Phase A (hydrate)** — reads the persisted cache via `AppIndexCacheRepository`, reconstructs `AppEntry` objects (the `launchIntent` is rebuilt from `ComponentName(pkg, activityClassName)` — no PM call needed), publishes into `_apps`. ~5-10ms on warm storage.
   - **Phase B (refresh)** — re-enumerates via `pm.queryIntentActivities` + `getPackageInfo`. No icon loading (Coil handles that lazily). Updates `_apps` and persists the cache only if the list differs. ~50-100ms.

3. **`BootReceiver`** retriggers `AppIndex.preload` on `BOOT_COMPLETED` — mostly to warm the page cache of the DataStore files. Marginal gain since the persistent cache already makes the first post-boot tap fast.

4. **`PackageReceiver`** listens to `PACKAGE_ADDED / REMOVED / REPLACED` and calls `AppIndex.refresh(...)` so installs/uninstalls show up at the next tap without waiting for the process to restart.

5. **`MainActivity`** uses theme `Theme.PixelishSearch.Transparent` + `FLAG_BLUR_BEHIND` for the Pixel Search blur over the launcher. Activity is `singleTask` + `excludeFromRecents` — re-tap reuses the instance and `onNewIntent` resets the query.

### Data layer (`data/`)

- **`AppIndex`** (Kotlin `object`) — in-memory index, fuzzy search `startsWith` then `contains`, accent normalization. Exposes `apps: StateFlow<List<AppEntry>>`.
- **`AppEntry`** — `label`, `packageName`, `launchIntent`, `lastUpdateTime`. **No `Drawable`** — icons come from Coil.
- **`AppIndexCacheRepository`** — persists a JSON snapshot of the app list in its own DataStore (`appIndexCacheDataStore`). Stores `activityClassName` so the launch Intent is rebuilt purely in Kotlin at restore time.
- **`AppIconFetcher` + `AppIconKeyer`** — Coil custom fetcher that resolves `AppIconRequest(pkg, lastUpdateTime)` via `pm.getApplicationIcon(pkg)`. Cache key is `app-icon://$pkg/$lastUpdateTime`.
- **`HistoryRepository<T, K>`** — abstract base; exposes `items: StateFlow<List<T>>` via `.stateIn(scope, Eagerly, emptyList())`. Used by:
  - **`AppHistoryRepository`** — per-package launch counter, exposed as `recents`.
  - **`WebSearchHistoryRepository`** — query history.
  - **`ContactHistoryRepository`** — contact-action history.
  - Each entry implements `HistoryEntry` with an exponential time-decay `score()` (14-day half-life).
- **`ContactRepository`** (object) — live search via `ContentResolver` (`READ_CONTACTS` permission), plus `warmUp(context, scope)`.
- **`WebSuggestRepository`** (object) — Google Suggest API via Ktor + `LruCache`, plus `warmUp(scope)`.
- **`HiddenAppsRepository`** — DataStore-backed `Set<String>` of packages the user opted out of the default suggestion strip ("Don't suggest app" menu). Exposes a hot `hidden: StateFlow<Set<String>>` and `hide` / `unhide` mutators. Hidden apps stay searchable; they're only filtered out of `appRecents`.

### UI (`ui/`)

- **`SearchScreen`** — a `BottomSheet` (custom wrapper around `ModalBottomSheet`) containing `SearchField`, an `AppList` (4 fixed slots), and either history (`WebSearchList` + `ContactRecentList`) or live results (`WebSearchList` + `ContactResultList`) depending on whether the query is blank. Long-pressing an `AppItem` opens a Material 3 `DropdownMenu` with **App info** (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`), **Add to home screen** (`ShortcutManager.requestPinShortcut`) and **Don't suggest app** (`HiddenAppsRepository.hide`).
- **`SearchViewModel`** — orchestrates everything: instant local search on every keystroke, web search debounced at 90ms, `combine` of `AppIndex.apps` + `appHistory.recents` + `hiddenApps.hidden` for the default suggestions (hidden packages are filtered out before ranking). **No defensive `AppIndex.preload` call** — `Application.onCreate` is always run first.
- Subpackages: `ui/app/` (AppList, AppItem), `ui/contact/` (ContactResultList, ContactRecentList, ContactUtils, ContactAvatar), `ui/websearch/` (WebSearchList, WebSearchRow), `ui/bottomsheet/` (BottomSheet wrapper), `ui/theme/`.
- The `ModalBottomSheet` lives in its own Dialog window: to make status / nav bar icons follow the theme, retrieve that window via `findDialogWindow()` and call `WindowCompat.getInsetsController(...)` from a `SideEffect`.

### Widget

`SearchWidget` (`AppWidgetProvider`) simply launches `MainActivity`. Layout in `res/layout/widget_search_bar.xml`, config in `res/xml/widget_info.xml`.

## Conventions

- **Preload / warmup pattern** for expensive one-shot startup work : the function takes `(scope: CoroutineScope, ...)` and encapsulates its own `launch(Dispatchers.X)`. Callers in `PixelishSearchApp.onCreate` line up symmetrically:
  ```kotlin
  AppIndex.preload(this, appScope)
  WebSuggestRepository.warmUp(appScope)
  ContactRepository.warmUp(this, appScope)
  ```
- **No DI framework.** Singletons are Kotlin `object`s, stateful repos are classes constructed in `PixelishSearchApp.onCreate` and exposed as `lateinit var` properties.
- **DataStore flows are hot.** Every `HistoryRepository<T>.items` is a `StateFlow` started eagerly so the search VM doesn't pay a decode cost when it subscribes.

## Post-Modification Verification

After any code change, run:

```bash
./gradlew compileDebugKotlin
```

Skip for changes that only touch docs / markdown / license files.

Also check whether `AGENTS.md` or `README.md` need updating to reflect your changes (new modules, moved files, changed patterns, etc.).
