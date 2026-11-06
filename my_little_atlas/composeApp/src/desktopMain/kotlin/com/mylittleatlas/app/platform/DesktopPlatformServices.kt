package com.mylittleatlas.app.platform

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.mylittleatlas.app.data.AtlasStorage
import com.mylittleatlas.app.domain.Place
import org.jetbrains.skia.Image as SkiaImage
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.util.UUID
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class DesktopPlatformServices : PlatformServices {
    private val appDir = File(System.getProperty("user.home"), ".mylittleatlas").apply { mkdirs() }
    private val dataFile = File(appDir, "atlas.json")
    override val storage: AtlasStorage = object : AtlasStorage {
        override fun read() = dataFile.takeIf { it.exists() }?.readText(Charsets.UTF_8)
        override fun write(content: String) {
            val temp = File(appDir, "atlas.json.tmp")
            temp.writeText(content, Charsets.UTF_8)
            try { Files.move(temp.toPath(), dataFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (_: Exception) { Files.move(temp.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING) }
        }
    }
    override fun today() = LocalDate.now().toString()
    override fun uniqueId() = UUID.randomUUID().toString()
    override fun pickPhoto(onResult: (String?) -> Unit) {
        val chooser = JFileChooser().apply { dialogTitle = "选择照片"; fileFilter = FileNameExtensionFilter("图片", "jpg", "jpeg", "png", "webp") }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            val source = chooser.selectedFile
            val target = File(appDir, "photos/${UUID.randomUUID()}.${source.extension.ifBlank { "jpg" }}").apply { parentFile?.mkdirs() }
            runCatching { Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING); target.absolutePath }
                .onSuccess(onResult).onFailure { onResult(null) }
        } else onResult(null)
    }
    override fun exportBackup(json: String) {
        val chooser = JFileChooser().apply { dialogTitle = "导出图鉴备份"; selectedFile = File("my-little-atlas-backup.json") }
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            val target = chooser.selectedFile
            val file = if (target.extension.equals("json", true)) target else File(target.parentFile, "${target.name}.json")
            runCatching { file.writeText(json, Charsets.UTF_8) }
        }
    }
    override fun importBackup(onResult: (String?) -> Unit) {
        val chooser = JFileChooser().apply { dialogTitle = "恢复图鉴备份"; fileFilter = FileNameExtensionFilter("JSON 备份", "json") }
        onResult(if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) runCatching { chooser.selectedFile.readText(Charsets.UTF_8) }.getOrNull() else null)
    }
    override fun shareText(text: String) {
        runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) }
        javax.swing.JOptionPane.showMessageDialog(null, "分享文字已复制到剪贴板。", "My Little Atlas", javax.swing.JOptionPane.INFORMATION_MESSAGE)
    }
    override fun shareImage(bitmap: ImageBitmap) {
        val chooser = JFileChooser().apply { dialogTitle = "保存分享图片"; selectedFile = File("my-little-atlas.png"); fileFilter = FileNameExtensionFilter("PNG 图片", "png") }
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile.let { if (it.extension.equals("png", true)) it else File(it.parentFile, "${it.name}.png") }
            runCatching { ImageIO.write(bitmap.toAwtImage(), "png", file) }
        }
    }
    override fun navigate(place: Place) {
        val query = java.net.URLEncoder.encode("${place.name} ${place.address} ${place.city}", "UTF-8")
        val city = java.net.URLEncoder.encode(place.city, "UTF-8")
        val url = if (place.latitude != null && place.longitude != null) {
            "https://uri.amap.com/marker?position=${place.longitude},${place.latitude}&name=$query&coordinate=gaode&callnative=1"
        } else "https://uri.amap.com/search?keyword=$query&city=$city"
        runCatching { Desktop.getDesktop().browse(java.net.URI(url)) }
    }
    override fun openSource(url: String) { runCatching { Desktop.getDesktop().browse(java.net.URI(url)) } }
}

@Composable
actual fun LocalPhoto(path: String, modifier: Modifier) {
    val bitmap = remember(path) { runCatching { SkiaImage.makeFromEncoded(File(path).readBytes()).toComposeImageBitmap() }.getOrNull() }
    if (bitmap != null) Image(bitmap, null, modifier, contentScale = ContentScale.Crop)
    else Box(modifier.background(Color(0xFFE5E8DC)))
}
