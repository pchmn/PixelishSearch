package com.pchmn.pixelishsearch.search.settings.data

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.util.Log

/**
 * Snapshot of whether a tile is currently "on". Read synchronously when the
 * search builds its result list — the search Activity is short-lived, so a
 * snapshot is enough.
 *
 * Tiles with no public read API (hotspot, cast) return false.
 */
internal fun SettingsTileId.isActive(context: Context): Boolean {
    Log.i(
        "BLUETOOTH",
        Settings.Global.getInt(context.contentResolver, Settings.Global.BLUETOOTH_ON, 0).toString()
    )
    val app = context.applicationContext
    return when (this) {
        SettingsTileId.WIFI -> globalSettingsActive(app, Settings.Global.WIFI_ON)
        SettingsTileId.BLUETOOTH -> globalSettingsActive(app, Settings.Global.BLUETOOTH_ON)
        SettingsTileId.AIRPLANE_MODE -> globalSettingsActive(app, Settings.Global.AIRPLANE_MODE_ON)
        SettingsTileId.NIGHT_LIGHT -> secureSettingsActive(app, NIGHT_DISPLAY_ACTIVATED)
        SettingsTileId.DARK_THEME -> darkThemeActive(app)
        SettingsTileId.AUTO_ROTATE -> systemSettingsActive(
            app,
            Settings.System.ACCELEROMETER_ROTATION
        )

        SettingsTileId.FLASHLIGHT -> FlashlightController.isOn
        SettingsTileId.HOTSPOT -> false
        SettingsTileId.CAST -> false
    }
}

private fun globalSettingsActive(context: Context, name: String): Boolean =
    Settings.Global.getInt(context.contentResolver, name, 0) == 1

private fun systemSettingsActive(context: Context, name: String): Boolean =
    Settings.System.getInt(context.contentResolver, name, 0) == 1

private fun secureSettingsActive(context: Context, name: String): Boolean =
    Settings.Secure.getInt(context.contentResolver, name, 0) == 1

//interface AndroidSettings {
//    fun getInt(cr: ContentResolver?, name: String?, def: Int): Int
//}
//
//private fun <AndroidSettings> isSettingsActive(context: Context, name: String): Boolean =
//    AndroidSettings.getInt(context.contentResolver, name, 0) == 1

private fun darkThemeActive(context: Context): Boolean {
    val uiMode = context.getSystemService(UiModeManager::class.java)
    if (uiMode != null && uiMode.nightMode != UiModeManager.MODE_NIGHT_AUTO) {
        return uiMode.nightMode == UiModeManager.MODE_NIGHT_YES
    }
    val mask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return mask == Configuration.UI_MODE_NIGHT_YES
}
