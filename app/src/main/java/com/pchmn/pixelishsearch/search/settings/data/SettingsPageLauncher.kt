package com.pchmn.pixelishsearch.search.settings.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.pchmn.pixelishsearch.core.data.launch

/**
 * Open a Settings sub-page directly by [ComponentName]. The component was
 * already resolved at preload by [SettingsPageIndex], so no extra existence
 * check is needed beyond catching `ActivityNotFoundException` defensively.
 */
fun launchSettingsPage(context: Context, component: ComponentName) {
    val intent = Intent()
        .setComponent(component)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.launch(intent)
    } catch (_: ActivityNotFoundException) {
    }
}
