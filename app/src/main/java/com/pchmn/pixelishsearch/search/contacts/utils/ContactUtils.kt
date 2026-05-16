package com.pchmn.pixelishsearch.search.contacts.utils

import android.content.Context
import com.pchmn.pixelishsearch.search.contacts.data.ContactAction
import com.pchmn.pixelishsearch.search.contacts.data.ContactHistoryEntry
import com.pchmn.pixelishsearch.search.contacts.data.launchContactDetails
import com.pchmn.pixelishsearch.search.contacts.data.launchDialer
import com.pchmn.pixelishsearch.search.contacts.data.launchSms

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