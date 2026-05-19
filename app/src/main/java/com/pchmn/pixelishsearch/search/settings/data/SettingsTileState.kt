package com.pchmn.pixelishsearch.search.settings.data

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.location.LocationManager
import android.provider.Settings

/**
 * Snapshot of whether a tile is currently "on". Read synchronously when the
 * search builds its result list — the search Activity is short-lived, so a
 * snapshot is enough for tiles that dismiss it (all but flashlight). The
 * flashlight tile is patched live by the VM via [FlashlightController.isOn].
 *
 * Tiles with no public read API (hotspot, cast) return false.
 */
internal fun SettingsTileId.isActive(context: Context): Boolean {
    val app = context.applicationContext
    return when (this) {
        SettingsTileId.WIFI -> isSettingActive(app, SettingsScope.GLOBAL, Settings.Global.WIFI_ON)
        SettingsTileId.BLUETOOTH -> isSettingActive(
            app,
            SettingsScope.GLOBAL,
            Settings.Global.BLUETOOTH_ON
        )

        SettingsTileId.AIRPLANE_MODE -> isSettingActive(
            app,
            SettingsScope.GLOBAL,
            Settings.Global.AIRPLANE_MODE_ON
        )

        SettingsTileId.NIGHT_LIGHT -> isSettingActive(
            app,
            SettingsScope.SECURE,
            NIGHT_DISPLAY_ACTIVATED
        )

        SettingsTileId.AUTO_ROTATE -> isSettingActive(
            app,
            SettingsScope.SYSTEM,
            Settings.System.ACCELEROMETER_ROTATION
        )

        SettingsTileId.DARK_THEME -> darkThemeActive(app)

        SettingsTileId.FLASHLIGHT -> FlashlightController.isOn.value
        SettingsTileId.LOCATION -> isLocationEnabled(app)
        SettingsTileId.HOTSPOT -> false
        SettingsTileId.CAST -> false
    }
}

private fun isLocationEnabled(context: Context): Boolean {
    val lm = context.getSystemService(LocationManager::class.java) ?: return false
    return lm.isLocationEnabled
}

enum class SettingsScope { GLOBAL, SYSTEM, SECURE }

private fun isSettingActive(context: Context, scope: SettingsScope, name: String): Boolean {
    val resolver = context.contentResolver
    val value = when (scope) {
        SettingsScope.GLOBAL -> Settings.Global.getInt(resolver, name, 0)
        SettingsScope.SYSTEM -> Settings.System.getInt(resolver, name, 0)
        SettingsScope.SECURE -> Settings.Secure.getInt(resolver, name, 0)
    }
    return value == 1
}

private fun darkThemeActive(context: Context): Boolean {
    val uiMode = context.getSystemService(UiModeManager::class.java)
    if (uiMode != null && uiMode.nightMode != UiModeManager.MODE_NIGHT_AUTO) {
        return uiMode.nightMode == UiModeManager.MODE_NIGHT_YES
    }
    val mask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return mask == Configuration.UI_MODE_NIGHT_YES
}
