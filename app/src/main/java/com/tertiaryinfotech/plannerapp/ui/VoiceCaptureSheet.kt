package com.tertiaryinfotech.plannerapp.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.plannerapp.data.PlannerItem
import com.tertiaryinfotech.plannerapp.logic.ParsedEntry
import com.tertiaryinfotech.plannerapp.logic.SmartParser
import com.tertiaryinfotech.plannerapp.speech.SpeechRecognizerManager

/**
 * Voice-activated capture — tap the mic, speak (native Android speech-to-text), and the
 * on-device `SmartParser` interprets it into a to-do or appointment you can confirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCaptureSheet(
    onSave: (PlannerItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val speech = remember { SpeechRecognizerManager(context) }
    var parsed by remember { mutableStateOf<ParsedEntry?>(null) }

    DisposableEffect(Unit) { onDispose { speech.destroy() } }

    if (speech.transcript.isNotEmpty()) {
        parsed = SmartParser.parse(speech.transcript)
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) speech.toggle() }

    ModalBottomSheet(
        onDismissRequest = { speech.stop(); onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Add by Voice", style = MaterialTheme.typography.titleLarge)

            // Transcript
            if (speech.transcript.isEmpty()) {
                Text(
                    if (speech.isListening) "Listening…" else "Tap the mic and speak",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "“${speech.transcript}”",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }

            // Parsed preview
            parsed?.takeIf { it.title.isNotEmpty() }?.let { entry ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            entry.kind.iconVector,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            entry.kind.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(entry.title, style = MaterialTheme.typography.titleSmall)
                    entry.date?.let { ClockLabel(it) }
                }
            }

            // Mic button
            FilledIconButton(
                onClick = { micPermission.launch(Manifest.permission.RECORD_AUDIO) },
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (speech.isListening) MaterialTheme.colorScheme.error
                                     else MaterialTheme.colorScheme.primary
                ),
                enabled = speech.state != SpeechRecognizerManager.State.UNAVAILABLE,
            ) {
                Icon(
                    if (speech.isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (speech.isListening) "Stop recording" else "Start recording",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White,
                )
            }

            Text(
                if (speech.state == SpeechRecognizerManager.State.UNAVAILABLE)
                    "Speech recognition isn't available on this device right now."
                else
                    "Try: “Lunch with Sam tomorrow at 1pm” or “Buy groceries”.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    onClick = { speech.stop(); onDismiss() },
                    modifier = Modifier.weight(1f),
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        parsed?.takeIf { it.title.isNotEmpty() }?.let {
                            speech.stop()
                            onSave(it.makeItem())
                        }
                    },
                    enabled = parsed?.title?.isNotEmpty() == true,
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
