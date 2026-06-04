package com.pchmn.pixelishsearch.search

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.core.content.ContextCompat
import androidx.tracing.trace
import com.pchmn.pixelishsearch.PixelishSearchApp
import com.pchmn.pixelishsearch.core.ui.theme.PixelishTheme
import com.pchmn.pixelishsearch.search.apps.data.AppIndex
import com.pchmn.pixelishsearch.search.settings.data.SettingsPageIndex
import com.pchmn.pixelishsearch.search.shortcuts.data.ShortcutIndex
import com.pchmn.pixelishsearch.search.ui.SearchScreen
import com.pchmn.pixelishsearch.search.ui.SearchViewModel
import com.pchmn.pixelishsearch.update.data.UpdateChecker

class MainActivity : ComponentActivity() {

    private val vm: SearchViewModel by viewModels()

    /**
     * Catches state changes for tiles whose underlying Settings UI doesn't
     * pause us (notably the Wi-Fi Internet Panel, which overlays without
     * dispatching `onPause`/`onResume`). Active only while we're started.
     */
    private val tileStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            vm.refreshTileStates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) = trace("MainActivity.onCreate") {
        super.onCreate(savedInstanceState)

        // Edge-to-edge with fully transparent status / nav bars (scrims = TRANSPARENT).
        // Light/dark pairing (icon color) is still handled automatically by auto()
        // based on the system theme.
        trace("MainActivity.enableEdgeToEdge") {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.Companion.auto(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT
                ),
                navigationBarStyle = SystemBarStyle.Companion.auto(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT
                )
            )
        }

        // "Pixel Search" effect: blur the wallpaper behind via the system
        // `backgroundBlurRadius` API instead of `FLAG_BLUR_BEHIND`. The latter
        // is a *system* effect on surfaces below us — it visibly drops off
        // the moment another window is composed on top during a launch
        // transition (Google search, an app, …) and the user perceives a
        // freeze where the wallpaper snaps sharp behind our still-drawn UI.
        // `setBackgroundBlurRadius` attaches the blurred surface to our own
        // window, so it stays composed until the target activity actually
        // takes over. The dim is rendered as a Compose layer below for the
        // same reason — `FLAG_DIM_BEHIND` has the same fade-off problem.
        trace("MainActivity.windowFlags") {
            window.setBackgroundBlurRadius(80)
        }

        trace("MainActivity.setContent") {
            setContent {
                PixelishTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ComposeColor.Black.copy(alpha = 0.35f)),
                    ) {
                        SearchScreen(
                            viewModel = vm,
                            onClose = { finish() }
                        )
                    }
                }
            }
        }

        // Only on first creation: on activity recreation the singleton indexes
        // are already populated in this process, so re-running the expensive
        // phase-B refresh (which re-loads every app's resources) is wasted work.
        if (savedInstanceState == null) preloadDeferredIndexesAfterFirstFrame()
        triggerUpdateCheck()
    }

    /**
     * Kick off the **phase-B refreshes** that re-resolve *other* packages'
     * labels / resources ([AppIndex], [SettingsPageIndex], [ShortcutIndex]) and
     * rewrite their disk caches — but only once the first content frame is
     * actually on screen. Dispatched from `Application.onCreate` this
     * cross-package `getResources` runs concurrently with the first-frame
     * composition, contending on ART locks and inflating TTID (see
     * `docs/performance-analysis.md`, ADR-0009). The first frame is already
     * served by the eager phase-A cache hydrates (typed results *and* the
     * blank-state recents block), so the authoritative re-scan can wait —
     * deferring it is free.
     *
     * We trigger off the first real [ViewTreeObserver.OnDrawListener] callback —
     * the frame that *actually draws* the Compose content — not a fixed number
     * of `Choreographer` vsync ticks. The transparent window emits an empty
     * frame first, so a vsync count is off-by-one and lands this work *inside*
     * the heavy composition frame (the original regression). Keying off the
     * real draw is robust to however many empty frames precede the content.
     *
     * From inside that first draw we [android.view.View.post] the work so it
     * runs *after* the draw frame completes, not at its start; the listener is
     * removed in the same post (it can't be removed mid-draw). The work runs on
     * `appScope` so it outlives this Activity. The caller gates this on
     * `savedInstanceState == null`, so recreation doesn't re-run the refreshes.
     */
    private fun preloadDeferredIndexesAfterFirstFrame() {
        val decor = window.decorView
        decor.viewTreeObserver.addOnDrawListener(object : ViewTreeObserver.OnDrawListener {
            private var fired = false
            override fun onDraw() {
                if (fired) return
                fired = true
                decor.post {
                    decor.viewTreeObserver.removeOnDrawListener(this)
                    val app = application as PixelishSearchApp
                    AppIndex.refresh(app, app.backgroundScope)
                    SettingsPageIndex.refresh(app.backgroundScope, app)
                    ShortcutIndex.refresh(app, app.backgroundScope)
                }
            }
        })
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
        triggerUpdateCheck()
    }

    /**
     * Tapping a quick-toggle tile typically opens the matching Settings
     * screen, which pauses us. When the user comes back we re-snapshot every
     * tile's on/off state so the chips reflect what they just changed.
     */
    override fun onResume() {
        super.onResume()
        vm.refreshTileStates()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this,
            tileStateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(tileStateReceiver)
    }

    /**
     * Fire-and-forget GitHub release check. Because the activity is
     * `singleTask + excludeFromRecents`, the user can't kill it from the
     * recents screen — the process can live for days, so we can't rely on
     * `Application.onCreate` to re-run the check. `UpdateChecker` throttles
     * internally, so calling this every time the search opens is cheap.
     */
    private fun triggerUpdateCheck() {
        val app = application as PixelishSearchApp
        UpdateChecker.check(app.backgroundScope, app.updates, app.currentVersionName())
    }
}