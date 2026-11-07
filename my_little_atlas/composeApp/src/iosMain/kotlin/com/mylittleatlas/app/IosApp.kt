@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.mylittleatlas.app

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.mutableStateOf
import com.mylittleatlas.app.data.AtlasRepository
import com.mylittleatlas.app.data.AtlasStorage
import com.mylittleatlas.app.data.DemoAtlas
import com.mylittleatlas.app.platform.IosPlatformBridge
import com.mylittleatlas.app.platform.IosPlatformServices
import com.mylittleatlas.app.ui.AtlasApp
import platform.UIKit.UIViewController

/** The same root, screens, assets and controller used by Android. UIKit only supplies device services. */
fun MainViewController(bridge: IosPlatformBridge): UIViewController {
    val platform = IosPlatformServices(bridge)
    val incomingText = mutableStateOf<String?>(null)
    bridge.observeIncomingText { incomingText.value = it }
    return ComposeUIViewController {
        AtlasApp(platform, incomingText.value) { incomingText.value = null }
    }
}

/** Isolated simulator UI fixtures. The Swift host only calls this in a DEBUG UI-test launch. */
fun previewAtlasJson(): String = AtlasRepository(object : AtlasStorage {
    override fun read(): String? = null
    override fun write(content: String) = Unit
}).export(DemoAtlas.create())
