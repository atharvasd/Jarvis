package com.example.jarvis.ui.main

import com.example.jarvis.data.ChatHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class JarvisVoiceAssistantTest {
    @Test
    fun chatHistoryEntry_initialization() {
        val entry = ChatHistoryEntry(message = "Hello Jarvis", isUser = true)
        assertEquals("Hello Jarvis", entry.message)
        assertEquals(true, entry.isUser)
        assertNotNull(entry.timestamp)
    }

    @Test
    fun chatHistoryEntry_assistantResponse() {
        val entry = ChatHistoryEntry(message = "At your service, Sir.", isUser = false)
        assertEquals("At your service, Sir.", entry.message)
        assertEquals(false, entry.isUser)
    }
}
