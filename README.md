# PixelishSearch

Minimalist Android app that replicates the Pixel Launcher unified search UI (pre-November 2025 Feature Drop), lighter and faster than PixelSearch.

## Features

- Installed apps search (local, instant)
- Default app suggestions ranked by usage frequency + recency (14-day half-life decay)
- Search history (recent queries) with manual removal
- Contact suggestions (with permission) + recent contact actions (call, SMS, view)
- Web suggestions via Google Suggest API
- Homescreen widget that opens the search
- Transparent search activity with blur-behind effect over the launcher
- Persistent app index cache + Coil disk cache for icons → near-instant cold start
- Material 3 Expressive + Dynamic Color (matches the system theme)
- Automatic dark / light mode
- Google Sans font

## Stack

- Kotlin
- Jetpack Compose + Material 3 (`1.4.0-alpha15` for Expressive)
- Coroutines + Flow (hot `StateFlow`s for repos)
- DataStore Preferences (usage stats, history, persisted app index)
- Coil 3 (in-memory + disk cache for app icons, custom `PackageManager` fetcher)
- Ktor (Google Suggest client)
- Min SDK 31 (Android 12, for Dynamic Color)
- Target SDK 37
- JVM target 17

## Architecture

The code is organised **by feature**, with two shared packages: `core/` for *technical* primitives (theme, generic Compose components, intent helper, history base class) and `common/` for *domain* primitives shared across features. Each feature owns its own `data/` and `ui/` subpackages.

```
app/src/main/
├── java/com/pchmn/pixelishsearch/
│   ├── PixelishSearchApp.kt             # Application: preloads index, warmups, Coil setup
│   ├── core/                            # Shared technical code (no domain knowledge)
│   │   ├── data/
│   │   │   ├── HistoryRepository.kt     # Abstract base: hot StateFlow over DataStore
│   │   │   └── LaunchIntents.kt         # launchAndDismiss helper
│   │   └── ui/
│   │       ├── components/              # BottomSheet, EntryList, EntryRow, AnchorBox, dropdown/
│   │       └── theme/Theme.kt           # Material 3 + Dynamic Color + Google Sans
│   ├── common/                          # Shared domain code (currently empty)
│   ├── search/
│   │   ├── MainActivity.kt              # Transparent search activity
│   │   ├── ui/                          # SearchScreen, SearchField, SearchViewModel
│   │   ├── apps/
│   │   │   ├── data/                    # AppIndex(+Cache), AppHistory, HiddenApps,
│   │   │   │                            #   AppIconFetcher, AppLauncher (Gemini/Lens/info/pin),
│   │   │   │                            #   BootReceiver, PackageReceiver, AppDataStore
│   │   │   └── ui/                      # AppList, AppItem (Coil AsyncImage)
│   │   ├── contacts/
│   │   │   ├── data/                    # ContactRepository (+warmup), ContactHistory,
│   │   │   │                            #   ContactLauncher (call/SMS/details), UriSerializer
│   │   │   ├── ui/                      # ContactResult/Recent List+Row, ContactAvatar
│   │   │   └── utils/ContactUtils.kt
│   │   └── web/
│   │       ├── data/                    # WebSuggestionsRepository (Ktor + warmup),
│   │       │                            #   WebSearchHistory, WebSearchLauncher
│   │       └── ui/                      # WebSearchList, WebSearchRow
│   ├── settings/
│   │   ├── SettingsActivity.kt
│   │   ├── data/SettingsRepository.kt   # contactSearchEnabled
│   │   └── ui/SettingsScreen.kt         # Material 3, language picker (API 33+)
│   ├── update/data/                     # GithubReleaseApi, UpdateRepository (scaffolded)
│   └── widget/SearchWidget.kt           # AppWidgetProvider
└── res/
    ├── layout/widget_search_bar.xml
    ├── xml/widget_info.xml, locale_config.xml
    └── drawable/, anim/, values/, values-{fr,es,de,it}/
```

## Performance

Cold start of the search activity is the main goal. The path is optimized at every layer:

- **`PixelishSearchApp` (Application)** preloads everything async at process creation:
  - `AppIndex.preload` (2-phase: hydrate from persisted cache → re-enumerate)
  - `WebSuggestionsRepository.warmUp` warms the TLS handshake to Google Suggest
  - `ContactRepository.warmUp` wakes the (out-of-process) Contacts provider
  - History repos start collecting DataStore eagerly via hot `StateFlow`s
- **Persisted app index** (`AppIndexCacheRepository`) — the launcher list is cached in DataStore. On any subsequent cold start, the `AppRow` populates from disk in ~5-10ms without any PackageManager call.
- **Coil-cached icons** — `AppIconFetcher` resolves icons via PackageManager once; subsequent loads hit Coil's disk cache (PNG). Cache key includes `lastUpdateTime` so updates invalidate automatically.
- **`PackageReceiver`** listens for installs / uninstalls / updates and refreshes the cache so changes appear at the next tap without restarting the process.
- **`BootReceiver`** retriggers the preload on `BOOT_COMPLETED` to warm DataStore page caches.
- **Activity** launches with a transparent theme and `FLAG_BLUR_BEHIND` to render the search sheet directly over the launcher, no app transition.

Net result on a Pixel 9: cold start (process killed) is visually instant; post-boot is near-instant; warm restart is instant.

## Build

```bash
./gradlew assembleDebug         # debug APK
./gradlew installDebug          # build + install on a connected device
./gradlew assembleRelease       # signed release APK (needs keystore.properties)
./gradlew lint                  # Android lint
```

Note: debug-build cold start performance is not representative due to missing R8 / AOT optimizations. Always measure on release.

## Setup

1. Open in Android Studio (Koala 2026 or newer)
2. Sync Gradle
3. Run on an Android 12+ device
4. Long-press the homescreen → Widgets → PixelishSearch → drag the widget

## License

MIT
