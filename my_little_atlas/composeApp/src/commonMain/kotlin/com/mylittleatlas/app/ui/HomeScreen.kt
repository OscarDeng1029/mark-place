package com.mylittleatlas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mylittleatlas.app.domain.*
import com.mylittleatlas.app.domain.Collection

@Composable
fun HomeScreen(data:AtlasData,onPlace:(Place)->Unit,onCollection:(String?)->Unit,onMap:()->Unit,onJournal:()->Unit,onSearch:()->Unit,onProfile:()->Unit,wide:Boolean) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=if(wide) 40.dp else 24.dp).padding(top=24.dp,bottom=28.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("My Little\nAtlas",style=MaterialTheme.typography.headlineLarge,lineHeight=35.sp,fontSize=32.sp)
                Row(Modifier.padding(top=11.dp).clickable(onClick=onMap),verticalAlignment=Alignment.CenterVertically) {
                    AtlasIcon(Glyph.PIN,size=12.dp,color=AtlasColors.Green);Spacer(Modifier.width(5.dp))
                    Text("${data.places.firstOrNull()?.city ?: "我的城市"}  /  我的城市收藏图鉴",fontSize=10.sp,color=AtlasColors.Muted)
                }
            }
            IconButton(Glyph.SEARCH,"搜索收藏",onSearch)
            Box(Modifier.padding(start=5.dp,top=6.dp).size(33.dp).clip(CircleShape).background(AtlasColors.SagePale).clickable(onClick=onProfile),contentAlignment=Alignment.Center) {
                Text(data.nickname.take(1),fontSize=13.sp,color=AtlasColors.Green)
            }
        }
        Spacer(Modifier.height(20.dp))
        if(data.demoContent) {
            Text("灵感样本  ·  地点与照片为演示内容",fontSize=9.sp,color=AtlasColors.Muted,modifier=Modifier.padding(bottom=10.dp))
        }
        if(wide) {
            Row(horizontalArrangement=Arrangement.spacedBy(32.dp),verticalAlignment=Alignment.CenterVertically) {
                HeroCard(data.places.firstOrNull(),onMap,Modifier.weight(1f).height(300.dp))
                Column(Modifier.weight(.7f)) {Handwritten("A personal atlas\nof places worth going.",size=29);Spacer(Modifier.height(24.dp));Text("把想去的地方，\n慢慢变成去过的地方。",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(18.dp));Text("从一间街角咖啡馆开始，\n收下生活里的小小心动。",color=AtlasColors.Muted,style=MaterialTheme.typography.bodyMedium)}
            }
        } else HeroCard(data.places.firstOrNull(),onMap,Modifier.fillMaxWidth().height(220.dp))
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(AtlasColors.Card.copy(alpha=.8f)).padding(vertical=17.dp)) {
            listOf(Triple(data.places.size,"收藏的去处",onCollection.let{{it(null)}}),
                Triple(data.places.count{data.isVisited(it.id)},"留下的足迹",onJournal),
                Triple(data.places.count{!data.isVisited(it.id)},"等一次出发",onMap)).forEachIndexed { index,item ->
                Column(Modifier.weight(1f).clickable(onClick=item.third),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(item.first.toString().padStart(2,'0'),fontFamily=FontFamily.Serif,fontSize=28.sp,color=AtlasColors.Ink)
                    Text(item.second,fontSize=10.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=5.dp))
                }
                if(index<2) Box(Modifier.width(1.dp).height(38.dp).align(Alignment.CenterVertically).background(AtlasColors.Line))
            }
        }
        Spacer(Modifier.height(25.dp))
        SectionTitle("我的收藏夹","全部",{onCollection(null)})
        if(data.collections.isEmpty()) {
            Text("把心动放进一个小小的主题里。",color=AtlasColors.Muted,fontSize=12.sp,modifier=Modifier.padding(vertical=15.dp))
        } else {
            val columns=if(wide) 4 else 2
            data.collections.take(4).chunked(columns).forEach { row ->
                Row(Modifier.padding(top=10.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    row.forEach { collection -> CollectionTile(collection,data,onClick={onCollection(collection.id)},modifier=Modifier.weight(1f)) }
                    repeat(columns-row.size){Spacer(Modifier.weight(1f))}
                }
            }
        }
        Spacer(Modifier.height(25.dp));SectionTitle("最近的心动","看看地图",onMap)
        data.places.take(3).forEach {place -> Spacer(Modifier.height(10.dp));PlaceRow(place,data,{onPlace(place)})}
        if(data.places.isEmpty()) EmptyPage("第一处心动，等你收藏","点下方的 ＋，开启你的城市图鉴。")
        Spacer(Modifier.height(30.dp));Handwritten("Little places, lovely memories. ♡",Modifier.align(Alignment.CenterHorizontally),size=17)
    }
}

@Composable
fun HeroCard(place:Place?,onClick:()->Unit,modifier:Modifier=Modifier) {
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(AtlasColors.PeachPale).clickable(onClick=onClick)) {
        Box(Modifier.fillMaxSize().padding(22.dp)) {
            Box(Modifier.align(Alignment.Center).fillMaxWidth(.87f).fillMaxHeight(.88f).rotate(-5f).background(AtlasColors.Line))
            Column(Modifier.align(Alignment.Center).fillMaxWidth(.9f).fillMaxHeight(.94f).rotate(-2f)
                .shadow(5.dp,RoundedCornerShape(2.dp)).background(AtlasColors.Card).padding(9.dp)) {
                Photo(place?.cover ?: "cafe",Modifier.fillMaxWidth().weight(1f))
                Row(Modifier.fillMaxWidth().padding(top=7.dp),verticalAlignment=Alignment.CenterVertically) {
                    Handwritten("A slow afternoon",Modifier.weight(1f),size=15)
                    AtlasIcon(Glyph.HEART,color=AtlasColors.Sage,size=19.dp)
                }
            }
            Tape(Modifier.align(Alignment.TopCenter).offset(y=(-5).dp).rotate(-6f))
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(end=10.dp,bottom=23.dp).rotate(-6f).background(AtlasColors.Card).padding(horizontal=14.dp,vertical=6.dp)) {
            Handwritten(place?.city?.let{if(it=="上海")"Shanghai" else it} ?: "My Atlas",size=16)
        }
        AtlasIcon(Glyph.SPARKLE,Modifier.align(Alignment.TopStart).padding(12.dp),color=AtlasColors.Peach,size=21.dp)
    }
}

@Composable
fun CollectionTile(collection:Collection,data:AtlasData,onClick:()->Unit,modifier:Modifier=Modifier) {
    val tint=when(collection.tint){"peach"->AtlasColors.PeachPale;"blue"->AtlasColors.BluePale;else->AtlasColors.SagePale}
    Column(modifier.clip(RoundedCornerShape(15.dp)).background(AtlasColors.Card).clickable(onClick=onClick).padding(8.dp)) {
        Box(Modifier.fillMaxWidth().height(105.dp).clip(RoundedCornerShape(10.dp)).background(tint)) {
            Photo(collection.cover,Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.TopEnd).padding(7.dp).background(AtlasColors.Card.copy(alpha=.9f),CircleShape).padding(5.dp)) {AtlasIcon(Glyph.FOLDER,size=13.dp,color=AtlasColors.Green)}
        }
        Text(collection.name,fontSize=13.sp,modifier=Modifier.padding(start=3.dp,top=10.dp),maxLines=1)
        Text("${data.countIn(collection)} 个去处",fontSize=10.sp,color=AtlasColors.Muted,modifier=Modifier.padding(start=3.dp,top=4.dp,bottom=5.dp))
    }
}
