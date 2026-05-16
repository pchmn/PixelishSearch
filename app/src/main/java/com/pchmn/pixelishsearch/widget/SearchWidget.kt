package com.pchmn.pixelishsearch.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.pchmn.pixelishsearch.R
import com.pchmn.pixelishsearch.search.MainActivity
import com.pchmn.pixelishsearch.search.apps.data.geminiIntent
import com.pchmn.pixelishsearch.search.apps.data.lensIntent

/**
 * Homescreen widget that looks like the native Pixel search bar.
 * On tap → opens MainActivity (the search screen).
 * Gemini / Lens icons → launch their respective apps when installed.
 */
class SearchWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_search_bar)
        val main = mainActivityIntent(context)

        views.setOnClickPendingIntent(
            R.id.widget_root,
            activityPendingIntent(context, widgetId * 10, main)
        )
        views.setOnClickPendingIntent(
            R.id.widget_gemini,
            activityPendingIntent(context, widgetId * 10 + 1, geminiIntent(context) ?: main)
        )
        views.setOnClickPendingIntent(
            R.id.widget_lens,
            activityPendingIntent(context, widgetId * 10 + 2, lensIntent(context) ?: main)
        )

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun activityPendingIntent(
        context: Context,
        requestCode: Int,
        intent: Intent,
    ): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun mainActivityIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
}
