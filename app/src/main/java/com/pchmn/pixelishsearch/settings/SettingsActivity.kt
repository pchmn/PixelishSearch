package com.pchmn.pixelishsearch.settings

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.pchmn.pixelishsearch.core.ui.theme.PixelishTheme
import com.pchmn.pixelishsearch.settings.ui.SettingsScreen
import com.pchmn.pixelishsearch.settings.ui.TilesScreen

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.Companion.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.Companion.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            PixelishTheme {
                var route by rememberSaveable { mutableStateOf(Route.Root) }
                when (route) {
                    Route.Root -> SettingsScreen(
                        onBack = { finish() },
                        onOpenTiles = { route = Route.Tiles },
                    )

                    Route.Tiles -> TilesScreen(onBack = { route = Route.Root })
                }
            }
        }
    }

    private enum class Route { Root, Tiles }
}
