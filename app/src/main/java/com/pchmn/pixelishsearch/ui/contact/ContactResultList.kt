package com.pchmn.pixelishsearch.ui.contact

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.pchmn.pixelishsearch.data.ContactEntry
import com.pchmn.pixelishsearch.ui.EntryList

@Composable
fun ContactResultList(
    contacts: List<ContactEntry>,
    onContactClick: (ContactEntry) -> Unit,
    onMessageClick: (ContactEntry) -> Unit,
    onCallClick: (ContactEntry) -> Unit,
) {
    val context = LocalContext.current
    val smsIcon = remember(context) {
        cachedSmsIcon ?: resolveAppIcon(context, Intent(Intent.ACTION_SENDTO, "smsto:".toUri()))
            .also { cachedSmsIcon = it }
    }
    val callIcon = remember(context) {
        cachedCallIcon ?: resolveAppIcon(context, Intent(Intent.ACTION_DIAL, "tel:".toUri()))
            .also { cachedCallIcon = it }
    }

    EntryList(entries = contacts) {
        contacts.forEachIndexed { index, contact ->
            ContactResultRow(
                contact = contact,
                isFirst = index == 0,
                isLast = index == contacts.lastIndex,
                smsIcon = smsIcon,
                callIcon = callIcon,
                onClick = { onContactClick(contact) },
                onMessageClick = { onMessageClick(contact) },
                onCallClick = { onCallClick(contact) },
            )
        }
    }
}

private var cachedSmsIcon: ImageBitmap? = null
private var cachedCallIcon: ImageBitmap? = null

private fun resolveAppIcon(context: Context, intent: Intent): ImageBitmap? {
    val pm = context.packageManager
    val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return null
    return runCatching {
        resolveInfo.loadIcon(pm).toBitmap().asImageBitmap()
    }.getOrNull()
}
