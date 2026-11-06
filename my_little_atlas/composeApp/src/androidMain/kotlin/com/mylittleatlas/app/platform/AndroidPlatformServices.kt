package com.mylittleatlas.app.platform

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import android.graphics.BitmapFactory
import com.mylittleatlas.app.data.AtlasStorage
import com.mylittleatlas.app.domain.Place
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.util.UUID

class AndroidPlatformServices(private val context: ComponentActivity) : PlatformServices {
    private val dataFile = File(context.filesDir, "atlas.json")
    override val storage: AtlasStorage = object : AtlasStorage {
        override fun read() = dataFile.takeIf { it.exists() }?.readText(Charsets.UTF_8)
        override fun write(content: String) {
            val temp = File(context.filesDir, "atlas.json.tmp")
            temp.writeText(content, Charsets.UTF_8)
            try { Files.move(temp.toPath(), dataFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (_: Exception) { Files.move(temp.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING) }
        }
    }
    lateinit var photoPicker: ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest>
    lateinit var backupWriter: ActivityResultLauncher<String>
    lateinit var backupReader: ActivityResultLauncher<Array<String>>
    private var photoCallback: ((String?) -> Unit)? = null
    private var backupCallback: ((String?) -> Unit)? = null
    private var pendingJson: String = ""

    override fun today() = LocalDate.now().toString()
    override fun uniqueId() = UUID.randomUUID().toString()
    override fun pickPhoto(onResult: (String?) -> Unit) {
        photoCallback = onResult
        photoPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    fun acceptPickedPhoto(uri: Uri?) {
        val saved = uri?.let(::copyPhoto)
        val callback = photoCallback; photoCallback = null
        callback?.invoke(saved)
    }
    private fun copyPhoto(uri: Uri): String? = runCatching {
        val folder = File(context.filesDir, "photos").apply { mkdirs() }
        val target = File(folder, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use(input::copyTo) }
        target.absolutePath.takeIf { target.exists() && target.length() > 0L }
    }.getOrNull()

    override fun exportBackup(json: String) { pendingJson = json; backupWriter.launch("my-little-atlas-backup.json") }
    fun writePendingBackup(uri: Uri?) {
        if (uri == null) return
        runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(pendingJson) } }
    }
    override fun importBackup(onResult: (String?) -> Unit) {
        backupCallback = onResult
        backupReader.launch(arrayOf("application/json", "text/json", "text/plain"))
    }
    fun readSelectedBackup(uri: Uri?) {
        val value = uri?.let { runCatching { context.contentResolver.openInputStream(it)?.bufferedReader(Charsets.UTF_8)?.use { reader -> reader.readText() } }.getOrNull() }
        val callback = backupCallback; backupCallback = null
        callback?.invoke(value)
    }
    override fun shareText(text: String) {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "分享图鉴"))
    }
    override fun shareImage(bitmap: ImageBitmap) {
        val folder = File(context.cacheDir, "shared").apply { mkdirs() }
        val image = File(folder, "atlas-${UUID.randomUUID()}.png")
        image.outputStream().use { bitmap.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", image)
        val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "分享我的城市收藏图鉴"))
    }
    override fun navigate(place: Place) {
        val query = Uri.encode("${place.name} ${place.address} ${place.city}")
        val uri = if (place.latitude != null && place.longitude != null) Uri.parse("geo:${place.latitude},${place.longitude}?q=$query") else Uri.parse("geo:0,0?q=$query")
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
    override fun openSource(url: String) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
}

@Composable
actual fun LocalPhoto(path: String, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap = remember(path) { runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
    if (bitmap != null) Image(bitmap, null, modifier, contentScale = ContentScale.Crop)
    else Box(modifier.background(Color(0xFFE5E8DC)))
}
