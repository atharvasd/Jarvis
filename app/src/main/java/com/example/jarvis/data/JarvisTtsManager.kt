package com.example.jarvis.data

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class JarvisTtsManager(context: Context) {
    private val TAG = "JarvisTts"
    private var tts: TextToSpeech? = null
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var onSpeechDoneCallback: (() -> Unit)? = null

    // Configuration settings that the user can tune
    var pitch: Float = 0.92f
        set(value) {
            field = value
            tts?.setPitch(value)
        }
        
    var speechRate: Float = 1.05f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureVoice()
                _isInitialized.value = true
                Log.d(TAG, "TTS Initialized successfully.")
            } else {
                Log.e(TAG, "Failed to initialize TTS. Status: $status")
            }
        }
    }

    private fun configureVoice() {
        val currentTts = tts ?: return
        
        // 1. Try to set British English locale first
        val localeUk = Locale.UK // en_GB
        val result = currentTts.setLanguage(localeUk)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "British English voice is not supported or missing data. Falling back to default English.")
            currentTts.setLanguage(Locale.ENGLISH)
        }

        // 2. Adjust Pitch and Speech Rate to fit J.A.R.V.I.S.
        currentTts.setPitch(pitch)
        currentTts.setSpeechRate(speechRate)

        // 3. Try to select a specific male voice if available (Android 5.0+)
        try {
            val voices = currentTts.voices
            if (!voices.isNullOrEmpty()) {
                // Find a British English voice (en-GB or en-UK)
                val ukVoices = voices.filter { voice ->
                    val loc = voice.locale
                    (loc.language.equals("en", ignoreCase = true) && loc.country.equals("GB", ignoreCase = true))
                }
                
                if (ukVoices.isNotEmpty()) {
                    // Try to find a male voice first. Usually has "male" or "ml" or "en-gb-x-gbd-local" or similar.
                    // Avoid voices that explicitly contain "female", "fml", "female" or similar indicators if we can.
                    val maleVoice = ukVoices.find { voice ->
                        val name = voice.name.lowercase(Locale.ROOT)
                        (name.contains("male") || name.contains("-x-gbd") || name.contains("-x-gbi") || name.contains("-x-gbc")) && 
                        !name.contains("female")
                    } ?: ukVoices.firstOrNull() // fallback to any UK voice
                    
                    if (maleVoice != null) {
                        currentTts.voice = maleVoice
                        Log.d(TAG, "Selected Jarvis TTS Voice: ${maleVoice.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting specific TTS voice: ${e.message}")
        }

        // 4. Setup Utterance Progress Listener to track play state
        currentTts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                _isPlaying.value = true
                Log.d(TAG, "TTS speech started: $utteranceId")
            }

            override fun onDone(utteranceId: String) {
                _isPlaying.value = false
                Log.d(TAG, "TTS speech completed: $utteranceId")
                onSpeechDoneCallback?.invoke()
                onSpeechDoneCallback = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                _isPlaying.value = false
                Log.e(TAG, "TTS speech error: $utteranceId")
                onSpeechDoneCallback?.invoke()
                onSpeechDoneCallback = null
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isPlaying.value = false
                Log.e(TAG, "TTS speech error: $utteranceId, code: $errorCode")
                onSpeechDoneCallback?.invoke()
                onSpeechDoneCallback = null
            }
        })
    }

    fun speak(text: String, onComplete: () -> Unit = {}) {
        if (!_isInitialized.value) {
            Log.w(TAG, "TTS not initialized yet. Skipping speech.")
            onComplete()
            return
        }
        
        stop()
        onSpeechDoneCallback = onComplete

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "jarvis_response_${System.currentTimeMillis()}")
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))
    }

    fun stop() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        _isPlaying.value = false
        onSpeechDoneCallback = null
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
