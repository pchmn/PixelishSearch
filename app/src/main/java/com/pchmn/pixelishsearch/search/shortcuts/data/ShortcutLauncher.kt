package com.pchmn.pixelishsearch.search.shortcuts.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.pchmn.pixelishsearch.core.data.launch

/**
 * Fire a shortcut's parsed [intent] ourselves — we can't use
 * `LauncherApps.startShortcut` without the launcher role. The index already
 * filtered to launchable shortcuts, but a target activity could have been
 * disabled / un-exported since, so we swallow the launch failures defensively.
 */
fun launchShortcut(context: Context, intent: Intent) {
    try {
        context.launch(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    } catch (_: SecurityException) {
    }
}
