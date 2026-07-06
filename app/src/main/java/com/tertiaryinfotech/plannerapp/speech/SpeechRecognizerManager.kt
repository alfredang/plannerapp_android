package com.tertiaryinfotech.plannerapp.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Thin state wrapper around the platform `SpeechRecognizer` — the Android counterpart of the
 * iOS `SpeechRecognizer` service. Uses on-device/native recognition, streams partial results
 * into [transcript], and flips [state] between Idle and Listening so Compose UI can react.
 *
 * Construct lazily (on first mic tap) so no microphone prompt appears at launch.
 */
class SpeechRecognizerManager(private val context: Context) {

    enum class State { IDLE, LISTENING, UNAVAILABLE }

    var state by mutableStateOf(State.IDLE)
        private set
    var transcript by mutableStateOf("")
        private set

    val isListening: Boolean get() = state == State.LISTENING

    private var recognizer: SpeechRecognizer? = null

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.let { if (it.isNotBlank()) transcript = it }
        }

        override fun onResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.let { if (it.isNotBlank()) transcript = it }
            state = State.IDLE
        }

        override fun onError(error: Int) {
            // "No match"/timeout just means silence — return to idle quietly.
            state = when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> State.IDLE
                else -> State.IDLE
            }
        }
    }

    fun start() {
        if (isListening) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            state = State.UNAVAILABLE
            return
        }
        transcript = ""
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context)
            .also { it.setRecognitionListener(listener); recognizer = it }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }
        state = State.LISTENING
        r.startListening(intent)
    }

    fun stop() {
        recognizer?.stopListening()
        state = State.IDLE
    }

    fun toggle() = if (isListening) stop() else start()

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        state = State.IDLE
    }
}
