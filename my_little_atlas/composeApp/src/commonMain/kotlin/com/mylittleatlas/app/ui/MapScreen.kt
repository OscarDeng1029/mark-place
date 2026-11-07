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
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.border
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
fun MapScreen(controller: AtlasController, onPlace: (Place)->Unit, onList: ()->Unit, wide: Boolean,initialFolder:String?=null,initialCity:String?=null) {
    val data = controller.data
    var filter by remember { mutableStateOf(PlaceFilter.ALL) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var collectionId by remember { mutableStateOf(initialFolder) }
    var city by remember { mutableStateOf(initialCity) }
    var kind by remember { mutableStateOf<PlaceKind?>(null) }
    var listMode by remember { mutableStateOf(false) }
    var permission by remember { mutableStateOf(false) }
    var locatingUser by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var userPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var layers by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf<Place?>(null) }
    var query by remember { mutableStateOf("") }
    val places = data.filterPlaces(filter,query,collectionId=collectionId).filter{(city==null||it.city==city)&&(kind==null||it.kind==kind)}
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
        userPoint?.let{point->val projected=MapGeometry.project(point,zoom)
            val px=((projected.x-origin.x)*tilePx+viewport.width/2).toFloat()/density
            val py=((projected.y-origin.y)*tilePx+viewport.height/2).toFloat()/density
            Box(Modifier.offset(x=(px-8).dp,y=(py-8).dp).size(16.dp).background(AtlasColors.Blue,CircleShape).border(3.dp,AtlasColors.Card,CircleShape).semantics{contentDescription="我的当前位置"})
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
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(5.dp)).background(AtlasColors.Card)
                        .border(if(isSelected)2.dp else 1.dp,if(data.isVisited(place.id))AtlasColors.Green else AtlasColors.Peach,RoundedCornerShape(5.dp)),contentAlignment=Alignment.Center) {
                        if(isSelected) Photo(place.cover,Modifier.padding(4.dp).fillMaxSize().clip(RoundedCornerShape(2.dp)))
                        else AtlasIcon(if(data.isVisited(place.id))Glyph.CHECK else Glyph.BOOKMARK,color=if(data.isVisited(place.id))AtlasColors.Green else AtlasColors.Peach,size=21.dp)
                    }
                    Box(Modifier.width(2.dp).height(5.dp).background(AtlasColors.Green))
                    Box(Modifier.size(4.dp).background(AtlasColors.Green,CircleShape))
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=22.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                OutlinedTextField(query,{query=it;selectedId=null},Modifier.weight(1f).height(52.dp),
                    placeholder={Text("搜索我的地点",fontSize=12.sp)},leadingIcon={AtlasIcon(Glyph.SEARCH,size=17.dp,color=AtlasColors.Muted)},
                    singleLine=true,shape=RoundedCornerShape(11.dp),colors=OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor=AtlasColors.Card,focusedContainerColor=AtlasColors.Card,
                        unfocusedBorderColor=AtlasColors.Line,focusedBorderColor=AtlasColors.Sage))
                Spacer(Modifier.width(10.dp)); IconButton(if(listMode)Glyph.MAP else Glyph.LIST,"切换地图列表",{listMode=!listMode},filled=true)
            }
            FilterRow(filter,{ filter=it; selectedId=null })
            Row(Modifier.background(AtlasColors.Card.copy(alpha=.96f),RoundedCornerShape(10.dp)).padding(horizontal=11.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).background(AtlasColors.Sage,CircleShape)); Spacer(Modifier.width(6.dp))
                Text("${plotted.size} 处已定位${if(unlocated.isNotEmpty()) " · ${unlocated.size} 处待定位" else ""}${if(sampleCount>0) " · 示例不标注" else ""}",fontSize=10.sp,color=AtlasColors.Muted)
                if(unlocated.isNotEmpty()) Text("  补全",fontSize=10.sp,color=AtlasColors.Green,modifier=Modifier.clickable { pending=true })
            }
        }
        Column(Modifier.align(Alignment.TopEnd).padding(end=18.dp,top=162.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            IconButton(Glyph.LAYERS,"筛选收藏夹",{ layers=true },filled=true)
            IconButton(Glyph.PLUS,"放大地图",{ zoom=(zoom+1).coerceAtMost(18) },filled=true)
            Surface(Modifier.size(42.dp).clickable { zoom=(zoom-1).coerceAtLeast(3) }.semantics { contentDescription="缩小地图" },shape=CircleShape,color=AtlasColors.Card) {
                Box(contentAlignment=Alignment.Center) { Text("−",fontSize=24.sp,color=AtlasColors.Green) }
            }
            if(locatingUser)CircularProgressIndicator(Modifier.size(34.dp),color=AtlasColors.Green,strokeWidth=2.dp)
            else IconButton(Glyph.NAVIGATE,"使用我的当前位置",{ permission=true },filled=true)
        }
        Column(Modifier.align(Alignment.BottomCenter).widthIn(max=650.dp).fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
            if(failures.values.any { it }) Row(Modifier.fillMaxWidth().background(AtlasColors.Card,RoundedCornerShape(10.dp)).padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("部分地图未加载，请检查网络。",fontSize=11.sp,color=AtlasColors.Muted,modifier=Modifier.weight(1f))
                TextButton(onClick={ retry++ }) { Text("重试",fontSize=11.sp) }
            }
            if(listMode)Column(Modifier.fillMaxWidth().heightIn(max=330.dp).background(AtlasColors.Card,RoundedCornerShape(12.dp)).verticalScroll(rememberScrollState()).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("当前筛选 · "+places.size+" 处",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f));IconButton(Glyph.CLOSE,"收起列表",{listMode=false})}
                places.forEach{place->PlaceRow(place,data,{onPlace(place)})}
                if(places.isEmpty())Text("没有符合当前条件的地点。",fontSize=12.sp,color=AtlasColors.Muted)
            } else if(selected!=null) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(AtlasColors.Card).padding(12.dp)) {
                Row(Modifier.fillMaxWidth().clickable { onPlace(selected) },
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    Photo(selected.cover,Modifier.size(62.dp).clip(RoundedCornerShape(7.dp)))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text(selected.name,style=MaterialTheme.typography.titleMedium)
                        Text(selected.address,fontSize=11.sp,color=AtlasColors.Muted,maxLines=2)
                        Text(if(data.isVisited(selected.id)) "已去过 · 回看这段小记" else "想去 · 等一次刚好的出发",fontSize=10.sp,color=AtlasColors.Green)
                    }
                    AtlasIcon(Glyph.ARROW,size=18.dp,color=AtlasColors.Green)
                }
                Row(Modifier.fillMaxWidth().padding(top=10.dp),verticalAlignment=Alignment.CenterVertically) {
                    Handwritten(selected.reason.ifBlank{"等一次刚好的出发。"},Modifier.weight(1f),size=11)
                    PrimaryButton("去这里",{controller.navigate(selected)},icon=Glyph.NAVIGATE)
                }
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
    if(permission)AlertDialog(onDismissRequest={permission=false},title={Text("看看身边的下一站")},text={Column{
        Text("开启定位后，地图会移动到你的位置，方便查看附近的收藏。仅在你点击时获取位置，不会自动记录到访。",fontSize=13.sp,lineHeight=23.sp)
        Text("也可以跳过，继续手动浏览城市与收藏。",fontSize=11.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=12.dp))
    }},confirmButton={TextButton(onClick={permission=false;locatingUser=true;controller.platform.locate{point,error->locatingUser=false;locationError=error;point?.let{val gcj=MapGeometry.gcj(it);userPoint=gcj;center=gcj;zoom=15}}}){Text("使用当前位置")}},dismissButton={TextButton(onClick={permission=false}){Text("先手动看看")}},containerColor=AtlasColors.Card)
    locationError?.let{error->AlertDialog(onDismissRequest={locationError=null},title={Text("可以继续浏览地图")},text={Text(error,fontSize=13.sp)},confirmButton={TextButton(onClick={locationError=null}){Text("继续看看")}},containerColor=AtlasColors.Card)}
    if(layers) AlertDialog(onDismissRequest={ layers=false },title={ Text("筛选我的地图",style=MaterialTheme.typography.titleLarge) },text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("城市",fontSize=12.sp,color=AtlasColors.Muted)
            Tag("全部城市",city==null,{city=null});data.places.map{it.city}.distinct().forEach{c->Tag(c,city==c,{city=c})}
            Text("地点分类",fontSize=12.sp,color=AtlasColors.Muted)
            Tag("全部分类",kind==null,{kind=null});PlaceKind.entries.forEach{k->Tag(k.title,kind==k,{kind=k})}
            Text("收藏夹",fontSize=12.sp,color=AtlasColors.Muted)
            Tag("全部收藏",collectionId==null,{ collectionId=null })
            data.collections.forEach { c -> Tag(c.name,collectionId==c.id,{ collectionId=c.id }) }
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
