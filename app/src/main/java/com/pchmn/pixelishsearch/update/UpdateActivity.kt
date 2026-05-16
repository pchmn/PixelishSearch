package com.pchmn.pixelishsearch.update

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pchmn.pixelishsearch.core.ui.theme.PixelishTheme
import com.pchmn.pixelishsearch.update.ui.UpdateScreen

class UpdateActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        setContent {
            PixelishTheme {
                UpdateScreen(onBack = { finish() })
            }
        }
    }
}
