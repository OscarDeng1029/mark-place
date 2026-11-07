package com.mylittleatlas.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import com.mylittleatlas.app.domain.*
import com.mylittleatlas.app.platform.decodeMapTile
import com.mylittleatlas.app.presentation.AtlasController
import kotlinx.coroutines.*
import kotlin.math.*

@Composable
fun MapScreen(controller: AtlasController, onPlace: (Place)->Unit, onList: ()->Unit, wide: Boolean) {
    val data = controller.data
    var filter by remember { mutableStateOf(PlaceFilter.ALL) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var collectionId by remember { mutableStateOf<String?>(null) }
    var layers by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf<Place?>(null) }
    val places = data.filterPlaces(filter,collectionId=collectionId)
    val plotted = places.filter { !data.isDemoPlace(it) && MapGeometry.position(it)!=null }
    val unlocated = places.filter { !data.isDemoPlace(it) && MapGeometry.position(it)==null }
    val sampleCount = places.count { data.isDemoPlace(it) }
    val selected = plotted.find { it.id==selectedId } ?: plotted.firstOrNull()
    var center by remember { mutableStateOf(GeoPoint(31.2304,121.4737)) }
    var zoom by remember { mutableStateOf(12) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var recenter by remember { mutableStateOf(0) }
    var retry by remember { mutableStateOf(0) }
    val failures = remember { mutableStateMapOf<String,Boolean>() }
    val density = LocalDensity.current.density
    val tilePx = 256f*density
    LaunchedEffect(plotted.map { it.id to MapGeometry.position(it) },viewport,recenter) {
        if(viewport.width>0 && viewport.height>0) {
            val fitted = MapGeometry.fit(plotted.mapNotNull(MapGeometry::position),viewport.width/density-80.0,viewport.height/density-350.0)
            center=fitted.first; zoom=fitted.second
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp)).background(AtlasColors.Paper).onSizeChanged { viewport=it }) {
        val origin = MapGeometry.project(center,zoom)
        val startX=floor(origin.x-viewport.width/(2*tilePx)).toInt(); val endX=floor(origin.x+viewport.width/(2*tilePx)).toInt()
        val startY=floor(origin.y-viewport.height/(2*tilePx)).toInt(); val endY=floor(origin.y+viewport.height/(2*tilePx)).toInt()
        Box(Modifier.fillMaxSize().semantics { contentDescription="真实高德地图，可拖动和双指缩放" }.pointerInput(zoom,tilePx) {
            var scale=1f
            detectTransformGestures { centroid,pan,factor,_ ->
                val before=MapGeometry.project(center,zoom)
                var next=WorldPoint(before.x-pan.x/tilePx,before.y-pan.y/tilePx)
                scale*=factor
                val delta=if(scale>1.45f) 1 else if(scale<.69f) -1 else 0
                if(delta!=0) {
                    val target=(zoom+delta).coerceIn(3,18)
                    if(target!=zoom) {
                        val ratio=2.0.pow(target-zoom)
                        val ax=(centroid.x-viewport.width/2f)/tilePx; val ay=(centroid.y-viewport.height/2f)/tilePx
                        next=WorldPoint((next.x+ax)*ratio-ax,(next.y+ay)*ratio-ay); zoom=target
                    }
                    scale=1f
                }
                val n=2.0.pow(zoom)
                center=MapGeometry.unproject(WorldPoint(next.x.coerceIn(0.0,n),next.y.coerceIn(0.0,n)),zoom)
            }
        }) {
            if(viewport.width>0) for(x in startX..endX) for(y in startY..endY) {
                if(x !in 0 until (1 shl zoom) || y !in 0 until (1 shl zoom)) continue
                val tileKey="$zoom/$x/$y"
                key(tileKey) {
                    MapTile(controller,zoom,x,y,retry,Modifier.offset {
                        IntOffset(((x-origin.x)*tilePx+viewport.width/2f).roundToInt(),((y-origin.y)*tilePx+viewport.height/2f).roundToInt())
                    }.requiredSize(256.dp), { failed -> failures[tileKey]=failed })
                    DisposableEffect(tileKey) { onDispose { failures.remove(tileKey) } }
                }
            }
        }
        plotted.forEach { place ->
            val point=MapGeometry.project(MapGeometry.position(place)!!,zoom)
            val x=((point.x-origin.x)*tilePx+viewport.width/2).toFloat()/density
            val y=((point.y-origin.y)*tilePx+viewport.height/2).toFloat()/density
            if(x> -50 && x<viewport.width/density+50 && y> -50 && y<viewport.height/density+50) {
                val isSelected=place.id==selected?.id
                // The tip is anchored to the real POI location.
                Column(Modifier.offset(x=(x-21).dp,y=(y-49).dp).width(42.dp).clickable { selectedId=place.id }
                    .semantics { contentDescription="地图标记：${place.name}" },horizontalAlignment=Alignment.CenterHorizontally) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(
                        if(isSelected) AtlasColors.Card else if(data.isVisited(place.id)) AtlasColors.Green else AtlasColors.Peach),contentAlignment=Alignment.Center) {
                        if(isSelected) Photo(place.cover,Modifier.padding(4.dp).fillMaxSize().clip(RoundedCornerShape(11.dp)))
                        else AtlasIcon(Glyph.PIN,color=AtlasColors.Card,size=23.dp)
                    }
                    Box(Modifier.width(2.dp).height(5.dp).background(AtlasColors.Green))
                    Box(Modifier.size(4.dp).background(AtlasColors.Green,CircleShape))
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=22.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Row(Modifier.weight(1f).background(AtlasColors.Paper.copy(alpha=.94f),RoundedCornerShape(12.dp)).padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
                    AtlasIcon(Glyph.PIN,size=18.dp,color=AtlasColors.Green); Spacer(Modifier.width(7.dp))
                    Text("我的${selected?.city ?: "城市"}地图",style=MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(10.dp)); IconButton(Glyph.LIST,"查看地点列表",onList,filled=true)
            }
            FilterRow(filter,{ filter=it; selectedId=null })
            Row(Modifier.background(AtlasColors.Card.copy(alpha=.96f),RoundedCornerShape(10.dp)).padding(horizontal=11.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).background(AtlasColors.Sage,CircleShape)); Spacer(Modifier.width(6.dp))
                Text("${plotted.size} 处已定位${if(unlocated.isNotEmpty()) " · ${unlocated.size} 处待定位" else ""}${if(sampleCount>0) " · 示例不标注" else ""}",fontSize=10.sp,color=AtlasColors.Muted)
                if(unlocated.isNotEmpty()) Text("  补全",fontSize=10.sp,color=AtlasColors.Green,modifier=Modifier.clickable { pending=true })
            }
        }
        Column(Modifier.align(Alignment.CenterEnd).padding(end=18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            IconButton(Glyph.LAYERS,"筛选收藏夹",{ layers=true },filled=true)
            IconButton(Glyph.PLUS,"放大地图",{ zoom=(zoom+1).coerceAtMost(18) },filled=true)
            Surface(Modifier.size(42.dp).clickable { zoom=(zoom-1).coerceAtLeast(3) }.semantics { contentDescription="缩小地图" },shape=CircleShape,color=AtlasColors.Card) {
                Box(contentAlignment=Alignment.Center) { Text("−",fontSize=24.sp,color=AtlasColors.Green) }
            }
            IconButton(Glyph.NAVIGATE,"返回收藏地图中心",{ recenter++ },filled=true)
        }
        Column(Modifier.align(Alignment.BottomCenter).widthIn(max=650.dp).fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
            if(failures.values.any { it }) Row(Modifier.fillMaxWidth().background(AtlasColors.Card,RoundedCornerShape(10.dp)).padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("部分地图未加载，请检查网络。",fontSize=11.sp,color=AtlasColors.Muted,modifier=Modifier.weight(1f))
                TextButton(onClick={ retry++ }) { Text("重试",fontSize=11.sp) }
            }
            if(selected!=null) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AtlasColors.Card).clickable { onPlace(selected) }.padding(12.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    Photo(selected.cover,Modifier.size(75.dp).clip(RoundedCornerShape(12.dp)))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text(selected.name,style=MaterialTheme.typography.titleMedium)
                        Text(selected.address,fontSize=11.sp,color=AtlasColors.Muted,maxLines=2)
                        Text(if(data.isVisited(selected.id)) "已去过 · 回看这段小记" else "想去 · 等一次刚好的出发",fontSize=10.sp,color=AtlasColors.Green)
                    }
                    AtlasIcon(Glyph.ARROW,size=18.dp,color=AtlasColors.Green)
                }
            } else Column(Modifier.fillMaxWidth().background(AtlasColors.Card,RoundedCornerShape(20.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(if(unlocated.isEmpty()) "这张地图，等你慢慢填满" else "${unlocated.size} 个收藏还没有地图位置",style=MaterialTheme.typography.titleMedium)
                Text(if(unlocated.isEmpty()) "解析笔记或确认门店后，在这里查看真实位置。" else "在高德搜索对应分店，确认后就会出现在地图。",fontSize=12.sp,color=AtlasColors.Muted)
                TextButton(onClick={ if(unlocated.isNotEmpty()) pending=true else onList() }) { Text(if(unlocated.isNotEmpty()) "补全地点位置" else "查看收藏列表") }
            }
            Text("© 高德地图 · GCJ-02 底图",fontSize=9.sp,color=AtlasColors.Ink,modifier=Modifier.align(Alignment.CenterHorizontally)
                .background(AtlasColors.Card.copy(alpha=.9f),RoundedCornerShape(4.dp)).padding(4.dp))
        }
    }
    if(layers) AlertDialog(onDismissRequest={ layers=false },title={ Text("只看这个收藏夹",style=MaterialTheme.typography.titleLarge) },text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Tag("全部收藏",collectionId==null,{ collectionId=null; layers=false })
            data.collections.forEach { c -> Tag(c.name,collectionId==c.id,{ collectionId=c.id; layers=false }) }
        }
    },confirmButton={ TextButton(onClick={ layers=false }) { Text("完成") } },containerColor=AtlasColors.Card)
    if(pending) AlertDialog(onDismissRequest={ pending=false },title={ Text("补全地点位置") },text={
        Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            unlocated.forEach { p -> TextButton(onClick={ locating=p; pending=false }) { Text("${p.name} · ${p.city}") } }
        }
    },confirmButton={ TextButton(onClick={ pending=false }) { Text("返回") } },containerColor=AtlasColors.Paper)
    locating?.let { p -> PoiPicker(controller.platform,p.city,p.name,emptyList(),{ poi ->
        if(controller.updatePlace(p.copy(address=poi.address,district=poi.district,latitude=poi.latitude,longitude=poi.longitude,
            gcjLatitude=poi.gcjLatitude,gcjLongitude=poi.gcjLongitude,poiId=poi.id))) { selectedId=p.id; locating=null }
    },{ locating=null }) }
}

@Composable
private fun MapTile(controller: AtlasController,z: Int,x: Int,y: Int,retry: Int,modifier: Modifier,onFailure: (Boolean)->Unit) {
    var bitmap by remember(z,x,y) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(z,x,y,retry) {
        var attempt=0
        while(isActive) {
            try { bitmap=decodeMapTile(controller.platform.transport.tile(z,x,y)); onFailure(false); break }
            catch(e: CancellationException) { throw e }
            catch(_: Exception) { onFailure(true); delay(listOf(2000L,5000L,10000L,20000L,30000L)[attempt.coerceAtMost(4)]); attempt++ }
        }
    }
    if(bitmap!=null) Image(bitmap!!,null,modifier)
    else Box(modifier.background(AtlasColors.SagePale.copy(alpha=.45f)))
}
