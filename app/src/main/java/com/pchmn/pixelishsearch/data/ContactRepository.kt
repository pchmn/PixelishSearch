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
    val photoUri: Uri?
)

/**
 * Contact search. No in-memory index here because contacts can be numerous
 * and the ContentResolver is already very fast for short queries.
 */
object ContactRepository {

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun search(context: Context, query: String, limit: Int = 4): List<ContactEntry> {
        if (query.isBlank() || !hasPermission(context)) return emptyList()

        val results = mutableListOf<ContactEntry>()

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT $limit"

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val seen = mutableSetOf<Long>()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    if (id in seen) continue
                    seen += id

                    val name = cursor.getString(1) ?: continue
                    val number = cursor.getString(2)
                    val photo = cursor.getString(3)?.let { Uri.parse(it) }

                    results += ContactEntry(id, name, number, photo)
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
