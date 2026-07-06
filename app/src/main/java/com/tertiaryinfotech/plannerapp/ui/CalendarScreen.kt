package com.tertiaryinfotech.plannerapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.plannerapp.PlannerViewModel
import com.tertiaryinfotech.plannerapp.data.PlannerItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Built-in calendar — a month grid with a dot on days that have appointments, plus the list
 * of appointments for the selected day and the next few upcoming ones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: PlannerViewModel) {
    val items by viewModel.activeItems.collectAsState()
    val appointments = items.filter { it.isAppointment && it.date != null }

    var selectedDay by remember { mutableLongStateOf(startOfDay(System.currentTimeMillis())) }
    var monthAnchor by remember { mutableLongStateOf(startOfDay(System.currentTimeMillis())) }
    var editingItem by remember { mutableStateOf<PlannerItem?>(null) }

    val daysWithAppointments = appointments.mapNotNull { it.date?.let(::startOfDay) }.toSet()
    val onSelectedDay = appointments
        .filter { startOfDay(it.date!!) == selectedDay }
        .sortedBy { it.date }
    val now = System.currentTimeMillis()
    val upcoming = appointments
        .filter { it.date!! >= now && startOfDay(it.date) != selectedDay }
        .sortedBy { it.date }
        .take(5)

    Scaffold(topBar = { TopAppBar(title = { Text("Calendar") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                MonthGrid(
                    monthAnchor = monthAnchor,
                    selectedDay = selectedDay,
                    markedDays = daysWithAppointments,
                    onPrevMonth = { monthAnchor = addMonths(monthAnchor, -1) },
                    onNextMonth = { monthAnchor = addMonths(monthAnchor, 1) },
                    onSelect = { selectedDay = it },
                )
            }

            item { SectionHeader(formatDayHeading(selectedDay)) }
            if (onSelectedDay.isEmpty()) {
                item {
                    Text(
                        "No appointments on this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            } else {
                items(onSelectedDay, key = { it.id }) { item ->
                    ItemRow(item, onToggle = { viewModel.toggleDone(item) },
                        onEdit = { editingItem = item })
                }
            }

            if (upcoming.isNotEmpty()) {
                item { SectionHeader("Upcoming") }
                items(upcoming, key = { "u-${it.id}" }) { item ->
                    ItemRow(item, onToggle = { viewModel.toggleDone(item) },
                        onEdit = { editingItem = item })
                }
            }
        }
    }

    editingItem?.let { item ->
        AddItemSheet(
            itemToEdit = item,
            onSave = { viewModel.update(it); editingItem = null },
            onDismiss = { editingItem = null },
        )
    }
}

@Composable
private fun MonthGrid(
    monthAnchor: Long,
    selectedDay: Long,
    markedDays: Set<Long>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    val cal = Calendar.getInstance().apply {
        timeInMillis = monthAnchor
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(cal.timeInMillis))
    val firstWeekday = cal.get(Calendar.DAY_OF_WEEK)   // 1 = Sunday
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = startOfDay(System.currentTimeMillis())

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                monthTitle,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onNextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }

        Row(Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        d,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val cells = firstWeekday - 1 + daysInMonth
        val rows = (cells + 6) / 7
        var day = 1
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    if (cellIndex < firstWeekday - 1 || day > daysInMonth) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val dayCal = (cal.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, day)
                        }
                        val dayMillis = startOfDay(dayCal.timeInMillis)
                        val isSelected = dayMillis == selectedDay
                        val isToday = dayMillis == today
                        val d = day
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(3.dp)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        else -> Color.Transparent
                                    },
                                    CircleShape,
                                )
                                .clickable { onSelect(dayMillis) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$d",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                if (dayMillis in markedDays) {
                                    Box(
                                        Modifier
                                            .size(5.dp)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.primary,
                                                CircleShape,
                                            )
                                    )
                                } else {
                                    Spacer(Modifier.height(5.dp))
                                }
                            }
                        }
                        day++
                    }
                }
            }
        }
    }
}

fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun addMonths(millis: Long, delta: Int): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.DAY_OF_MONTH, 1)
    add(Calendar.MONTH, delta)
}.timeInMillis
