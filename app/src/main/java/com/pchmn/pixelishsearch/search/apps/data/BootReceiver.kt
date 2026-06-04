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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Authoritative re-enumeration (phase B) — there's no first frame to
            // protect on boot, and we want the freshest list cached for the next
            // launch. The cold-start path's cheap cache-hydrate (phase A) is
            // pointless here since nothing is displayed.
            AppIndex.refresh(context.applicationContext, CoroutineScope(Dispatchers.Default))
        }
    }
}