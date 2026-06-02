package com.pchmn.pixelishsearch.search.apps.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pchmn.pixelishsearch.search.shortcuts.data.ShortcutIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Reacts to package install / uninstall / update events by refreshing the
 * AppIndex cache and the static-shortcut index. Keeps both in sync without
 * forcing the user to wait for the next cold start.
 */
class PackageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                val appContext = context.applicationContext
                val scope = CoroutineScope(Dispatchers.Default)
                AppIndex.refresh(appContext, scope)
                ShortcutIndex.refresh(appContext, scope)
            }
        }
    }
}
