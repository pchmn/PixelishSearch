# PixelishSearch

Minimalist Android app that replicates the Pixel Launcher unified search UI (pre-November 2025 Feature Drop), lighter and faster than PixelSearch.

## Features

- Installed apps search (local, instant)
- Default app suggestions ranked by usage frequency + recency (14-day half-life decay)
- Search history (recent queries)
- Contact suggestions (with permission) + recent contacts
- Web suggestions via Google Suggest API
- Homescreen widget that opens the search
- Transparent search activity with blur-behind effect over the launcher
- Material 3 Expressive + Dynamic Color (matches the system theme)
- Automatic dark / light mode
- Google Sans font

## Stack

- Kotlin
- Jetpack Compose + Material 3 (`1.4.0-alpha15` for Expressive)
- Coroutines + Flow
- DataStore Preferences (usage stats, history)
- Ktor (Google Suggest client)
- Min SDK 31 (Android 12, for Dynamic Color)
- Target SDK 35
- JVM target 17

## Architecture

```
app/src/main/
├── java/com/pchmn/pixelishsearch/
│   ├── MainActivity.kt              # Transparent search activity
│   ├── PixelishApp.kt               # Application class, preloads index + repos
│   ├── data/
│   │   ├── AppIndex.kt              # In-memory app index, fuzzy search
│   │   ├── AppUsageRepository.kt    # Per-package launch stats with decay scoring
│   │   ├── SearchHistoryRepository.kt
│   │   ├── ContactRepository.kt     # Live contact search via ContentResolver
│   │   ├── ContactHistoryRepository.kt
│   │   ├── WebSuggestRepository.kt  # Google Suggest API (Ktor)
│   │   └── BootReceiver.kt          # Re-preloads on BOOT_COMPLETED
│   ├── ui/
│   │   ├── SearchScreen.kt          # ModalBottomSheet with input + results
│   │   ├── SearchViewModel.kt       # Orchestrates local + web search
│   │   └── theme/Theme.kt           # Material 3 + Dynamic Color + Google Sans
│   └── widget/
│       └── SearchWidget.kt          # AppWidgetProvider
└── res/
    ├── layout/widget_search_bar.xml
    ├── xml/widget_info.xml
    └── drawable/, anim/, values/
```

## Performance

Cold start of the search activity is the main goal. The app index and DataStore-backed repositories are preloaded:

- At process creation in `PixelishApp` (Application class)
- On `BOOT_COMPLETED` via `BootReceiver`

The activity launches with a transparent theme and `FLAG_BLUR_BEHIND` to render the search sheet directly over the launcher without a visible app transition.

## Build

```bash
./gradlew assembleDebug         # debug APK
./gradlew installDebug          # build + install on a connected device
./gradlew lint                  # Android lint
```

## Setup

1. Open in Android Studio (Koala 2026 or newer)
2. Sync Gradle
3. Run on an Android 12+ device
4. Long-press the homescreen → Widgets → PixelishSearch → drag the widget

## License

MIT
