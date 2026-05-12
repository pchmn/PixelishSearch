package com.pchmn.pixelishsearch.ui.contact

import androidx.compose.runtime.Composable
import com.pchmn.pixelishsearch.data.ContactEntry
import com.pchmn.pixelishsearch.ui.EntryList

@Composable
fun ResultContactList(
    contacts: List<ContactEntry>,
    onContactClick: (ContactEntry) -> Unit,
    onMessageClick: (ContactEntry) -> Unit,
    onCallClick: (ContactEntry) -> Unit,
) {
    EntryList(entries = contacts) {
        contacts.forEachIndexed { index, contact ->
            ResultContactItem(
                contact = contact,
                isFirst = index == 0,
                isLast = index == contacts.lastIndex,
                onClick = { onContactClick(contact) },
                onMessageClick = { onMessageClick(contact) },
                onCallClick = { onCallClick(contact) },
            )
        }
    }
}