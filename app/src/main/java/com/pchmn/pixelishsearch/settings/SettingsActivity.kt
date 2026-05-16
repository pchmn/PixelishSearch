package com.pchmn.pixelishsearch.settings

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pchmn.pixelishsearch.settings.ui.SettingsScreen
import com.pchmn.pixelishsearch.core.ui.theme.PixelishTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.Companion.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.Companion.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            PixelishTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}