package com.pchmn.pixelishsearch.search.contacts.ui

import androidx.compose.runtime.Composable
import com.pchmn.pixelishsearch.search.contacts.data.ContactHistoryEntry
import com.pchmn.pixelishsearch.core.ui.components.EntryList

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