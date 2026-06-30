package com.example.jarvis.ui.main

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.data.ChatHistoryEntry
import com.example.jarvis.data.JarvisBrain
import com.example.jarvis.data.JarvisSpeechRecognizer
import com.example.jarvis.data.JarvisTtsManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class AssistantState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "JarvisVM"

    private val prefs = application.getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)

    // Services
    private val ttsManager = JarvisTtsManager(application)
    private val speechRecognizer = JarvisSpeechRecognizer(application)
    private val brain = JarvisBrain()

    // UI States
    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _history = MutableStateFlow<List<ChatHistoryEntry>>(emptyList())
    val history: StateFlow<List<ChatHistoryEntry>> = _history.asStateFlow()

    private val _speechText = MutableStateFlow("")
    val speechText: StateFlow<String> = _speechText.asStateFlow()

    private val _jarvisReplyText = MutableStateFlow("")
    val jarvisReplyText: StateFlow<String> = _jarvisReplyText.asStateFlow()

    val volumeDb: StateFlow<Float> = speechRecognizer.volumeDb
    val isTtsInitialized: StateFlow<Boolean> = ttsManager.isInitialized

    private val _apiKey = MutableStateFlow(prefs.getString("api_key", "") ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    // Voice customizers
    private val _voicePitch = MutableStateFlow(prefs.getFloat("voice_pitch", 0.92f))
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    private val _voiceRate = MutableStateFlow(prefs.getFloat("voice_rate", 1.05f))
    val voiceRate: StateFlow<Float> = _voiceRate.asStateFlow()

    // Events to trigger UI permission request
    private val _requestPermissionEvent = MutableSharedFlow<Unit>()
    val requestPermissionEvent: SharedFlow<Unit> = _requestPermissionEvent.asSharedFlow()

    init {
        // Load voice parameters into manager
        ttsManager.pitch = _voicePitch.value
        ttsManager.speechRate = _voiceRate.value

        // Observe speech recognizer states
        viewModelScope.launch {
            speechRecognizer.state.collectLatest { state ->
                handleSpeechState(state)
            }
        }
    }

    private fun handleSpeechState(state: JarvisSpeechRecognizer.SpeechState) {
        when (state) {
            JarvisSpeechRecognizer.SpeechState.Idle -> {
                if (_assistantState.value == AssistantState.LISTENING) {
                    _assistantState.value = AssistantState.IDLE
                }
            }
            JarvisSpeechRecognizer.SpeechState.Ready -> {
                _assistantState.value = AssistantState.LISTENING
                _speechText.value = "Listening..."
            }
            JarvisSpeechRecognizer.SpeechState.Listening -> {
                _assistantState.value = AssistantState.LISTENING
                _speechText.value = "Speak now, Sir..."
            }
            is JarvisSpeechRecognizer.SpeechState.PartialResult -> {
                _speechText.value = state.text
            }
            is JarvisSpeechRecognizer.SpeechState.Result -> {
                _speechText.value = state.text
                processTextCommand(state.text)
            }
            is JarvisSpeechRecognizer.SpeechState.Error -> {
                _speechText.value = state.message
                _assistantState.value = AssistantState.IDLE
                speakText(state.message)
            }
        }
    }

    fun toggleListening(hasPermission: Boolean) {
        if (!hasPermission) {
            viewModelScope.launch {
                _requestPermissionEvent.emit(Unit)
            }
            return
        }

        when (_assistantState.value) {
            AssistantState.LISTENING -> {
                speechRecognizer.stopListening()
                _assistantState.value = AssistantState.IDLE
            }
            AssistantState.SPEAKING -> {
                ttsManager.stop()
                startListening()
            }
            AssistantState.IDLE, AssistantState.PROCESSING -> {
                startListening()
            }
        }
    }

    private fun startListening() {
        ttsManager.stop()
        _speechText.value = "Initializing systems..."
        _jarvisReplyText.value = ""
        speechRecognizer.startListening()
    }

    fun processTextCommand(text: String) {
        if (text.isBlank()) return
        
        _speechText.value = text
        _assistantState.value = AssistantState.PROCESSING
        _jarvisReplyText.value = "Analyzing data, Sir..."

        val key = _apiKey.value
        if (key.isBlank()) {
            val warning = "I require a Gemini API Key to function, Sir. Please configure it in my settings panel."
            _jarvisReplyText.value = warning
            _assistantState.value = AssistantState.IDLE
            speakText(warning)
            return
        }

        // Add user query to local list (history)
        val userEntry = ChatHistoryEntry(text, isUser = true)
        val currentHistory = _history.value.toMutableList()
        currentHistory.add(userEntry)
        _history.value = currentHistory

        viewModelScope.launch {
            // Get reply from Gemini, passing conversation history
            // We pass the history excluding the userEntry we just added, as brain takes history + userMessage separately
            val historyForBrain = currentHistory.dropLast(1)
            val reply = brain.generateReply(key, text, historyForBrain)
            
            // Add assistant response to history
            val assistantEntry = ChatHistoryEntry(reply, isUser = false)
            val updatedHistory = _history.value.toMutableList()
            updatedHistory.add(assistantEntry)
            _history.value = updatedHistory

            _jarvisReplyText.value = reply
            speakText(reply)
        }
    }

    private fun speakText(text: String) {
        _assistantState.value = AssistantState.SPEAKING
        ttsManager.speak(text) {
            viewModelScope.launch {
                _assistantState.value = AssistantState.IDLE
            }
        }
    }

    fun stopSpeaking() {
        if (_assistantState.value == AssistantState.SPEAKING) {
            ttsManager.stop()
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
        _jarvisReplyText.value = ""
        _speechText.value = ""
        ttsManager.stop()
        _assistantState.value = AssistantState.IDLE
    }

    // Settings management
    fun setApiKey(key: String) {
        _apiKey.value = key
        prefs.edit().putString("api_key", key).apply()
    }

    fun setVoiceSettings(pitch: Float, rate: Float) {
        _voicePitch.value = pitch
        _voiceRate.value = rate
        prefs.edit().putFloat("voice_pitch", pitch).putFloat("voice_rate", rate).apply()
        
        ttsManager.pitch = pitch
        ttsManager.speechRate = rate
    }

    fun setShowSettings(show: Boolean) {
        _showSettings.value = show
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.cancel()
        ttsManager.shutdown()
    }
}
