package com.pchmn.pixelishsearch.search.calendar.utils

import android.content.Context
import android.text.format.DateFormat
import com.pchmn.pixelishsearch.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Build the second-line "when" label for a calendar event:
 *
 * - today / tomorrow render as the relative word, anything else as a localized
 *   weekday + day + month (year appended only when it differs from the current
 *   one, so a January event seen in December isn't ambiguous);
 * - all-day events show the date alone, timed events append a localized short
 *   time.
 *
 * All-day `begin` values are stored at UTC midnight, so they're interpreted in
 * UTC to land on the intended calendar day; timed events use the device zone.
 */
fun formatEventWhen(context: Context, begin: Long, allDay: Boolean): String {
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    // All-day events store BEGIN at UTC midnight, so the event's calendar day
    // is read in UTC; timed events use the device zone. "Today" is always the
    // user's *local* day either way, so an all-day event near the date boundary
    // still labels correctly.
    val zone = if (allDay) ZoneOffset.UTC else ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(begin).atZone(zone)
    val eventDate = dateTime.toLocalDate()
    val today = LocalDate.now(ZoneId.systemDefault())

    val datePart = when (eventDate) {
        today -> context.getString(R.string.calendar_date_today)
        today.plusDays(1) -> context.getString(R.string.calendar_date_tomorrow)
        else -> {
            val skeleton = if (eventDate.year == today.year) "EEEdMMM" else "EEEdMMMy"
            val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
            eventDate.format(DateTimeFormatter.ofPattern(pattern, locale))
        }
    }

    if (allDay) return datePart

    val time = dateTime.toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    return "$datePart, $time"
}
