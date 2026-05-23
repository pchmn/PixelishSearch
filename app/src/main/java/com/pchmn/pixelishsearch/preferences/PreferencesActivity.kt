package com.pchmn.pixelishsearch.preferences

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import com.pchmn.pixelishsearch.core.ui.theme.PixelishTheme
import com.pchmn.pixelishsearch.preferences.ui.PreferencesScreen
import com.pchmn.pixelishsearch.preferences.ui.TilesScreen

class PreferencesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.Companion.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.Companion.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            PixelishTheme {
                var route by rememberSaveable { mutableStateOf(Route.Root) }
                AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        val spatial = tween<IntOffset>(durationMillis = 350, easing = FastOutSlowInEasing)
                        val effects = tween<Float>(durationMillis = 250, easing = FastOutSlowInEasing)
                        val enterOffset: (Int) -> Int = { if (forward) it / 3 else -it / 3 }
                        val exitOffset: (Int) -> Int = { if (forward) -it / 3 else it / 3 }
                        (slideInHorizontally(spatial, enterOffset) + fadeIn(effects)) togetherWith
                            (slideOutHorizontally(spatial, exitOffset) + fadeOut(effects))
                    },
                    label = "preferencesRoute",
                ) { current ->
                    when (current) {
                        Route.Root -> PreferencesScreen(
                            onBack = { finish() },
                            onOpenTiles = { route = Route.Tiles },
                        )

                        Route.Tiles -> TilesScreen(onBack = { route = Route.Root })
                    }
                }
            }
        }
    }

    private enum class Route { Root, Tiles }
}
