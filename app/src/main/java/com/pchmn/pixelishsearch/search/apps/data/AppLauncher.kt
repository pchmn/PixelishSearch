package com.pchmn.pixelishsearch.search.apps.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.provider.Settings
import androidx.core.graphics.drawable.toBitmap
import com.pchmn.pixelishsearch.core.data.launchAndDismiss

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

fun launchAppInfo(context: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.launchAndDismiss(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

/**
 * Asks the current launcher to pin a shortcut that re-launches [entry]'s app.
 * The launcher displays its own confirmation dialog — we don't get a callback
 * unless we pass a PendingIntent, which we don't need.
 */
fun pinAppShortcut(context: Context, entry: AppEntry) {
    val sm = context.getSystemService(ShortcutManager::class.java) ?: return
    if (!sm.isRequestPinShortcutSupported) return

    val icon = runCatching {
        val drawable = context.packageManager.getApplicationIcon(entry.packageName)
        Icon.createWithBitmap(drawable.toBitmap())
    }.getOrNull()

    val builder = ShortcutInfo.Builder(context, "pin-${entry.packageName}")
        .setShortLabel(entry.label)
        .setIntent(entry.launchIntent)
    if (icon != null) builder.setIcon(icon)

    runCatching { sm.requestPinShortcut(builder.build(), null) }
}