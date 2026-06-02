package com.pchmn.pixelishsearch.search.calendar.data

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.pchmn.pixelishsearch.core.data.launch

/**
 * Open a single occurrence in the calendar app. The begin/end extras make a
 * recurring instance open on its own date rather than the series' first one.
 */
fun launchCalendarEvent(context: Context, eventId: Long, begin: Long, end: Long) {
    val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
    val intent = Intent(Intent.ACTION_VIEW, uri)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.launch(intent)
    } catch (_: ActivityNotFoundException) {
        // No calendar app available — silently ignore.
    }
}
