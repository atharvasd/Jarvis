package com.example.jarvis.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class JarvisSpeechRecognizer(private val context: Context) {
    private val TAG = "JarvisSpeech"
    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private val _volumeDb = MutableStateFlow(0f)
    val volumeDb: StateFlow<Float> = _volumeDb.asStateFlow()

    sealed interface SpeechState {
        object Idle : SpeechState
        object Ready : SpeechState
        object Listening : SpeechState
        data class PartialResult(val text: String) : SpeechState
        data class Result(val text: String) : SpeechState
        data class Error(val message: String) : SpeechState
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
            _state.value = SpeechState.Ready
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Speech beginning")
            _state.value = SpeechState.Listening
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Keep volume in 0..1 range (normalized roughly from -2dB to 10dB)
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _volumeDb.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "Speech end")
            _volumeDb.value = 0f
        }

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error, Sir."
                SpeechRecognizer.ERROR_CLIENT -> "Client side error."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions, Sir. Please allow audio access."
                SpeechRecognizer.ERROR_NETWORK -> "Network error, Sir. I need an internet connection."
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
                SpeechRecognizer.ERROR_NO_MATCH -> "No match found. Did you speak, Sir?"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition engine is busy. Please try again."
                SpeechRecognizer.ERROR_SERVER -> "Server error."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. No command detected, Sir."
                else -> "An unknown error occurred, Sir."
            }
            Log.e(TAG, "Speech Error: $message (code: $error)")
            _state.value = SpeechState.Error(message)
            _volumeDb.value = 0f
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            Log.d(TAG, "Speech Result: $text")
            _state.value = SpeechState.Result(text)
            _volumeDb.value = 0f
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotEmpty()) {
                _state.value = SpeechState.PartialResult(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = SpeechState.Error("Speech recognition is not available on this device, Sir.")
            return
        }

        cancel() // Clean up any active session

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Request English/UK if possible, fallback to default locale
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-GB")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
        }

        speechRecognizer?.startListening(intent)
        _state.value = SpeechState.Ready
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _volumeDb.value = 0f
    }

    fun cancel() {
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _state.value = SpeechState.Idle
        _volumeDb.value = 0f
    }
}
