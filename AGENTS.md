# AGENTS.md - PixelishSearch

PixelishSearch is a minimalist Android app that replicates the Pixel Launcher unified search UI. A transparent search Activity (launched from a homescreen widget) combines installed apps, contacts and Google web suggestions.

## Stack & UI constraints

- **Kotlin + Jetpack Compose + Material 3** (`material3:1.4.0-alpha15` for Material 3 Expressive).
- **Always use Material 3 Compose components first.** Before writing a custom component (sheet, button, list, dialog, text field, chip, etc.), check whether an existing `androidx.compose.material3.*` component does the job. Custom is only justified when no M3 component fits (e.g. the current `SuggestionList`).
- Min SDK 31, target SDK 35. JVM target 17.
- Dynamic Color (Material You) everywhere via `dynamicLight/DarkColorScheme` — no hardcoded palette.
- Font: `GoogleSans` via `DeviceFontFamilyName("google-sans")`, applied to every text style through `appTypography()` in `ui/theme/Theme.kt`.

## Build

```bash
./gradlew assembleDebug         # debug APK
./gradlew installDebug          # build + install on connected device
./gradlew lint                  # Android lint
```

No tests in the project yet.

## Architecture

Code lives in `app/src/main/java/com/pchmn/pixelishsearch/`.

**Ultra-fast cold start** is the main architectural goal:
- `PixelishApp` (Application class) preloads `AppIndex` and initializes `AppUsageRepository` / `SearchHistoryRepository` as soon as the process is created.
- `BootReceiver` re-triggers the preload on `BOOT_COMPLETED`.
- `MainActivity` is launched transparent (theme `Theme.PixelishSearch.Transparent`) with `FLAG_BLUR_BEHIND` for the Pixel Search blur effect over the launcher.

**State singletons (Kotlin `object`) exposing `StateFlow`** — no DI framework:
- `AppIndex`: in-memory index of launchable apps, fuzzy search `startsWith` then `contains`, accent normalization.
- `AppUsageRepository`: per-package launch counter persisted in DataStore Preferences. `scoreOf()` applies exponential decay (14-day half-life) to combine frequency and recency.
- `SearchHistoryRepository`: query history (DataStore).
- `ContactRepository`: live search via `ContentResolver` (`READ_CONTACTS` permission).
- `WebSuggestRepository`: Google Suggest API via Ktor.

**UI**:
- `SearchScreen` is a `ModalBottomSheet` containing the `TextField`, an `AppRow` (4 fixed slots) and a `SuggestionList` (history when the query is empty, web suggestions otherwise).
- `SearchViewModel` orchestrates everything: instant local search on every keystroke, web search debounced at 180 ms, `combine` of `AppIndex.apps` + `AppUsageRepository.stats` for the default suggestions.
- The `ModalBottomSheet` lives in its own Dialog window: to make status / nav bar icons follow the theme, retrieve that window via `findDialogWindow()` and call `WindowCompat.getInsetsController(...)` from a `SideEffect`.

**Widget**: `SearchWidget` (`AppWidgetProvider`) simply launches `MainActivity`. Layout in `res/layout/widget_search_bar.xml`, config in `res/xml/widget_info.xml`.
