package com.tertiaryinfotech.plannerapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.plannerapp.data.PlannerItem
import com.tertiaryinfotech.plannerapp.data.PlannerKind
import com.tertiaryinfotech.plannerapp.logic.ParsedEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Manual add/edit form for a to-do or appointment — the Android counterpart of the iOS
 * `AddItemView`. Pass [itemToEdit] to edit in place, or [prefill] (from voice parsing) to
 * pre-populate a new entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(
    itemToEdit: PlannerItem? = null,
    prefill: ParsedEntry? = null,
    onSave: (PlannerItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialKind = itemToEdit?.kind ?: prefill?.kind ?: PlannerKind.TASK
    val initialDate = itemToEdit?.date ?: prefill?.date

    var title by remember { mutableStateOf(itemToEdit?.title ?: prefill?.title ?: "") }
    var notes by remember { mutableStateOf(itemToEdit?.notes ?: "") }
    var kind by remember { mutableStateOf(initialKind) }
    var includeDate by remember { mutableStateOf(initialDate != null || initialKind == PlannerKind.APPOINTMENT) }
    var dateMillis by remember { mutableStateOf(initialDate ?: defaultDate()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val canSave = title.trim().isNotEmpty()
    val isEditing = itemToEdit != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                when {
                    isEditing && kind == PlannerKind.TASK -> "Edit To-Do"
                    isEditing -> "Edit Appointment"
                    kind == PlannerKind.TASK -> "New To-Do"
                    else -> "New Appointment"
                },
                style = MaterialTheme.typography.titleLarge,
            )

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PlannerKind.entries.forEachIndexed { i, k ->
                    SegmentedButton(
                        selected = kind == k,
                        onClick = {
                            kind = k
                            // Appointments need a time; default it on when switching.
                            if (k == PlannerKind.APPOINTMENT && !includeDate) includeDate = true
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, PlannerKind.entries.size),
                    ) { Text(k.title) }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Set date & time", Modifier.weight(1f))
                Switch(checked = includeDate, onCheckedChange = { includeDate = it })
            }

            if (includeDate) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { showDatePicker = true }, Modifier.weight(1f)) {
                        Text(SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(dateMillis)))
                    }
                    OutlinedButton(onClick = { showTimePicker = true }) {
                        Text(formatTime(dateMillis))
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onDismiss, Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        val item = (itemToEdit ?: PlannerItem()).copy(
                            title = title.trim(),
                            notes = notes.trim(),
                            kindRaw = kind.raw,
                            date = if (includeDate) dateMillis else null,
                        )
                        onSave(item)
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked ->
                        dateMillis = mergeDate(dateMillis, picked)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val state = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    cal.set(Calendar.HOUR_OF_DAY, state.hour)
                    cal.set(Calendar.MINUTE, state.minute)
                    dateMillis = cal.timeInMillis
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = state) },
        )
    }
}

/** Next top of the hour. */
private fun defaultDate(): Long = Calendar.getInstance().apply {
    set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    add(Calendar.HOUR_OF_DAY, 1)
}.timeInMillis

/** Keep the time-of-day from [current], take the calendar day from the UTC [pickedUtcMillis]. */
private fun mergeDate(current: Long, pickedUtcMillis: Long): Long {
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        .apply { timeInMillis = pickedUtcMillis }
    return Calendar.getInstance().apply {
        timeInMillis = current
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
