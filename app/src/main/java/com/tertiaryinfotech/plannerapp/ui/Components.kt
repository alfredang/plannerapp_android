package com.tertiaryinfotech.plannerapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.plannerapp.data.PlannerItem
import com.tertiaryinfotech.plannerapp.data.PlannerKind
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Shared date formatting, mirroring the iOS row/preview styles.
fun formatFull(millis: Long): String =
    SimpleDateFormat("EEEE, MMM d 'at' h:mm a", Locale.getDefault()).format(Date(millis))

fun formatRow(millis: Long, withTime: Boolean): String =
    SimpleDateFormat(if (withTime) "EEE, MMM d, h:mm a" else "MMM d", Locale.getDefault())
        .format(Date(millis))

fun formatDayHeading(millis: Long): String =
    SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(millis))

fun formatTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))

val PlannerKind.iconVector
    get() = if (this == PlannerKind.TASK) Icons.Filled.Checklist else Icons.Filled.CalendarMonth

/**
 * A single planner row with a tappable check circle. Checking auto-archives the item.
 * Tapping the row content opens the edit form (when [onEdit] is provided).
 */
@Composable
fun ItemRow(
    item: PlannerItem,
    onToggle: () -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onEdit != null) Modifier.clickable { onEdit() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (item.isDone) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = if (item.isDone) "Mark as not done" else "Mark as done",
                tint = if (item.isDone) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f).padding(top = 10.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    item.kind.iconVector,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    buildString {
                        append(item.kind.title)
                        item.date?.let { append("  •  ").append(formatRow(it, item.isAppointment)) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.notes.isNotEmpty()) {
                Text(
                    item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Small labelled clock row used in parsed-entry previews. */
@Composable
fun ClockLabel(millis: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Schedule,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            formatFull(millis),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
