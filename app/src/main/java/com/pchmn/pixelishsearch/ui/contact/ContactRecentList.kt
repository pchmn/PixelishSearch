package com.pchmn.pixelishsearch.ui.contact

import androidx.compose.runtime.Composable
import com.pchmn.pixelishsearch.data.ContactHistoryEntry
import com.pchmn.pixelishsearch.ui.EntryList

@Composable
fun ContactRecentList(
    contacts: List<ContactHistoryEntry>,
    onClick: (ContactHistoryEntry) -> Unit,
    onDelete: (ContactHistoryEntry) -> Unit,
) {
    EntryList(entries = contacts) {
        contacts.forEachIndexed { index, contact ->
            ContactRecentRow(
                contact = contact,
                isFirst = index == 0,
                isLast = index == contacts.lastIndex,
                onClick = { onClick(contact) },
                onDelete = { onDelete(contact) },
            )
        }
    }
}