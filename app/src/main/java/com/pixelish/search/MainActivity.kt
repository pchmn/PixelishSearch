package com.pixelish.search

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelish.search.ui.SearchScreen
import com.pixelish.search.ui.SearchViewModel
import com.pixelish.search.ui.theme.PixelishTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
