package com.pchmn.pixelishsearch.ui.contact

import android.content.Context
import com.pchmn.pixelishsearch.data.ContactAction
import com.pchmn.pixelishsearch.data.ContactHistoryEntry
import com.pchmn.pixelishsearch.launchContactDetails
import com.pchmn.pixelishsearch.launchDialer
import com.pchmn.pixelishsearch.launchSms

fun replayContactAction(context: Context, entry: ContactHistoryEntry) {
    val phone = entry.phoneNumber
    when (entry.action) {
        ContactAction.MESSAGE -> if (phone != null) launchSms(
            context,
            phone
        ) else launchContactDetails(context, entry.id)

        ContactAction.CALL -> if (phone != null) launchDialer(
            context,
            phone
        ) else launchContactDetails(context, entry.id)

        ContactAction.CARD -> launchContactDetails(context, entry.id)
    }
}