package com.pchmn.pixelishsearch.ui.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pchmn.pixelishsearch.data.ContactHistoryEntry
import com.pchmn.pixelishsearch.ui.EntryList

@Composable
fun RecentContactList(
    contacts: List<ContactHistoryEntry>,
    onClick: (ContactHistoryEntry) -> Unit,
    onDelete: (ContactHistoryEntry) -> Unit,
) {
    EntryList(entries = contacts) {
        contacts.forEachIndexed { index, contact ->
            RecentContactItem(
                contact = contact,
                isFirst = index == 0,
                isLast = index == contacts.lastIndex,
                onClick = { onClick(contact) },
                onDelete = { onDelete(contact) },
            )
        }
    }
}