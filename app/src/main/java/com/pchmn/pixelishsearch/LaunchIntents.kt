package com.pchmn.pixelishsearch

import android.app.Activity
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.ContactsContract
import androidx.core.net.toUri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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

fun launchContactDetails(context: Context, contactId: Long) {
    val uri = ContentUris.withAppendedId(
        ContactsContract.Contacts.CONTENT_URI,
        contactId,
    )
    val intent = Intent(Intent.ACTION_VIEW, uri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.launchAndDismiss(intent)
    } catch (_: ActivityNotFoundException) {
        // No contacts app available — silently ignore.
    }
}

fun launchSms(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_SENDTO, "smsto:$phoneNumber".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.launchAndDismiss(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

fun launchDialer(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL, "tel:$phoneNumber".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.launchAndDismiss(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

fun launchGoogleSearch(context: Context, query: String) {
    val googleApp = Intent(Intent.ACTION_WEB_SEARCH).apply {
        putExtra(SearchManager.QUERY, query)
        setPackage("com.google.android.googlequicksearchbox")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.launchAndDismiss(googleApp)
        return
    } catch (_: ActivityNotFoundException) {
        // Google app unavailable, fall back to the browser.
    }

    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
    val fallback = Intent(Intent.ACTION_VIEW, "https://www.google.com/search?q=$encoded".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.launchAndDismiss(fallback)
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
