package com.pchmn.pixelishsearch

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.pchmn.pixelishsearch.ui.SearchScreen
import com.pchmn.pixelishsearch.ui.SearchViewModel
import com.pchmn.pixelishsearch.ui.theme.PixelishTheme

class MainActivity : ComponentActivity() {

    private val vm: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge with fully transparent status / nav bars (scrims = TRANSPARENT).
        // Light/dark pairing (icon color) is still handled automatically by auto()
        // based on the system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        // "Pixel Search" effect: blur + slight dim of the wallpaper / content behind.
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.35f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply {
                blurBehindRadius = 80
            }
        }

        setContent {
            PixelishTheme {
                SearchScreen(
                    viewModel = vm,
                    onClose = { finish() }
                )
            }
        }
    }

    /**
     * Triggered when the widget re-launches us while the existing instance
     * is still alive (singleTask + we deliberately don't finish() after
     * forwarding to Lens/Gemini/an app). Reset the query so the user lands
     * on a clean search bar instead of their previous results.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.reset()
    }
}
