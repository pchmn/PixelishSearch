# PixelishSearch

Minimalist Android app that aims to stay as close as possible to the native Pixel Launcher unified
search UI (pre-November 2025 Feature Drop). Inspired by PixelSearch.

## Features

- Search across installed apps, contacts (with permission), web (Google Suggest) and recent queries
- Default apps ranked by usage frequency + recency (14-day half-life decay)
- Recent contact actions (call, SMS, view) and search history with manual removal
- Homescreen widget + transparent search activity with blur-behind over the launcher
- Material 3 Expressive, Dynamic Color, auto dark/light, Google Sans

## Installation

Grab the latest APK from the [Releases](https://github.com/pchmn/PixelishSearch/releases) page and
install it.

To replace the native search bar in the Pixel Launcher, run:

```bash
adb shell settings put secure selected_search_engine com.pchmn.pixelishsearch
adb shell am force-stop com.google.android.apps.nexuslauncher
```

### Advanced

A few quick-toggle tiles depend on protected settings. By default, tapping them opens the matching
Settings panel. You can enable in-place toggling by granting two permissions via `adb`:

```bash
adb shell pm grant com.pchmn.pixelishsearch android.permission.WRITE_SECURE_SETTINGS
adb shell pm grant com.pchmn.pixelishsearch android.permission.WRITE_SETTINGS
```

- `WRITE_SECURE_SETTINGS` enables in-place toggle for **Airplane mode**, **Night Light**,
  **Dark theme** and **Location**.
- `WRITE_SETTINGS` enables in-place toggle for **Auto-rotate**.

Grants persist across reboots and are revoked on uninstall. **Wi-Fi**, **Bluetooth**, **Hotspot**
and **Cast** always open the matching Settings panel — no public API lets a third-party app
toggle them in process.

## Development

### Stack

- Kotlin + Jetpack Compose + Material 3 Expressive (`1.4.0-alpha15`)
- Coroutines + Flow (hot `StateFlow`s for repos)
- DataStore Preferences (usage stats, history, persisted app index)
- Coil 3 (custom `PackageManager` fetcher) + Ktor (Google Suggest)
- Min SDK 31 / Target SDK 37 / JVM 17

### Architecture

The code is organised **by feature**, with two shared packages: `core/` for *technical* primitives (
theme, generic Compose components, intent helper, history base class) and `common/` for *domain*
primitives shared across features. Each feature owns its own `data/` and `ui/` subpackages.

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

### Performance

Cold start of the search activity is the main goal:

- **`PixelishSearchApp` (Application)** preloads everything async at process creation:
    - `AppIndex.preload` (2-phase: hydrate from persisted cache → re-enumerate)
    - `WebSuggestionsRepository.warmUp` warms the TLS handshake to Google Suggest
    - `ContactRepository.warmUp` wakes the (out-of-process) Contacts provider
    - History repos start collecting DataStore eagerly via hot `StateFlow`s
- **Persisted app index** (`AppIndexCacheRepository`) — the launcher list is cached in DataStore. On
  any subsequent cold start, the `AppRow` populates from disk in ~5-10ms without any PackageManager
  call.
- **Coil-cached icons** — `AppIconFetcher` resolves icons via PackageManager once; subsequent loads
  hit Coil's disk cache (PNG). Cache key includes `lastUpdateTime` so updates invalidate
  automatically.
- **`PackageReceiver`** listens for installs / uninstalls / updates and refreshes the cache so
  changes appear at the next tap without restarting the process.
- **`BootReceiver`** retriggers the preload on `BOOT_COMPLETED` to warm DataStore page caches.
- **Activity** launches with a transparent theme and `FLAG_BLUR_BEHIND` to render the search sheet
  directly over the launcher, no app transition. `ModalBottomSheet` starts at `SheetValue.Expanded`
  and a baseline profile is packaged into release builds.

Net result on a Pixel 9 release build: ~120-135ms `TotalTime` (`adb shell am start -W`), ~150-170ms
to a fully stable screen. See [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md) for the cold-start path,
baseline profile / macrobenchmark workflow, and Perfetto profiling.

### Commands

```bash
./gradlew assembleDebug         # debug APK
./gradlew installDebug          # build + install on a connected device
./gradlew assembleRelease       # signed release APK (needs keystore.properties)
./gradlew installRelease        # build + install release on a connected device
./gradlew installBenchmarkRelease  # release variant with `.benchmark` applicationIdSuffix —
                                   # daily-driver build for measuring cold start (coexists
                                   # with the production install on the device)
./gradlew lint                  # Android lint
```

For the baseline profile and startup-benchmark workflow, see [
`docs/PERFORMANCE.md`](docs/PERFORMANCE.md).

### Setup

1. Open in Android Studio (Koala 2026 or newer)
2. Sync Gradle
3. Run on an Android 12+ device
4. Long-press the homescreen → Widgets → PixelishSearch → drag the widget

## License

MIT
