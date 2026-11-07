package com.mylittleatlas.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.mylittleatlas.app.data.AtlasStorage
import com.mylittleatlas.app.domain.Place

interface PlatformServices {
    var serviceUrl: String
    val transport: AtlasTransport
    val storage: AtlasStorage
    fun today(): String
    fun uniqueId(): String
    fun pickPhoto(onResult: (String?) -> Unit)
    fun exportBackup(json: String)
    fun importBackup(onResult: (String?) -> Unit)
    fun shareText(text: String)
    fun shareImage(bitmap: ImageBitmap)
    fun saveImage(bitmap: ImageBitmap): Boolean
    fun locate(onResult:(com.mylittleatlas.app.domain.GeoPoint?,String?)->Unit)
    fun navigate(place: Place)
    fun openSource(url: String)
}

@Composable
expect fun LocalPhoto(path: String, modifier: Modifier)

@Composable
expect fun AtlasBackHandler(enabled:Boolean,onBack:()->Unit)

@Composable
expect fun AtlasWindowStyle()
