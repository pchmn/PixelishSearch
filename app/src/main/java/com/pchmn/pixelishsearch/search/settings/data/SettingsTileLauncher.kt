package com.pchmn.pixelishsearch.search.settings.data

import android.Manifest
import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
        SettingsTileId.LOCATION -> toggleLocation(context)
        SettingsTileId.FLASHLIGHT -> FlashlightController.toggle(context)
        else -> false
    }
    if (!handled) openSettingsFor(context, tile)
}

private fun openSettingsFor(context: Context, tile: SettingsTileId) {
    val intent = tile.buildSettingsIntent(context) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    tile.highlightKey()?.let(intent::withSettingsHighlight)
    try {
        context.launchAndDismiss(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

/**
 * Prefer a direct sub-screen activity alias when the Settings app exposes
 * one (Pixel / AOSP do — `Settings$DarkThemeSettingsActivity`, etc.); fall
 * back to the canonical [Settings] action otherwise.
 */
private fun SettingsTileId.buildSettingsIntent(context: Context): Intent? {
    directSettingsActivity()?.let { className ->
        val direct = Intent().setClassName(SETTINGS_PKG, className)
        if (context.packageManager.resolveActivity(direct, 0) != null) return direct
    }
    val action = when (this) {
        SettingsTileId.WIFI -> Settings.Panel.ACTION_INTERNET_CONNECTIVITY
        SettingsTileId.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
        SettingsTileId.AIRPLANE_MODE -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
        SettingsTileId.NIGHT_LIGHT -> Settings.ACTION_NIGHT_DISPLAY_SETTINGS
        SettingsTileId.DARK_THEME -> Settings.ACTION_DISPLAY_SETTINGS
        SettingsTileId.AUTO_ROTATE -> Settings.ACTION_DISPLAY_SETTINGS
        SettingsTileId.HOTSPOT -> Settings.ACTION_WIRELESS_SETTINGS
        SettingsTileId.FLASHLIGHT -> return null
        SettingsTileId.CAST -> Settings.ACTION_CAST_SETTINGS
        SettingsTileId.LOCATION -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
    }
    return Intent(action)
}

private const val SETTINGS_PKG = "com.android.settings"

private fun SettingsTileId.directSettingsActivity(): String? = when (this) {
    SettingsTileId.BLUETOOTH -> "com.android.settings.Settings\$BluetoothSettingsActivity"
    SettingsTileId.NIGHT_LIGHT -> "com.android.settings.Settings\$NightDisplaySettingsActivity"
    SettingsTileId.DARK_THEME -> "com.android.settings.Settings\$DarkThemeSettingsActivity"
    SettingsTileId.AUTO_ROTATE -> "com.android.settings.Settings\$SmartAutoRotateSettingsActivity"
    SettingsTileId.HOTSPOT -> "com.android.settings.Settings\$WifiTetherSettingsActivity"
    SettingsTileId.CAST -> "com.android.settings.Settings\$WifiDisplaySettingsActivity"
    SettingsTileId.LOCATION -> "com.android.settings.Settings\$LocationSettingsActivity"
    SettingsTileId.WIFI, SettingsTileId.AIRPLANE_MODE, SettingsTileId.FLASHLIGHT -> null
}

/**
 * Pixel Launcher trick: when present, the target Settings screen scrolls to
 * the preference whose key matches and flashes it briefly. The extras are
 * stable across AOSP / Pixel since Android 6 but are NOT public API — keys
 * vary by vendor / version. Unknown keys are silently ignored, so the screen
 * just opens normally as a fallback.
 */
private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
private const val EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args"

private fun SettingsTileId.highlightKey(): String? = when (this) {
    SettingsTileId.AIRPLANE_MODE -> "airplane_mode_on"
    else -> null
}

private fun Intent.withSettingsHighlight(key: String) {
    putExtra(EXTRA_FRAGMENT_ARG_KEY, key)
    putExtra(EXTRA_SHOW_FRAGMENT_ARGS, Bundle().apply {
        putString(EXTRA_FRAGMENT_ARG_KEY, key)
    })
}

private fun hasWriteSecureSettings(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

private fun toggleAirplaneMode(context: Context): Boolean {
    if (!hasWriteSecureSettings(context)) return false
    return try {
        val cr = context.contentResolver
        val current = Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0)
        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, if (current == 0) 1 else 0)
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

/**
 * `setNightMode` is gated by `MODIFY_DAY_NIGHT_MODE` (signature|privileged) on
 * recent Pixel / AOSP and **no-ops silently** when the caller lacks it — no
 * exception, just nothing happens. Re-read to detect that and fall back to
 * opening the Dark theme settings page.
 */
private fun toggleDarkTheme(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        ?: return false
    val next = if (uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES) {
        UiModeManager.MODE_NIGHT_NO
    } else {
        UiModeManager.MODE_NIGHT_YES
    }
    return try {
        uiModeManager.nightMode = next
        uiModeManager.nightMode == next
    } catch (_: SecurityException) {
        false
    }
}

@Suppress("DEPRECATION")
private fun toggleLocation(context: Context): Boolean {
    if (!hasWriteSecureSettings(context)) return false
    return try {
        val cr = context.contentResolver
        val current = Settings.Secure.getInt(cr, Settings.Secure.LOCATION_MODE, LOCATION_MODE_OFF)
        val next =
            if (current == LOCATION_MODE_OFF) LOCATION_MODE_HIGH_ACCURACY else LOCATION_MODE_OFF
        Settings.Secure.putInt(cr, Settings.Secure.LOCATION_MODE, next)
        true
    } catch (_: SecurityException) {
        false
    }
}

private const val LOCATION_MODE_OFF = 0
private const val LOCATION_MODE_HIGH_ACCURACY = 3

private fun toggleAutoRotate(context: Context): Boolean {
    Log.i("TOGGLE AUTO ROTATE", Settings.System.canWrite(context).toString())
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
