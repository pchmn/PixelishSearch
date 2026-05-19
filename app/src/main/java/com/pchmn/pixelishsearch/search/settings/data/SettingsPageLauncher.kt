package com.pchmn.pixelishsearch.search.settings.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.pchmn.pixelishsearch.core.data.launchAndDismiss

/**
 * Open a system Settings page by intent action. The action is one of the
 * `Settings.ACTION_*` constants kept in [settingsPages]; resolvability was
 * already checked at preload time by [SettingsPageIndex].
 */
fun launchSettingsPage(context: Context, action: String) {
    val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.launchAndDismiss(intent)
    } catch (_: ActivityNotFoundException) {
    }
}
