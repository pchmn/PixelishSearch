package com.pixelish.search

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelish.search.ui.SearchScreen
import com.pixelish.search.ui.SearchViewModel
import com.pixelish.search.ui.theme.PixelishTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge avec icônes status/nav bar toujours en blanc
        // (SystemBarStyle.dark = fond sombre, donc icônes claires).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        // Effet "Pixel Search" : flou + assombrissement léger du wallpaper / contenu derrière.
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
                val vm: SearchViewModel = viewModel()
                SearchScreen(
                    viewModel = vm,
                    onClose = { finish() }
                )
            }
        }
    }
}
