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
}

data class SettingsTile(
    val id: SettingsTileId,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    val keywords: List<String>,
)

/**
 * A tile + a snapshot of its current on/off state, as returned by
 * [SettingsTileRepository.search]. The snapshot is read once at query time.
 */
data class SettingsTileResult(
    val tile: SettingsTile,
    val isActive: Boolean,
)

val SettingsTiles: List<SettingsTile> = listOf(
    SettingsTile(
        SettingsTileId.WIFI, R.string.tile_wifi, R.drawable.ic_wifi,
        listOf("wifi", "wi-fi", "internet", "network", "wlan", "reseau"),
    ),
    SettingsTile(
        SettingsTileId.BLUETOOTH, R.string.tile_bluetooth, R.drawable.ic_bluetooth,
        listOf("bluetooth", "bt"),
    ),
    SettingsTile(
        SettingsTileId.AIRPLANE_MODE, R.string.tile_airplane_mode, R.drawable.ic_airplanemode_active,
        listOf("airplane", "flight", "plane", "avion", "flugmodus", "aereo"),
    ),
    SettingsTile(
        SettingsTileId.NIGHT_LIGHT, R.string.tile_night_light, R.drawable.ic_nightlight,
        listOf("night", "light", "warm", "nuit", "nocturne", "notturna"),
    ),
    SettingsTile(
        SettingsTileId.DARK_THEME, R.string.tile_dark_theme, R.drawable.ic_dark_mode,
        listOf("dark", "theme", "sombre", "oscuro", "dunkles", "scuro"),
    ),
    SettingsTile(
        SettingsTileId.AUTO_ROTATE, R.string.tile_auto_rotate, R.drawable.ic_screen_rotation,
        listOf("rotate", "rotation", "auto", "screen", "drehung"),
    ),
    SettingsTile(
        SettingsTileId.HOTSPOT, R.string.tile_hotspot, R.drawable.ic_wifi_tethering,
        listOf("hotspot", "wifi", "tethering", "partage", "punto", "acceso"),
    ),
    SettingsTile(
        SettingsTileId.FLASHLIGHT, R.string.tile_flashlight, R.drawable.ic_flashlight_on,
        listOf("flashlight", "torch", "lampe", "torcia", "linterna", "taschenlampe"),
    ),
    SettingsTile(
        SettingsTileId.CAST, R.string.tile_cast, R.drawable.ic_cast,
        listOf("cast", "screen", "caster", "trasmetti", "streamen", "enviar"),
    ),
)
