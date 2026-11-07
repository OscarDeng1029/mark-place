package com.mylittleatlas.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mylittleatlas.app.platform.AndroidPlatformServices
import com.mylittleatlas.app.ui.AtlasApp

class MainActivity : ComponentActivity() {
    private lateinit var services: AndroidPlatformServices
    private var incomingText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingText = sharedText(intent)
        services = AndroidPlatformServices(this)
        services.photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            services.acceptPickedPhoto(uri)
        }
        services.backupWriter = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            services.writePendingBackup(uri)
        }
        services.backupReader = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            services.readSelectedBackup(uri)
        }
        setContent { AtlasApp(services, incomingText) {
            incomingText = null
            intent?.removeExtra(Intent.EXTRA_TEXT)
        } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingText = sharedText(intent)
    }

    private fun sharedText(intent: Intent?): String? =
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true)
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() } else null
}
