package com.mylittleatlas.app

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mylittleatlas.app.platform.DesktopPlatformServices
import com.mylittleatlas.app.ui.AtlasApp

fun main() = application {
    val services = remember { DesktopPlatformServices() }
    Window(onCloseRequest = ::exitApplication, title = "My Little Atlas · 私藏去处") {
        AtlasApp(services)
    }
}
