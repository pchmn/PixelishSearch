package com.pchmn.pixelishsearch.search.settings.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.pchmn.pixelishsearch.R

enum class SettingsTileId {
    WIFI,
    BLUETOOTH,
    AIRPLANE_MODE,
    NIGHT_LIGHT,
    DARK_THEME,
    AUTO_ROTATE,
    HOTSPOT,
    FLASHLIGHT,
    CAST,
    LOCATION,
}

data class SettingsTile(
    val id: SettingsTileId,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    val keywords: List<String>,
    @param:DrawableRes val inactiveIconRes: Int? = null,
)

/**
 * A tile + a snapshot of its current on/off state, as returned by
 * [SettingsTileRepository.search]. The snapshot is read once at query time.
 */
data class SettingsTileResult(
    val tile: SettingsTile,
    val isActive: Boolean,
)

val settingsTiles: List<SettingsTile> = listOf(
    SettingsTile(
        SettingsTileId.WIFI, R.string.tile_wifi, R.drawable.ic_wifi,
        listOf("wifi", "wlan", "internet"),
    ),
    SettingsTile(
        SettingsTileId.BLUETOOTH, R.string.tile_bluetooth, R.drawable.ic_bluetooth,
        listOf("bt"),
    ),
    SettingsTile(
        SettingsTileId.AIRPLANE_MODE,
        R.string.tile_airplane_mode,
        R.drawable.ic_airplanemode_active,
        listOf("flight"),
    ),
    SettingsTile(
        SettingsTileId.NIGHT_LIGHT, R.string.tile_night_light, R.drawable.ic_filled_nightlight,
        emptyList(),
        inactiveIconRes = R.drawable.ic_nightlight
    ),
    SettingsTile(
        SettingsTileId.DARK_THEME, R.string.tile_dark_theme, R.drawable.ic_contrast,
        emptyList(),
    ),
    SettingsTile(
        SettingsTileId.AUTO_ROTATE, R.string.tile_auto_rotate, R.drawable.ic_screen_rotation,
        emptyList(),
        inactiveIconRes = R.drawable.ic_screen_rotation_up
    ),
    SettingsTile(
        SettingsTileId.HOTSPOT, R.string.tile_hotspot, R.drawable.ic_wifi_tethering,
        listOf("tethering"),
    ),
    SettingsTile(
        SettingsTileId.FLASHLIGHT, R.string.tile_flashlight, R.drawable.ic_flashlight_on,
        listOf("torch"),
    ),
    SettingsTile(
        SettingsTileId.CAST, R.string.tile_cast, R.drawable.ic_cast,
        emptyList(),
    ),
    SettingsTile(
        SettingsTileId.LOCATION, R.string.tile_location, R.drawable.ic_location_on,
        listOf("gps"),
        inactiveIconRes = R.drawable.ic_location_off,
    ),
)
