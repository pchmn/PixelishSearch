package com.pchmn.pixelishsearch.search.web.data

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.pchmn.pixelishsearch.core.data.launchAndDismiss
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun launchGoogleSearch(context: Context, query: String) {
    // Lightweight Google Search entrypoint (same as the Pixel Launcher uses).
    // Much faster to show its first frame than ACTION_WEB_SEARCH, which routes
    // through a heavier activity and causes a perceptible freeze.
    val implicitEntrypoint = Intent("com.google.android.googlequicksearchbox.GOOGLE_SEARCH").apply {
        component = ComponentName(
            "com.google.android.googlequicksearchbox",
            "com.google.android.googlequicksearchbox.ImplicitGoogleSearchEntrypointInternal",
        )
        putExtra(SearchManager.QUERY, query)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }
    try {
        context.launchAndDismiss(implicitEntrypoint)
        return
    } catch (_: ActivityNotFoundException) {
        // Entrypoint missing on this Google app version, try ACTION_WEB_SEARCH.
    }

    val webSearch = Intent(Intent.ACTION_WEB_SEARCH).apply {
        putExtra(SearchManager.QUERY, query)
        setPackage("com.google.android.googlequicksearchbox")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.launchAndDismiss(webSearch)
        return
    } catch (_: ActivityNotFoundException) {
        // Google app unavailable, fall back to the browser.
    }

    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
    val fallback = Intent(Intent.ACTION_VIEW, "https://www.google.com/search?q=$encoded".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.launchAndDismiss(fallback)
}