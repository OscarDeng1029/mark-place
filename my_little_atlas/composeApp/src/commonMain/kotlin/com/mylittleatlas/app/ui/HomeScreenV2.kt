package com.mylittleatlas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mylittleatlas.app.domain.*
import com.mylittleatlas.app.domain.Collection

@Composable
fun HomeScreenV2(data:AtlasData,onPlace:(Place)->Unit,onCollection:(String?)->Unit,onMap:()->Unit,onJournal:()->Unit,onSearch:()->Unit,onProfile:()->Unit,wide:Boolean) {
    var filter by remember { mutableStateOf(PlaceFilter.ALL) }
    var city by remember { mutableStateOf<String?>(data.preferredCity.takeIf{it.isNotBlank()}) }
    var choosingCity by remember { mutableStateOf(false) }
    val places=data.places.filter { city==null || it.city==city }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal=if(wide)40.dp else 18.dp).padding(top=8.dp,bottom=25.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("My Little",style=MaterialTheme.typography.headlineMedium,lineHeight=27.sp,fontSize=27.sp)
                Text("Atlas ✧",style=MaterialTheme.typography.headlineMedium,lineHeight=27.sp,fontSize=27.sp)
            }
            Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable{choosingCity=true}.padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                AtlasIcon(Glyph.PIN,size=13.dp,color=AtlasColors.Green)
                Text(city ?: "全部城市",fontSize=11.sp,color=AtlasColors.Ink,modifier=Modifier.padding(start=4.dp))
                Text("⌄",fontSize=13.sp,modifier=Modifier.padding(start=7.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(11.dp))
            .background(AtlasColors.Card).border(1.dp,AtlasColors.Line,RoundedCornerShape(11.dp))
            .clickable(onClick=onSearch).padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically) {
            AtlasIcon(Glyph.SEARCH,size=17.dp,color=AtlasColors.Muted)
            Text("搜索我的收藏",fontSize=12.sp,color=AtlasColors.Muted,modifier=Modifier.padding(start=10.dp))
        }
        Spacer(Modifier.height(16.dp))
        HeroCardV2(places.firstOrNull(),onMap,Modifier.fillMaxWidth().height(if(wide)240.dp else 177.dp))
        Spacer(Modifier.height(17.dp))
        AtlasMetrics(listOf(places.size to "收藏地点",places.count{data.isVisited(it.id)} to "去过的地方",
            places.count{!data.isVisited(it.id)} to "还想去"),listOf({onCollection(null)},onJournal,onMap))
        Spacer(Modifier.height(17.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            PlaceFilter.entries.forEach { f ->
                val count=places.count{data.matches(it,f)}
                Tag(if(f==PlaceFilter.ALL)f.title else f.title+" "+count,f==filter,{filter=f},AtlasColors.Card)
            }
        }
        Spacer(Modifier.height(19.dp))
        SectionTitle("我的收藏夹","全部",{onCollection(null)})
        if(data.collections.isEmpty()) {
            Text("把心动放进一个小小的主题里。",color=AtlasColors.Muted,fontSize=12.sp,modifier=Modifier.padding(vertical=15.dp))
        } else {
            val columns=if(wide)4 else 3
            Row(Modifier.padding(top=6.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                data.collections.take(columns).forEach { c -> CollectionTileV2(c,data,{onCollection(c.id)},Modifier.weight(1f)) }
                repeat((columns-data.collections.size).coerceAtLeast(0)){Spacer(Modifier.weight(1f))}
            }
        }
        Spacer(Modifier.height(17.dp))
        SectionTitle("最近收藏","查看全部",onSearch)
        val filtered=places.filter{data.matches(it,filter)}
        filtered.take(5).forEach{p->Spacer(Modifier.height(8.dp));PlaceRow(p,data,{onPlace(p)})}
        if(filtered.isEmpty()) EmptyPage("下一处心动，等你收藏",if(filter==PlaceFilter.ALL)"点下方 ＋，把想去的地方收好。" else "这里还没有"+filter.title+"的地点。")
        if(data.demoContent) Text("示例地点与照片仅作演示",fontSize=9.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=15.dp).align(Alignment.CenterHorizontally))
    }
    if(choosingCity) AlertDialog(onDismissRequest={choosingCity=false},title={Text("把目光放在哪座城？",style=MaterialTheme.typography.titleLarge)},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            TextButton(onClick={city=null;choosingCity=false}){Text("全部城市") }
            data.places.map{it.city}.filter{it.isNotBlank()}.distinct().forEach{c->TextButton(onClick={city=c;choosingCity=false}){Text(c)}}
        }},confirmButton={TextButton(onClick={choosingCity=false}){Text("完成")}},containerColor=AtlasColors.Card)
}

@Composable
fun AtlasMetrics(values:List<Pair<Int,String>>,actions:List<()->Unit> = emptyList()) {
    Row(Modifier.fillMaxWidth().padding(vertical=4.dp)) {
        values.forEachIndexed { i,(n,label)->
            Column(Modifier.weight(1f).then(if(i<actions.size)Modifier.clickable(onClick=actions[i]) else Modifier).padding(vertical=3.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Text(n.toString(),fontFamily=FontFamily.Serif,fontSize=25.sp,color=AtlasColors.Ink)
                Text(label,fontSize=10.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=5.dp))
            }
            if(i<values.lastIndex) Box(Modifier.width(1.dp).height(37.dp).align(Alignment.CenterVertically).background(AtlasColors.Line))
        }
    }
}

@Composable
fun HeroCardV2(place:Place?,onClick:()->Unit,modifier:Modifier=Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFFECE7D9)).clickable(onClick=onClick)) {
        Column(Modifier.align(Alignment.TopStart).padding(start=17.dp,top=23.dp).fillMaxWidth(.53f)) {
            Text("收集生活里的",style=MaterialTheme.typography.titleLarge,lineHeight=26.sp,fontSize=21.sp)
            Text("小小心动。",style=MaterialTheme.typography.titleLarge,lineHeight=26.sp,fontSize=21.sp)
            Text("把想去，慢慢变成去过。",fontSize=10.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=8.dp))
        }
        PaperNote(Modifier.align(Alignment.CenterEnd).padding(end=10.dp).size(142.dp,145.dp).rotate(-6f)) {}
        PolaroidPhoto(place?.cover ?: "city",Modifier.align(Alignment.CenterEnd).padding(end=15.dp).size(137.dp,155.dp),caption="a little city walk ♡",rotation=7f)
        Handwritten("♧  good things take time",Modifier.align(Alignment.BottomStart).padding(start=17.dp,bottom=11.dp),size=11)
    }
}

@Composable
fun CollectionTileV2(collection:Collection,data:AtlasData,onClick:()->Unit,modifier:Modifier=Modifier) {
    Column(modifier.clip(RoundedCornerShape(8.dp)).background(AtlasColors.Card)
        .border(1.dp,AtlasColors.Line,RoundedCornerShape(8.dp)).clickable(onClick=onClick)) {
        Photo(collection.cover,Modifier.fillMaxWidth().height(91.dp))
        Text(collection.name,fontSize=11.sp,modifier=Modifier.padding(start=9.dp,end=7.dp,top=9.dp),maxLines=1)
        Text(data.countIn(collection).toString()+" 个地点",fontSize=10.sp,color=AtlasColors.Muted,modifier=Modifier.padding(start=9.dp,top=4.dp,bottom=10.dp))
    }
}
