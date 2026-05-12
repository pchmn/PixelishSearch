package com.pchmn.pixelishsearch.data

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class ContactEntry(
    val id: Long,
    val name: String,
    val phoneNumber: String?,
    val photoUri: Uri?,
    val starred: Boolean = false,
)

/**
 * Contact search. No in-memory index here because contacts can be numerous
 * and the ContentResolver is already very fast for short queries.
 */
object ContactRepository {

    // Hard cap on rows pulled from the ContentResolver before in-memory ranking.
    // We need to over-fetch (vs. the user-visible `limit`) because the SQL sort
    // is alphabetical — we re-rank by starred + usage score in Kotlin.
    private const val MAX_CANDIDATES = 50

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun search(
        context: Context,
        query: String,
        limit: Int = 4,
        scoreOf: (Long) -> Float = { 0f },
    ): List<ContactEntry> {
        if (query.isBlank() || !hasPermission(context)) return emptyList()

        val candidates = mutableListOf<ContactEntry>()

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
            ContactsContract.Contacts.STARRED,
        )

        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        val sortOrder =
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT $MAX_CANDIDATES"

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
                ?.use { cursor ->
                    val seen = mutableSetOf<Long>()
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        if (id in seen) continue
                        seen += id

                        val name = cursor.getString(1) ?: continue
                        val number = cursor.getString(2)
                        val photo = cursor.getString(3)?.let { Uri.parse(it) }
                        val starred = cursor.getInt(4) == 1

                        candidates += ContactEntry(id, name, number, photo, starred)
                    }
                }
        } catch (e: SecurityException) {
            // Permission revoked during the session
            return emptyList()
        }

        val ranker = compareByDescending<ContactEntry> { it.starred }
            .thenByDescending { scoreOf(it.id) }
            .thenBy { it.name.lowercase() }

        return candidates.sortedWith(ranker).take(limit)
    }
}
