package com.pchmn.pixelishsearch.search.settings.data

import android.provider.Settings

/**
 * One entry in the curated list of system Settings pages addressable by intent.
 * Labels are *not* hardcoded here — they're resolved at preload time from the
 * Settings APK via `ResolveInfo.loadLabel`, so they follow the device locale.
 * `keywords` are English fallbacks used for prefix matching alongside the
 * localized label.
 */
data class SettingsPage(
    val action: String,
    val keywords: List<String> = emptyList(),
)

/**
 * A resolved [SettingsPage]: kept in [SettingsPageIndex] after `preload` filters
 * out actions that don't resolve on the current device. The icon (Settings app
 * launcher icon) is shared by all entries and lives on [SettingsPageIndex].
 */
data class SettingsPageEntry(
    val action: String,
    val label: String,
    val keywords: List<String>,
)

/**
 * Curated list of public `Settings.ACTION_*` intents that don't require extras
 * or a data URI. Order doesn't matter — results are ranked by match quality
 * later.
 */
val settingsPages: List<SettingsPage> = listOf(
    SettingsPage(Settings.ACTION_WIFI_SETTINGS, listOf("wifi", "wlan", "internet", "network")),
    SettingsPage(Settings.ACTION_BLUETOOTH_SETTINGS, listOf("bluetooth", "bt")),
    SettingsPage(Settings.ACTION_AIRPLANE_MODE_SETTINGS, listOf("airplane", "flight", "plane")),
    SettingsPage(Settings.ACTION_WIRELESS_SETTINGS, listOf("wireless", "network")),
    SettingsPage(Settings.ACTION_DATA_USAGE_SETTINGS, listOf("data", "usage", "mobile", "cellular")),
    SettingsPage(Settings.ACTION_DATA_ROAMING_SETTINGS, listOf("roaming")),
    SettingsPage(Settings.ACTION_NFC_SETTINGS, listOf("nfc")),
    SettingsPage(Settings.ACTION_VPN_SETTINGS, listOf("vpn")),
    SettingsPage(Settings.ACTION_DISPLAY_SETTINGS, listOf("display", "screen", "brightness")),
    SettingsPage(Settings.ACTION_NIGHT_DISPLAY_SETTINGS, listOf("night", "bedtime", "blue", "eye")),
    SettingsPage(Settings.ACTION_DREAM_SETTINGS, listOf("screensaver", "dream")),
    SettingsPage(Settings.ACTION_SOUND_SETTINGS, listOf("sound", "volume", "ring", "audio")),
    SettingsPage(Settings.ACTION_BATTERY_SAVER_SETTINGS, listOf("battery", "saver", "power")),
    SettingsPage(Settings.ACTION_APPLICATION_SETTINGS, listOf("apps", "applications")),
    SettingsPage(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, listOf("apps", "installed", "uninstall")),
    SettingsPage(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, listOf("default", "browser")),
    SettingsPage(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, listOf("notification", "listener")),
    SettingsPage(Settings.ACTION_USAGE_ACCESS_SETTINGS, listOf("usage", "access")),
    SettingsPage(Settings.ACTION_HOME_SETTINGS, listOf("home", "launcher", "default")),
    SettingsPage(Settings.ACTION_LOCATION_SOURCE_SETTINGS, listOf("location", "gps")),
    SettingsPage(Settings.ACTION_SECURITY_SETTINGS, listOf("security", "lock")),
    SettingsPage(Settings.ACTION_PRIVACY_SETTINGS, listOf("privacy")),
    SettingsPage(Settings.ACTION_BIOMETRIC_ENROLL, listOf("fingerprint", "biometric", "face", "unlock")),
    SettingsPage(Settings.ACTION_ACCESSIBILITY_SETTINGS, listOf("accessibility")),
    SettingsPage(Settings.ACTION_CAPTIONING_SETTINGS, listOf("caption", "subtitle")),
    SettingsPage(Settings.ACTION_LOCALE_SETTINGS, listOf("language", "locale")),
    SettingsPage(Settings.ACTION_DATE_SETTINGS, listOf("date", "time", "clock")),
    SettingsPage(Settings.ACTION_INPUT_METHOD_SETTINGS, listOf("keyboard", "input")),
    SettingsPage(Settings.ACTION_DEVICE_INFO_SETTINGS, listOf("about", "phone", "info", "model", "version")),
    SettingsPage(Settings.ACTION_INTERNAL_STORAGE_SETTINGS, listOf("storage", "memory", "space")),
    SettingsPage(Settings.ACTION_CAST_SETTINGS, listOf("cast")),
    SettingsPage(Settings.ACTION_PRINT_SETTINGS, listOf("print")),
    SettingsPage(Settings.ACTION_SYNC_SETTINGS, listOf("sync", "account")),
    SettingsPage(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, listOf("notification", "dnd", "disturb", "zen", "focus")),
)
