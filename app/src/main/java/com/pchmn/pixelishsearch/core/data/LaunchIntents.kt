package com.pchmn.pixelishsearch.core.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent

/**
 * Start [intent] and instantly tear down the host Activity with no exit
 * animation, so the perceived launch latency matches what the native Pixel
 * Launcher delivers (no bottom-sheet slide-down, no transparent-activity
 * close animation stacking on top of the target's open animation).
 */
fun Context.launchAndDismiss(intent: Intent) {
    startActivity(intent)
    /*    val activity = findActivity() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // overrideActivityTransition must be called BEFORE finish().
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
            // activity.finish()
        } else {
            // overridePendingTransition must be called AFTER finish().
            // activity.finish()
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(0, 0)
        }*/
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
