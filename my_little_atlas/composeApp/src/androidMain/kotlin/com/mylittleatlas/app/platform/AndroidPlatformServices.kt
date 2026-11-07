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
    private val preferences = context.getSharedPreferences("atlas-services", android.content.Context.MODE_PRIVATE)
    override var serviceUrl: String
        get() = preferences.getString("endpoint", "http://127.0.0.1:3212")!!
        set(value) { preferences.edit().putString("endpoint", value.trim().trimEnd('/')).apply() }
    override val transport: AtlasTransport = JvmAtlasTransport { serviceUrl }
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
    lateinit var locationPermission: ActivityResultLauncher<Array<String>>
    private var locationCallback: ((com.mylittleatlas.app.domain.GeoPoint?,String?)->Unit)? = null
    lateinit var backupWriter: ActivityResultLauncher<String>
    lateinit var backupReader: ActivityResultLauncher<Array<String>>
    private var photoCallback: ((String?) -> Unit)? = null
    private var backupCallback: ((String?) -> Unit)? = null
    private var pendingJson: String = ""

    override fun today() = LocalDate.now().toString()
    override fun locate(onResult:(com.mylittleatlas.app.domain.GeoPoint?,String?)->Unit){
        locationCallback=onResult
        if(androidx.core.content.ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_COARSE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED)acceptLocationPermission()
        else locationPermission.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    @android.annotation.SuppressLint("MissingPermission")
    fun acceptLocationPermission(){
        val callback=locationCallback ?: return
        locationCallback=null
        if(androidx.core.content.ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_COARSE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED){callback(null,"未开启定位。仍可手动切换城市、浏览地图和导航。");return}
        val manager=context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val providers=listOf(android.location.LocationManager.NETWORK_PROVIDER,android.location.LocationManager.GPS_PROVIDER).filter{runCatching{manager.isProviderEnabled(it)}.getOrDefault(false)}
        val last=providers.mapNotNull{runCatching{manager.getLastKnownLocation(it)}.getOrNull()}.maxByOrNull{it.time}
        if(last!=null&&System.currentTimeMillis()-last.time<10*60*1000){callback(com.mylittleatlas.app.domain.GeoPoint(last.latitude,last.longitude),null);return}
        if(providers.isEmpty()){callback(null,"系统定位服务未开启。开启后可再次尝试，也可以继续手动浏览。");return}
        val handler=android.os.Handler(android.os.Looper.getMainLooper())
        var done=false
        lateinit var listener:android.location.LocationListener
        val finish:(android.location.Location?)->Unit={location->if(!done){done=true;manager.removeUpdates(listener);callback(location?.let{com.mylittleatlas.app.domain.GeoPoint(it.latitude,it.longitude)},if(location==null)"暂时没有获得位置，请稍后重试。" else null)}}
        listener=object:android.location.LocationListener{override fun onLocationChanged(location:android.location.Location){finish(location)}}
        var requested=false
        providers.forEach{provider->runCatching{manager.requestLocationUpdates(provider,0L,0f,listener,android.os.Looper.getMainLooper());requested=true}}
        if(!requested)finish(null) else handler.postDelayed({finish(null)},12000)
    }
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
    override fun saveImage(bitmap: ImageBitmap): Boolean = runCatching {
        if(android.os.Build.VERSION.SDK_INT<29)return@runCatching false
        val values=android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,"MyLittleAtlas-"+System.currentTimeMillis()+".png")
            put(MediaStore.Images.Media.MIME_TYPE,"image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/MyLittleAtlas")
            put(MediaStore.Images.Media.IS_PENDING,1)
        }
        val resolver=context.contentResolver
        val uri=resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values) ?: return@runCatching false
        try {
            requireNotNull(resolver.openOutputStream(uri)).use{check(bitmap.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it))}
            values.clear();values.put(MediaStore.Images.Media.IS_PENDING,0);resolver.update(uri,values,null,null);true
        }catch(e:Exception){resolver.delete(uri,null,null);throw e}
    }.getOrDefault(false)
    override fun navigate(place: Place) {
        val query = Uri.encode("${place.name} ${place.address} ${place.city}")
        val uri = if (place.latitude != null && place.longitude != null) Uri.parse("geo:${place.latitude},${place.longitude}?q=$query") else Uri.parse("geo:0,0?q=$query")
        runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri),"选择导航方式")) }
    }
    override fun openSource(url: String) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
}

actual fun decodeMapTile(bytes: ByteArray): ImageBitmap =
    requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)).asImageBitmap()

@Composable
actual fun LocalPhoto(path: String, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap = remember(path) { runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
    if (bitmap != null) Image(bitmap, null, modifier, contentScale = ContentScale.Crop)
    else Box(modifier.background(Color(0xFFE5E8DC)))
}

@Composable
actual fun AtlasBackHandler(enabled:Boolean,onBack:()->Unit){androidx.activity.compose.BackHandler(enabled,onBack)}

@Composable
actual fun AtlasWindowStyle(){
    val view=androidx.compose.ui.platform.LocalView.current
    val context=LocalContext.current
    androidx.compose.runtime.SideEffect {
        val provider=generateSequence(view as android.view.View?){it.parent as? android.view.View}.filterIsInstance<androidx.compose.ui.window.DialogWindowProvider>().firstOrNull()
        val window=provider?.window ?: (context as? ComponentActivity)?.window
        if(window!=null){
            if(provider!=null){window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window,false)}
            window.statusBarColor=android.graphics.Color.rgb(247,242,233)
            window.navigationBarColor=android.graphics.Color.rgb(247,242,233)
            androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).apply{isAppearanceLightStatusBars=true;isAppearanceLightNavigationBars=true}
        }
    }
}
