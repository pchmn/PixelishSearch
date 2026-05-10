package com.pchmn.pixelishsearch

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri

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
 * Launch Google Lens. Tries known exported activities of the Google app,
 * then falls back to the lens.google.com web URL.
 */
fun lensIntent(context: Context): Intent? {
    val pm = context.packageManager
    val googlePkg = "com.google.android.googlequicksearchbox"

    val componentCandidates = listOf(
        "com.google.android.apps.search.lens.LensExportedActivity",
        "com.google.android.apps.gsa.lens.LensExportedActivity",
        "com.google.android.apps.gsa.googlequicksearchbox.LensExportedActivity",
    )
    for (cls in componentCandidates) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(googlePkg, cls)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(pm) != null) return intent
    }

    val webLens = Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com/"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (webLens.resolveActivity(pm) != null) return webLens

    return null
}
