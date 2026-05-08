# PixelishSearch

App Android minimaliste reproduisant l'UI de la recherche unifiée du Pixel Launcher (avant le Feature Drop de novembre 2025), en plus léger et plus rapide que PixelSearch.

## Fonctionnalités

- Recherche d'apps installées (locale, instantanée)
- Suggestions de contacts (avec permission)
- Suggestions web (Google Suggest API, optionnel)
- Widget homescreen qui ouvre la recherche
- Material 3 Expressive + Dynamic Color (matche le thème système)
- Mode sombre / clair automatique

## Stack

- Kotlin
- Jetpack Compose + Material 3 (alpha pour Expressive)
- Coroutines + Flow
- Min SDK 31 (Android 12, pour Dynamic Color)
- Target SDK 35

## Architecture

```
app/
├── src/main/
│   ├── java/com/pixelish/search/
│   │   ├── MainActivity.kt          # Activity de recherche transparente
│   │   ├── PixelishApp.kt           # Application class, pré-charge l'index
│   │   ├── data/
│   │   │   ├── AppIndex.kt          # Index en mémoire des apps
│   │   │   ├── ContactRepository.kt # Recherche de contacts
│   │   │   └── WebSuggestRepository.kt # Suggestions Google
│   │   ├── ui/
│   │   │   ├── SearchScreen.kt      # Écran principal Compose
│   │   │   ├── SearchViewModel.kt
│   │   │   └── theme/Theme.kt       # Material 3 + Dynamic Color
│   │   └── widget/
│   │       └── SearchWidget.kt      # AppWidgetProvider
│   └── res/
│       ├── values/
│       ├── xml/widget_info.xml      # Config du widget
│       ├── layout/widget_search_bar.xml
│       └── drawable/
└── build.gradle.kts
```

## Setup

1. Ouvrir dans Android Studio (Koala 2026 ou plus récent)
2. Sync Gradle
3. Run sur appareil Android 12+
4. Long-press sur l'écran d'accueil → Widgets → PixelishSearch → glisser le widget

## Performance

L'index des apps est pré-chargé au boot (BOOT_COMPLETED) et au lancement de l'app, gardé en mémoire. Le cold start de l'Activity de recherche cible 100-200ms.

## Licence

MIT
