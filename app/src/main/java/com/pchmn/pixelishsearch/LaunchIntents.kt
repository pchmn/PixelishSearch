package com.pchmn.pixelishsearch

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build

/**
 * Open Gemini in voice mode. ACTION_VOICE_COMMAND triggers the default
 * voice assistant — on Pixel devices where Gemini is the assistant, this
 * opens Gemini straight into mic / voice input.
 */
fun geminiIntent(context: Context): Intent? {
    val pm = context.packageManager

    val voice = Intent(Intent.ACTION_VOICE_COMMAND)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (voice.resolveActivity(pm) != null) return voice

    pm.getLaunchIntentForPackage("com.google.android.apps.bard")
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ?.let { return it }

    return null
}

/**
 * Launch Google Lens. The reliable entry point on current Pixel devices is
 * `com.google.android.apps.lens.DirectLensYoutubeActivity` in the Google
 * app, launched with ACTION_MAIN (despite its name, it's the generic Lens
 * launcher used by other Pixel-search alternatives).
 */
fun lensIntent(context: Context): Intent? {
    val pm = context.packageManager
    val googlePkg = "com.google.android.googlequicksearchbox"

    val installed = runCatching { pm.getPackageInfo(googlePkg, 0) }.isSuccess
    if (installed) {
        return Intent(Intent.ACTION_MAIN)
            .setComponent(
                ComponentName(googlePkg, "com.google.android.apps.lens.DirectLensYoutubeActivity")
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    pm.getLaunchIntentForPackage("com.google.ar.lens")
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ?.let { return it }

    return null
}

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
