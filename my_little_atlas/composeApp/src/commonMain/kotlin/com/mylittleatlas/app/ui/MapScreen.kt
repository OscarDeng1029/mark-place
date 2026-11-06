package com.mylittleatlas.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mylittleatlas.app.domain.*

@Composable
fun MapScreen(data:AtlasData,onPlace:(Place)->Unit,onList:()->Unit,wide:Boolean) {
    var filter by remember{mutableStateOf(PlaceFilter.ALL)}
    var selectedId by remember{mutableStateOf<String?>(null)}
    var collectionId by remember{mutableStateOf<String?>(null)}
    var layers by remember{mutableStateOf(false)}
    val places=data.filterPlaces(filter,collectionId=collectionId)
    val plotted=places.filter{it.latitude!=null&&it.longitude!=null}
    val unlocated=places.size-plotted.size
    val selected=places.find{it.id==selectedId} ?: plotted.firstOrNull()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fullHeight=maxHeight
        AtlasMapDrawing(Modifier.fillMaxSize())
        Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=22.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically){AtlasIcon(Glyph.PIN,size=18.dp,color=AtlasColors.Green);Spacer(Modifier.width(7.dp));Text("我的${data.places.firstOrNull()?.city ?: "城市"}地图",style=MaterialTheme.typography.titleLarge)}
                IconButton(Glyph.LIST,"查看地点列表",onList,filled=true)
            }
            FilterRow(filter,{filter=it;selectedId=null})
            Row(Modifier.background(AtlasColors.Card.copy(alpha=.94f),RoundedCornerShape(10.dp)).padding(horizontal=11.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).background(AtlasColors.Sage,CircleShape));Spacer(Modifier.width(6.dp))
                Text("离线布局示意 · ${places.size} 个私人收藏${if(unlocated>0)" · $unlocated 个待定位" else ""}",fontSize=9.sp,color=AtlasColors.Muted)
            }
        }
        val coordinates=listOf(.30f to .34f,.56f to .30f,.74f to .43f,.43f to .51f,.21f to .61f,.72f to .64f,.49f to .70f,.83f to .33f)
        plotted.forEachIndexed{index,place->
            val xy=coordinates[index%coordinates.size]
            val isSelected=place.id==selected?.id
            Box(Modifier.offset(x=maxWidth*xy.first-20.dp,y=fullHeight*xy.second-25.dp)
                .size(if(isSelected)54.dp else 39.dp).clip(RoundedCornerShape(18.dp))
                .background(if(isSelected) AtlasColors.Card else if(data.isVisited(place.id)) AtlasColors.Green else AtlasColors.Peach)
                .clickable{selectedId=place.id},contentAlignment=Alignment.Center) {
                if(isSelected) Photo(place.cover,Modifier.padding(5.dp).fillMaxSize().clip(RoundedCornerShape(13.dp)))
                else AtlasIcon(Glyph.PIN,color=AtlasColors.Card,size=23.dp)
            }
        }
        Column(Modifier.align(Alignment.CenterEnd).padding(end=18.dp),verticalArrangement=Arrangement.spacedBy(11.dp)) {
            IconButton(Glyph.LAYERS,"筛选收藏夹",{layers=true},filled=true)
            IconButton(Glyph.NAVIGATE,"返回收藏地图中心",{selectedId=null;collectionId=null;filter=PlaceFilter.ALL},filled=true)
        }
        Column(Modifier.align(Alignment.BottomCenter).widthIn(max=650.dp).fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
            if(selected!=null) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AtlasColors.Card).clickable{onPlace(selected)}.padding(12.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    Photo(selected.cover,Modifier.size(75.dp).clip(RoundedCornerShape(12.dp)))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text(selected.name,style=MaterialTheme.typography.titleMedium)
                        Text("${selected.city} · ${selected.district}",fontSize=11.sp,color=AtlasColors.Muted)
                        Text(if(data.isVisited(selected.id))"已去过 · 回看这段小记" else "想去 · 等一次刚好的出发",fontSize=10.sp,color=AtlasColors.Green)
                    }
                    AtlasIcon(Glyph.ARROW,size=18.dp,color=AtlasColors.Green)
                }
            } else {
                Column(Modifier.fillMaxWidth().background(AtlasColors.Card,RoundedCornerShape(20.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(if(places.isEmpty())"这张地图，等你慢慢填满" else "${places.size} 个收藏还没有地图坐标",style=MaterialTheme.typography.titleMedium)
                    Text(if(places.isEmpty())"添加一个心动去处，开启新的探索。" else "打开地点可修改地址，或通过系统地图搜索导航。",fontSize=12.sp,color=AtlasColors.Muted)
                    TextButton(onClick=onList){Text("查看收藏列表")}
                }
            }
            Text("底图与标记位置仅展示交互，非真实地理位置。",fontSize=8.sp,color=AtlasColors.Muted,modifier=Modifier.align(Alignment.CenterHorizontally))
        }
    }
    if(layers) AlertDialog(onDismissRequest={layers=false},title={Text("只看这个收藏夹",style=MaterialTheme.typography.titleLarge)},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Tag("全部收藏",collectionId==null,{collectionId=null;layers=false})
            data.collections.forEach{collection->Tag(collection.name,collectionId==collection.id,{collectionId=collection.id;layers=false})}
        }
    },confirmButton={TextButton(onClick={layers=false}){Text("完成")}},containerColor=AtlasColors.Card)
}

