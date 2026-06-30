package com.example.jarvis.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig

data class ChatHistoryEntry(
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class JarvisBrain {
    private val TAG = "JarvisBrain"
    
    private val systemInstructionText = """
        You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), the advanced, dry-witted AI assistant created by Tony Stark (Iron Man).
        You speak with a highly sophisticated, polite, and British tone. Always address the user as "Sir" (or "Ma'am" if they ask, but default to "Sir").
        Use signature phrases like "At your service, Sir", "Very well, Sir", "Right away, Sir", "System diagnostics indicate...".
        Keep replies brief, concise, and conversational, suitable for speech synthesis (Text-to-Speech). Avoid markdown styling (no asterisks, bolding, or lists) and special characters, as they sound awkward when spoken.
        Be helpful, witty, and highly intelligent. Do not break character.
    """.trimIndent()

    suspend fun generateReply(apiKey: String, userMessage: String, history: List<ChatHistoryEntry>): String {
        try {
            val model = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.7f
                },
                systemInstruction = content { text(systemInstructionText) }
            )

            // Convert custom history list to Gemini SDK content history
            // We omit the very last user message because we'll pass it to sendMessage
            val sdkHistory = history.map { entry ->
                content(role = if (entry.isUser) "user" else "model") { text(entry.message) }
            }

            val chat = model.startChat(history = sdkHistory)
            val response = chat.sendMessage(userMessage)
            
            return response.text?.trim() ?: "I'm afraid I cannot formulate a reply at the moment, Sir."
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown error"
            if (errorMsg.contains("API_KEY_INVALID") || errorMsg.contains("API key not valid")) {
                return "The API key provided appears to be invalid, Sir. Please check your credentials in the settings panel."
            }
            return "Apologies, Sir. An error occurred while communicating with the neural networks: $errorMsg"
        }
    }
}
