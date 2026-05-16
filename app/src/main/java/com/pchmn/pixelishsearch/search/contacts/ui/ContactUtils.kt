package com.pchmn.pixelishsearch.search.contacts.ui

import android.content.Context
import com.pchmn.pixelishsearch.launchContactDetails
import com.pchmn.pixelishsearch.launchDialer
import com.pchmn.pixelishsearch.launchSms
import com.pchmn.pixelishsearch.search.contacts.data.ContactAction
import com.pchmn.pixelishsearch.search.contacts.data.ContactHistoryEntry

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