package com.pchmn.pixelishsearch.search.calendar.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class CalendarEventEntry(
    val id: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
)

/**
 * Calendar event search. Like [ContactRepository][com.pchmn.pixelishsearch.search.contacts.data.ContactRepository],
 * there's no in-memory index — the calendar ContentProvider is queried per
 * keystroke. Only *upcoming* occurrences are returned and a recurring series
 * collapses to its next occurrence; see `docs/adr/0007`.
 */
object CalendarRepository {

    // Upper bound of the lookahead window. Instances must be queried over a
    // bounded range, and a launcher search is forward-looking.
    private const val WINDOW_MILLIS = 365L * 24 * 60 * 60 * 1000

    // Defensive cap on cursor rows walked before giving up. A daily event over
    // a full year is 365 instances; this keeps a very frequent recurrence from
    // making us scan an unbounded number of rows to fill `limit` distinct hits.
    private const val MAX_ROWS_SCANNED = 500

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Fires a trivial query against the Calendar provider so its (separate)
     * process is alive and the binder connection is warm before the user types
     * their first character. Costs nothing if the provider is already up.
     * Call from Application.onCreate().
     */
    fun warmUp(context: Context, scope: CoroutineScope) {
        if (!hasPermission(context)) return
        scope.launch(Dispatchers.IO) {
            runCatching {
                val now = System.currentTimeMillis()
                val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                    .appendPath(now.toString())
                    .appendPath((now + 1).toString())
                    .build()
                context.contentResolver.query(
                    uri,
                    arrayOf(CalendarContract.Instances.EVENT_ID),
                    null,
                    null,
                    null,
                )?.use { /* drain */ }
            }
        }
    }

    fun search(
        context: Context,
        query: String,
        limit: Int = 3,
    ): List<CalendarEventEntry> {
        if (query.isBlank() || !hasPermission(context)) return emptyList()

        val now = System.currentTimeMillis()

        // Instances.CONTENT_URI takes the [begin, end] range as path segments;
        // the provider expands recurring events into concrete dated occurrences
        // within that range.
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(now.toString())
            .appendPath((now + WINDOW_MILLIS).toString())
            .build()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
        )

        // Title match only, restricted to calendars the user keeps visible.
        val selection =
            "${CalendarContract.Instances.TITLE} LIKE ? AND ${CalendarContract.Calendars.VISIBLE} = 1"
        val selectionArgs = arrayOf("%$query%")
        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        val results = mutableListOf<CalendarEventEntry>()
        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
                ?.use { cursor ->
                    val seen = mutableSetOf<Long>()
                    var scanned = 0
                    while (cursor.moveToNext()) {
                        if (scanned++ >= MAX_ROWS_SCANNED) break

                        val eventId = cursor.getLong(0)
                        // Cursor is BEGIN-ascending, so the first row for an
                        // event id is its next occurrence; collapse the rest.
                        if (eventId in seen) continue
                        val title = cursor.getString(1) ?: continue
                        seen += eventId

                        results += CalendarEventEntry(
                            id = eventId,
                            title = title,
                            begin = cursor.getLong(2),
                            end = cursor.getLong(3),
                            allDay = cursor.getInt(4) == 1,
                        )
                        if (results.size >= limit) break
                    }
                }
        } catch (e: SecurityException) {
            // Permission revoked during the session
            return emptyList()
        }

        return results
    }
}
