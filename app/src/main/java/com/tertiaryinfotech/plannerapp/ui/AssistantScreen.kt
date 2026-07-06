package com.tertiaryinfotech.plannerapp.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.plannerapp.PlannerViewModel
import com.tertiaryinfotech.plannerapp.data.PlannerKind
import com.tertiaryinfotech.plannerapp.logic.IntentAssistant
import com.tertiaryinfotech.plannerapp.speech.SpeechRecognizerManager
import java.util.UUID

private enum class Role { USER, ASSISTANT }

private data class ItemSummary(
    val id: String,
    val title: String,
    val kind: PlannerKind,
    val date: Long?,
)

private data class ChatMessage(
    val role: Role,
    val text: String,
    val item: ItemSummary? = null,
    val id: String = UUID.randomUUID().toString(),
)

private val GREETING = ChatMessage(
    role = Role.ASSISTANT,
    text = "Hi! Tell me what you need to do — type it or tap the mic. " +
        "Try “Dentist appointment Friday 3pm” or “Buy groceries”.",
)

/**
 * Chat-style capture — the app's front door. Tell the assistant what you need, by text or
 * voice, and it drafts a nicely worded to-do or appointment and saves it instantly (with undo).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: PlannerViewModel) {
    val context = LocalContext.current
    val messages = remember { listOf(GREETING).toMutableStateList() }
    var input by remember { mutableStateOf("") }

    // Created only on first mic tap so no microphone prompt appears at launch.
    var speech by remember { mutableStateOf<SpeechRecognizerManager?>(null) }
    val isListening = speech?.isListening == true

    fun send() {
        val text = input.trim()
        if (text.isEmpty()) return
        speech?.stop()
        input = ""
        messages += ChatMessage(Role.USER, text)
        val draft = IntentAssistant.draft(text)
        var summary: ItemSummary? = null
        if (draft.entry.title.isNotEmpty()) {
            val item = draft.entry.makeItem()
            viewModel.add(item)
            summary = ItemSummary(item.id, item.title, item.kind, item.date)
        }
        messages += ChatMessage(Role.ASSISTANT, draft.reply, summary)
    }

    // Live transcript fills the input; when dictation ends with text, send automatically.
    LaunchedEffect(speech?.transcript) {
        speech?.transcript?.takeIf { it.isNotBlank() }?.let { input = it }
    }
    LaunchedEffect(isListening) {
        if (!isListening && speech != null && input.isNotBlank()) send()
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val s = speech ?: SpeechRecognizerManager(context).also { speech = it }
            s.toggle()
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Assistant") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatBubble(message) { itemId, title ->
                        viewModel.deleteById(itemId)
                        messages += ChatMessage(Role.ASSISTANT, "Removed “$title”.")
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = {
                        Text(if (isListening) "Listening…" else "e.g. “Lunch with Sam tomorrow 1pm”")
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                )
                FilledIconButton(
                    onClick = { micPermission.launch(Manifest.permission.RECORD_AUDIO) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isListening) MaterialTheme.colorScheme.error
                                         else MaterialTheme.colorScheme.primary
                    ),
                ) {
                    Icon(
                        if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isListening) "Stop dictation" else "Dictate",
                        tint = Color.White,
                    )
                }
                FilledIconButton(
                    onClick = { send() },
                    enabled = input.trim().isNotEmpty(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, onUndo: (String, String) -> Unit) {
    val isUser = message.role == Role.USER
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (isUser) Spacer(Modifier.weight(1f, fill = true))
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(18.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                message.text,
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
            message.item?.let { item ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                            RoundedCornerShape(12.dp),
                        )
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            item.kind.iconVector,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            item.kind.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(item.title, style = MaterialTheme.typography.bodyMedium)
                    item.date?.let { ClockLabel(it) }
                    TextButton(
                        onClick = { onUndo(item.id, item.title) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) { Text("Undo", style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
        if (!isUser) Spacer(Modifier.weight(1f, fill = true))
    }
}
