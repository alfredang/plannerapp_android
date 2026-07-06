package com.tertiaryinfotech.plannerapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.plannerapp.PlannerViewModel
import com.tertiaryinfotech.plannerapp.data.PlannerItem
import com.tertiaryinfotech.plannerapp.data.PlannerKind

/**
 * The main planner screen: every active (non-archived) to-do and appointment, grouped by
 * kind, with a manual add button and an "Add by Voice" action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(viewModel: PlannerViewModel) {
    val items by viewModel.activeItems.collectAsState()
    var showingAdd by remember { mutableStateOf(false) }
    var showingVoice by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<PlannerItem?>(null) }

    val tasks = items.filter { it.kind == PlannerKind.TASK }
    val appointments = items.filter { it.kind == PlannerKind.APPOINTMENT }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planner") },
                actions = {
                    IconButton(onClick = { showingAdd = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add item")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showingVoice = true },
                icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                text = { Text("Add by Voice") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Text("Nothing planned yet", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap + to add a to-do or appointment, or use the mic to add one by voice.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                if (appointments.isNotEmpty()) {
                    item { SectionHeader("Appointments") }
                    items(appointments, key = { it.id }) { item ->
                        PlannerRow(item, viewModel) { editingItem = item }
                    }
                }
                if (tasks.isNotEmpty()) {
                    item { SectionHeader("To-Do") }
                    items(tasks, key = { it.id }) { item ->
                        PlannerRow(item, viewModel) { editingItem = item }
                    }
                }
            }
        }
    }

    if (showingAdd) {
        AddItemSheet(
            onSave = { viewModel.add(it); showingAdd = false },
            onDismiss = { showingAdd = false },
        )
    }
    editingItem?.let { item ->
        AddItemSheet(
            itemToEdit = item,
            onSave = { viewModel.update(it); editingItem = null },
            onDismiss = { editingItem = null },
        )
    }
    if (showingVoice) {
        VoiceCaptureSheet(
            onSave = { viewModel.add(it); showingVoice = false },
            onDismiss = { showingVoice = false },
        )
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun PlannerRow(item: PlannerItem, viewModel: PlannerViewModel, onEdit: () -> Unit) {
    Box {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                ItemRow(item, onToggle = { viewModel.toggleDone(item) }, onEdit = onEdit)
            }
            IconButton(onClick = { viewModel.delete(item) }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
