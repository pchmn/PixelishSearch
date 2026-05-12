package com.pchmn.pixelishsearch.ui.contact

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pchmn.pixelishsearch.data.ContactAction
import com.pchmn.pixelishsearch.data.ContactHistoryEntry
import com.pchmn.pixelishsearch.ui.EntryRow

@Composable
fun RecentContactItem(
    contact: ContactHistoryEntry,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    EntryRow(
        isFirst = isFirst,
        isLast = isLast,
        onClick = onClick,
        onDelete = onDelete,
        leading = {
            ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 32.dp)
        },
    ) {
        Text(
            text = contact.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = " · ${contact.action.label()}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun ContactAction.label(): String = when (this) {
    ContactAction.CARD -> "Contacts"
    ContactAction.MESSAGE -> "Message"
    ContactAction.CALL -> "Phone"
}