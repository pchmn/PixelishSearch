package com.pchmn.pixelishsearch.search.settings.data

import android.Manifest
import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.pchmn.pixelishsearch.core.data.launchAndDismiss

/**
 * Launch a quick-toggle tile. Strategy:
 * - Tiles with a direct API (flashlight) toggle in-process.
 * - Tiles guarded by a special permission (WRITE_SECURE_SETTINGS for
 *   airplane / night light / dark theme, WRITE_SETTINGS for auto-rotate)
 *   toggle if the permission has been granted, otherwise fall back to
 *   opening the matching Settings panel.
 * - Tiles with no public toggle API (wifi, bluetooth, hotspot, cast)
 *   always open the matching Settings panel.
 *
 * In all cases the host Activity is dismissed via [launchAndDismiss].
 */
fun launchSettingsTile(context: Context, tile: SettingsTileId) {
    val handled = when (tile) {
        SettingsTileId.AIRPLANE_MODE -> toggleAirplaneMode(context)
        SettingsTileId.NIGHT_LIGHT -> toggleNightLight(context)
        SettingsTileId.DARK_THEME -> toggleDarkTheme(context)
        SettingsTileId.AUTO_ROTATE -> toggleAutoRotate(context)
        SettingsTileId.FLASHLIGHT -> FlashlightController.toggle(context)
        else -> false
    }
    if (!handled) openSettingsFor(context, tile)
}

private fun openSettingsFor(context: Context, tile: SettingsTileId) {
    val action = when (tile) {
        SettingsTileId.WIFI -> Settings.Panel.ACTION_INTERNET_CONNECTIVITY
        SettingsTileId.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
        SettingsTileId.AIRPLANE_MODE -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
        SettingsTileId.NIGHT_LIGHT -> Settings.ACTION_NIGHT_DISPLAY_SETTINGS
        SettingsTileId.DARK_THEME -> Settings.ACTION_DISPLAY_SETTINGS
        SettingsTileId.AUTO_ROTATE -> Settings.ACTION_DISPLAY_SETTINGS
        SettingsTileId.HOTSPOT -> Settings.ACTION_WIRELESS_SETTINGS
        SettingsTileId.FLASHLIGHT -> return
        SettingsTileId.CAST -> Settings.ACTION_CAST_SETTINGS
    }
    val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.launchAndDismiss(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

private fun hasWriteSecureSettings(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
        PackageManager.PERMISSION_GRANTED

private fun toggleAirplaneMode(context: Context): Boolean {
    if (!hasWriteSecureSettings(context)) return false
    return try {
        val cr = context.contentResolver
        val current = Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0)
        val next = if (current == 0) 1 else 0
        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, next)
        context.sendBroadcast(
            Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).putExtra("state", next == 1)
        )
        true
    } catch (_: SecurityException) {
        false
    }
}

private fun toggleNightLight(context: Context): Boolean {
    if (!hasWriteSecureSettings(context)) return false
    return try {
        val cr = context.contentResolver
        val current = Settings.Secure.getInt(cr, NIGHT_DISPLAY_ACTIVATED, 0)
        Settings.Secure.putInt(cr, NIGHT_DISPLAY_ACTIVATED, if (current == 0) 1 else 0)
        true
    } catch (_: SecurityException) {
        false
    }
}

private fun toggleDarkTheme(context: Context): Boolean {
    val uiMode = context.getSystemService(UiModeManager::class.java) ?: return false
    return try {
        val next = if (uiMode.nightMode == UiModeManager.MODE_NIGHT_YES) {
            UiModeManager.MODE_NIGHT_NO
        } else {
            UiModeManager.MODE_NIGHT_YES
        }
        uiMode.setNightMode(next)
        true
    } catch (_: SecurityException) {
        false
    }
}

private fun toggleAutoRotate(context: Context): Boolean {
    if (!Settings.System.canWrite(context)) return false
    return try {
        val cr = context.contentResolver
        val current = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0)
        Settings.System.putInt(
            cr,
            Settings.System.ACCELEROMETER_ROTATION,
            if (current == 0) 1 else 0,
        )
        true
    } catch (_: SecurityException) {
        false
    }
}

/**
 * Hidden but stable since Android 7 (Nougat). Documented in AOSP under
 * `Settings.Secure.NIGHT_DISPLAY_ACTIVATED` (no public constant).
 */
internal const val NIGHT_DISPLAY_ACTIVATED = "night_display_activated"