/** Decorative offline canvas. Real map integration lives behind the platform boundary. */
@Composable
fun AtlasMapDrawing(modifier:Modifier=Modifier) {
    Canvas(modifier.background(Color(0xFFECEADD))) {
        val w=size.width;val h=size.height
        fun area(color:Color,vararg points:Pair<Float,Float>) {
            val p=Path().apply{moveTo(points[0].first*w,points[0].second*h);points.drop(1).forEach{lineTo(it.first*w,it.second*h)};close()}
            drawPath(p,color)
        }
        area(Color(0xFFDAE3CD),.0f to .12f,.23f to .10f,.28f to .24f,.04f to .29f)
        area(Color(0xFFD5E1C8),.59f to .36f,.86f to .31f,.90f to .42f,.65f to .49f)
        area(Color(0xFFDAE3CD),.05f to .62f,.30f to .59f,.33f to .71f,.04f to .75f)
        area(Color(0xFFDFE5D0),.63f to .72f,.94f to .64f,.99f to .82f,.73f to .88f)
        val river=Path().apply{moveTo(w*.91f,-20f);cubicTo(w*.64f,h*.22f,w*1.25f,h*.35f,w*.84f,h*.53f);cubicTo(w*.61f,h*.64f,w*.95f,h*.80f,w*.70f,h*1.1f)}
        drawPath(river,Color(0xFFC5D8DC),style=Stroke(w*.095f))
        for(i in -3..12) {
            val x=i*w*.16f
            val p=Path().apply{moveTo(x,-h*.2f);lineTo(x-w*.57f,h*1.2f)}
            drawPath(p,Color(0xFFDAD6C7),style=Stroke(8.dp.toPx()))
            drawPath(p,Color(0xFFFFFCF2),style=Stroke(5.dp.toPx()))
        }
        for(i in 0..14) {
            val y=i*h*.085f
            val p=Path().apply{moveTo(-w*.1f,y+h*.09f);lineTo(w*1.1f,y-h*.09f)}
            drawPath(p,Color(0xFFDAD6C7),style=Stroke(7.dp.toPx()))
            drawPath(p,Color(0xFFFFFCF2),style=Stroke(4.dp.toPx()))
        }
        val boulevard=Path().apply{moveTo(-20f,h*.81f);cubicTo(w*.4f,h*.74f,w*.30f,h*.32f,w*1.1f,h*.21f)}
        drawPath(boulevard,Color(0xFFE0C898),style=Stroke(12.dp.toPx(),cap=StrokeCap.Round))
        drawPath(boulevard,Color(0xFFF9EAC7),style=Stroke(8.dp.toPx(),cap=StrokeCap.Round))
    }
}
