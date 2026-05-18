package com.pchmn.pixelishsearch.search.settings.data

import android.app.UiModeManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.res.Configuration
import android.net.wifi.WifiManager
import android.provider.Settings

/**
 * Snapshot of whether a tile is currently "on". Read synchronously when the
 * search builds its result list — the search Activity is short-lived, so a
 * snapshot is enough.
 *
 * Tiles with no public read API (hotspot, cast) return false.
 */
internal fun SettingsTileId.isActive(context: Context): Boolean {
    val app = context.applicationContext
    return when (this) {
        SettingsTileId.WIFI -> wifiActive(app)
        SettingsTileId.BLUETOOTH -> bluetoothActive(app)
        SettingsTileId.AIRPLANE_MODE -> airplaneActive(app)
        SettingsTileId.NIGHT_LIGHT -> nightLightActive(app)
        SettingsTileId.DARK_THEME -> darkThemeActive(app)
        SettingsTileId.AUTO_ROTATE -> autoRotateActive(app)
        SettingsTileId.FLASHLIGHT -> FlashlightController.isOn
        SettingsTileId.HOTSPOT -> false
        SettingsTileId.CAST -> false
    }
}

private fun wifiActive(context: Context): Boolean =
    runCatching {
        context.getSystemService(WifiManager::class.java)?.isWifiEnabled == true
    }.getOrDefault(false)

private fun bluetoothActive(context: Context): Boolean =
    runCatching {
        context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
    }.getOrDefault(false)

private fun airplaneActive(context: Context): Boolean =
    Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

private fun nightLightActive(context: Context): Boolean =
    Settings.Secure.getInt(context.contentResolver, NIGHT_DISPLAY_ACTIVATED, 0) == 1

private fun darkThemeActive(context: Context): Boolean {
    val uiMode = context.getSystemService(UiModeManager::class.java)
    if (uiMode != null && uiMode.nightMode != UiModeManager.MODE_NIGHT_AUTO) {
        return uiMode.nightMode == UiModeManager.MODE_NIGHT_YES
    }
    val mask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return mask == Configuration.UI_MODE_NIGHT_YES
}

private fun autoRotateActive(context: Context): Boolean =
    Settings.System.getInt(
        context.contentResolver,
        Settings.System.ACCELEROMETER_ROTATION,
        0,
    ) == 1
