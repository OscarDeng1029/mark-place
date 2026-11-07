package com.mylittleatlas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mylittleatlas.app.domain.*
import com.mylittleatlas.app.presentation.AtlasController

@Composable
fun CityAtlasV2(controller:AtlasController,onClose:()->Unit,onPlace:(Place)->Unit,onMap:(String)->Unit,onShare:(String)->Unit) {
    val data=controller.data
    val cities=data.places.map{it.city}.filter{it.isNotBlank()}.distinct()
    var city by remember{mutableStateOf(cities.firstOrNull())}
    val places=data.places.filter{it.city==city}
    AtlasFullPage("我的城市图鉴",onClose,actions={if(city!=null)IconButton(Glyph.SHARE,"分享当前城市图鉴",{onShare(city!!)})}) {
        if(cities.isEmpty())EmptyPage("城市图鉴，从一处心动开始","收藏一个地点，这座城就会出现在这里。")
        cities.chunked(3).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){row.forEach{c->Tag(c,c==city,{city=c})}}}
        if(city!=null){
            Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp))) {
                Photo(places.firstOrNull()?.cover ?: "city",Modifier.fillMaxSize())
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(AtlasColors.Ink.copy(alpha=.45f)).padding(15.dp)){Text(city!!,style=MaterialTheme.typography.headlineMedium,color=AtlasColors.Card);Text("慢慢喜欢你",fontSize=12.sp,color=AtlasColors.Card)}
            }
            AtlasMetrics(listOf(places.size to "收藏地点",places.count{data.isVisited(it.id)} to "去过的地方",places.count{!data.isVisited(it.id)} to "还想去"))
            PrimaryButton("在真实地图上看足迹",{onMap(city!!)},Modifier.fillMaxWidth(),Glyph.MAP)
            Text("在这座城，收集到的日常",style=MaterialTheme.typography.titleLarge)
            places.groupingBy{it.kind.title}.eachCount().entries.chunked(3).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){row.forEach{Tag(it.key+" "+it.value)}}}
            places.forEach{place->PlaceRow(place,data,{onPlace(place)})}
        }
    }
}
