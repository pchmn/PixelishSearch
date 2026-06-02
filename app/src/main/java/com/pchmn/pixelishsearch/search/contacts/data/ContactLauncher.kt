package com.pchmn.pixelishsearch.search.contacts.data

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import androidx.core.net.toUri
import com.pchmn.pixelishsearch.core.data.launch

fun launchContactDetails(context: Context, contactId: Long) {
    val uri = ContentUris.withAppendedId(
        ContactsContract.Contacts.CONTENT_URI,
        contactId,
    )
    val intent = Intent(Intent.ACTION_VIEW, uri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.launch(intent)
    } catch (_: ActivityNotFoundException) {
        // No contacts app available — silently ignore.
    }
}

fun launchSms(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_SENDTO, "smsto:$phoneNumber".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.launch(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

fun launchDialer(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL, "tel:$phoneNumber".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.launch(intent)
    } catch (_: ActivityNotFoundException) {
    }
}