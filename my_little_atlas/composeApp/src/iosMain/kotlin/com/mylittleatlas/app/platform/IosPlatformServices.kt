package com.mylittleatlas.app.platform

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toSkiaBitmap
import androidx.compose.ui.layout.ContentScale
import com.mylittleatlas.app.data.AtlasStorage
import com.mylittleatlas.app.domain.GeoPoint
import com.mylittleatlas.app.domain.MapGeometry
import com.mylittleatlas.app.domain.Place
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image as SkiaImage
import kotlin.coroutines.resume

internal class IosPlatformServices(private val bridge: IosPlatformBridge) : PlatformServices {
    init { activeBridge = bridge }
    override var serviceUrl: String
        get() = bridge.serviceUrl
        set(value) { bridge.serviceUrl = value.trim().trimEnd('/') }
    override val transport: AtlasTransport = IosAtlasTransport(bridge) { serviceUrl }
    override val storage: AtlasStorage = object : AtlasStorage {
        override fun read() = bridge.readData()
        override fun write(content: String) {
            bridge.writeData(content)?.let { error(it) }
        }
    }
    override fun today() = bridge.today()
    override fun uniqueId() = bridge.uniqueId()
    override fun pickPhoto(onResult: (String?) -> Unit) = bridge.pickPhoto(onResult)
    override fun exportBackup(json: String) = bridge.exportBackup(json)
    override fun importBackup(onResult: (String?) -> Unit) = bridge.importBackup(onResult)
    override fun shareText(text: String) = bridge.shareText(text)
    override fun shareImage(bitmap: ImageBitmap) = bridge.sharePng(bitmap.pngBytes())
    override suspend fun saveImage(bitmap: ImageBitmap): Boolean = suspendCancellableCoroutine { continuation ->
        bridge.savePng(bitmap.pngBytes()) { saved ->
            if (continuation.isActive) continuation.resume(saved)
        }
    }
    override fun locate(onResult: (GeoPoint?, String?) -> Unit) = bridge.locate { lat, lon, error ->
        val latitude = lat?.toDoubleOrNull()
        val longitude = lon?.toDoubleOrNull()
        onResult(if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null, error)
    }
    override fun navigate(place: Place) {
        val position = MapGeometry.position(place)
        bridge.navigate(place.name, place.address, place.city, position?.latitude?.toString(), position?.longitude?.toString())
    }
    override fun openSource(url: String) = bridge.openSource(url)
}

// The app has one long-lived root controller. LocalPhoto may also appear in Compose dialog controllers.
private var activeBridge: IosPlatformBridge? = null

private fun ImageBitmap.pngBytes(): ByteArray =
    requireNotNull(SkiaImage.makeFromBitmap(toSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)).bytes

actual fun decodeMapTile(bytes: ByteArray): ImageBitmap =
    SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()

@Composable
actual fun LocalPhoto(path: String, modifier: Modifier) {
    val bitmap = remember(path) {
        runCatching { activeBridge?.readImage(path)?.let { SkiaImage.makeFromEncoded(it).toComposeImageBitmap() } }.getOrNull()
    }
    if (bitmap != null) Image(bitmap, null, modifier, contentScale = ContentScale.Crop)
    else Box(modifier.background(Color(0xFFE5E8DC)))
}

/** iOS uses the shared visible back/close controls; there is no Android system-back button. */
@Composable
actual fun AtlasBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

/** Safe-area and keyboard insets are supplied by ComposeUIViewController. The host forces light mode. */
@Composable
actual fun AtlasWindowStyle() = Unit
