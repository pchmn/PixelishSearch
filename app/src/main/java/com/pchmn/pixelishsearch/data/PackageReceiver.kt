package com.pchmn.pixelishsearch.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Reacts to package install / uninstall / update events by refreshing the
 * AppIndex cache. Keeps the launcher list in sync without forcing the user
 * to wait for the next cold start.
 */
class PackageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                AppIndex.refresh(context.applicationContext, CoroutineScope(Dispatchers.Default))
            }
        }
    }
}
