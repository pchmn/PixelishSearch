package com.pchmn.pixelishsearch.search.calendar.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.pchmn.pixelishsearch.core.ui.components.EntryList
import com.pchmn.pixelishsearch.search.apps.data.AppIconRequest
import com.pchmn.pixelishsearch.search.calendar.data.CalendarEventEntry

@Composable
fun CalendarResultList(
    events: List<CalendarEventEntry>,
    onEventClick: (CalendarEventEntry) -> Unit,
) {
    val context = LocalContext.current
    val iconRequest = remember(context) {
        cachedCalendarIcon ?: resolveCalendarIcon(context).also { cachedCalendarIcon = it }
    }

    EntryList(entries = events) {
        events.forEachIndexed { index, event ->
            CalendarResultRow(
                event = event,
                iconRequest = iconRequest,
                isFirst = index == 0,
                isLast = index == events.lastIndex,
                onClick = { onEventClick(event) },
            )
        }
    }
}

// The calendar app's icon is identical on every row, so resolve it once and
// keep it for the rest of the process.
private var cachedCalendarIcon: AppIconRequest? = null

/**
 * Resolve the user's calendar app via `CATEGORY_APP_CALENDAR` — the category
 * Android defines for exactly this — and read its `lastUpdateTime` so Coil
 * invalidates the cached icon after an app update. Null when no calendar app
 * is installed, in which case the row falls back to a Material glyph.
 */
private fun resolveCalendarIcon(context: Context): AppIconRequest? {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
    val pkg = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo?.packageName ?: return null
    val lastUpdate = try {
        pm.getPackageInfo(pkg, 0).lastUpdateTime
    } catch (_: PackageManager.NameNotFoundException) {
        return null
    }
    return AppIconRequest(pkg, lastUpdate)
}
