package com.pchmn.pixelishsearch.ui.contact

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pchmn.pixelishsearch.data.ContactEntry
import com.pchmn.pixelishsearch.ui.EntryRow

@Composable
fun ContactResultRow(
    contact: ContactEntry,
    isFirst: Boolean,
    isLast: Boolean,
    smsIcon: ImageBitmap?,
    callIcon: ImageBitmap?,
    onClick: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: () -> Unit,
) {
    EntryRow(
        padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        isFirst = isFirst,
        isLast = isLast,
        onClick = onClick,
        leading = {
            ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 48.dp)
        },
    ) {
        Text(
            text = contact.name,
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp),
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (contact.phoneNumber != null) {
            Spacer(modifier = Modifier.width(8.dp))
            ActionIconButton(
                bitmap = smsIcon,
                fallbackIcon = Icons.AutoMirrored.Outlined.Message,
                contentDescription = stringResource(com.pchmn.pixelishsearch.R.string.contact_action_message),
                onClick = onMessageClick,
            )
            Spacer(modifier = Modifier.width(16.dp))
            ActionIconButton(
                bitmap = callIcon,
                fallbackIcon = Icons.Outlined.Phone,
                contentDescription = stringResource(com.pchmn.pixelishsearch.R.string.contact_action_call),
                onClick = onCallClick,
            )
        }
    }
}

@Composable
private fun ActionIconButton(
    bitmap: ImageBitmap?,
    fallbackIcon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val baseModifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .clickable(onClick = onClick)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = baseModifier,
        )
    } else {
        Box(
            modifier = baseModifier
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
