package com.pchmn.pixelishsearch.search.web.data

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.pchmn.pixelishsearch.core.data.launchAndDismiss
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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