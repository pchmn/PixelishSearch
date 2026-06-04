package com.pchmn.pixelishsearch.search.apps.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Preloads the index as soon as the phone boots, so the first launch
 * of the search app is instant.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Skip on the `.benchmark` build only. Macrobenchmark's StartupMode.COLD
        // re-delivers BOOT_COMPLETED to the freshly-started process on *every*
        // iteration, so this refresh would run phase-B `enumerate` during the
        // first frame and contaminate the measured TTID — work a real
        // tap-to-search cold start never does (BOOT_COMPLETED fires once, at real
        // boot, in the background). The benchmark applicationId is suffixed
        // `.benchmark`; production keeps the refresh. See docs/performance-analysis.md.
        if (context.packageName.endsWith(".benchmark")) return

        // Authoritative re-enumeration (phase B) — there's no first frame to
        // protect on boot, and we want the freshest list cached for the next
        // launch. The cold-start path's cheap cache-hydrate (phase A) is
        // pointless here since nothing is displayed.
        AppIndex.refresh(context.applicationContext, CoroutineScope(Dispatchers.Default))
    }
}