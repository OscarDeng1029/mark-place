package com.mylittleatlas.app.ui

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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mylittleatlas.app.domain.*
import com.mylittleatlas.app.presentation.AtlasController

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollectionsScreen(controller:AtlasController,collectionId:String?,onCollection:(String?)->Unit,onPlace:(Place)->Unit,
    search:String,onSearch:(String)->Unit,searching:Boolean,onSearching:(Boolean)->Unit,onAdd:()->Unit,wide:Boolean) {
    var filter by remember{mutableStateOf(PlaceFilter.ALL)}
    var newCollection by remember{mutableStateOf(false)}
    var showFolders by remember{mutableStateOf(collectionId==null)}
    val data=controller.data
    val collection=data.collections.find{it.id==collectionId}
    val places=data.filterPlaces(filter,search,collectionId)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom=24.dp)) {
        PageHeader("COLLECT LITTLE JOYS",collection?.name ?: "我的私藏",collection?.subtitle ?: "每一次收藏，都是对生活的小小期待。") {
            IconButton(Glyph.PLUS,"新建收藏夹",{newCollection=true})
        }
        Column(Modifier.padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(search,onSearch,Modifier.fillMaxWidth(),placeholder={Text("找找那家心动的小店…",fontSize=13.sp)},
                leadingIcon={AtlasIcon(Glyph.SEARCH,size=18.dp,color=AtlasColors.Muted)},singleLine=true,shape=RoundedCornerShape(15.dp),
                colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=AtlasColors.Line,focusedBorderColor=AtlasColors.Sage))
            if(collectionId!=null) TextButton(onClick={onCollection(null);showFolders=true},contentPadding=PaddingValues(0.dp)) {
                AtlasIcon(Glyph.BACK,size=15.dp);Spacer(Modifier.width(5.dp));Text("所有收藏夹",fontSize=12.sp)
            }
            if(collectionId==null && search.isBlank()) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("按主题收好",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                    IconButton(if(showFolders) Glyph.LIST else Glyph.FOLDER,"切换收藏夹显示",{showFolders=!showFolders})
                }
                if(showFolders) data.collections.chunked(if(wide) 4 else 2).forEach {row ->
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        row.forEach{CollectionTile(it,data,{onCollection(it.id)},Modifier.weight(1f))}
                        repeat((if(wide)4 else 2)-row.size){Spacer(Modifier.weight(1f))}
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(if(search.isBlank()) "${places.size} 个心动去处" else "找到 ${places.size} 个去处",style=MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f));Text("只属于你",fontSize=10.sp,color=AtlasColors.Muted)
            }
            FilterRow(filter,{filter=it})
            places.forEach {PlaceRow(it,data,{onPlace(it)})}
            if(places.isEmpty()) EmptyPage("这里还空着呢",if(search.isNotBlank())"换个名字或区域再找找。" else "把下一次心动，好好收下来。",if(search.isBlank())"收藏新去处" else null,onAdd)
        }
    }
    if(newCollection) TextEditDialog("给收藏夹取个名字",onConfirm={if(controller.addCollection(it))newCollection=false},onDismiss={newCollection=false})
}

@Composable
fun JournalScreen(data:AtlasData,onPlace:(Place)->Unit,onExplore:()->Unit,wide:Boolean,onVisit:(Visit)->Unit = {},onCities:()->Unit={}) {
    var city by remember{mutableStateOf<String?>(null)}
    var month by remember{mutableStateOf<String?>(null)}
    var selecting by remember{mutableStateOf("")}
    val visits=data.visits.filter{v->(month==null||v.date.startsWith(month!!))&&(city==null||data.places.find{it.id==v.placeId}?.city==city)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom=30.dp)) {
        PageHeader("MY LITTLE JOURNAL","我的足迹","把每一次抵达，留成自己的一页。"){IconButton(Glyph.MAP,"城市图鉴",onCities)}
        Column(Modifier.padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            if(data.visits.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().background(AtlasColors.PeachPale,RoundedCornerShape(18.dp)).padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
                    AtlasIcon(Glyph.CAMERA,color=AtlasColors.Green,size=28.dp);Spacer(Modifier.width(14.dp))
                    Column {Text("${data.visits.size} 段小小的回忆",style=MaterialTheme.typography.titleMedium);Text("每一次抵达，都值得被记住。",fontSize=11.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=5.dp))}
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Tag(city ?: "全部城市",city!=null,{selecting="city"});Tag(month ?: "全部月份",month!=null,{selecting="month"})}
            visits.sortedByDescending{it.date}.forEach {visit ->
                data.places.find{it.id==visit.placeId}?.let {place ->
                    Row(horizontalArrangement=Arrangement.spacedBy(15.dp),modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.width(37.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(visit.date.takeLast(2),fontFamily=FontFamily.Serif,fontSize=27.sp)
                            Text(visit.date.take(7).replace('-','.'),fontSize=8.sp,color=AtlasColors.Muted)
                            Box(Modifier.padding(top=14.dp).width(1.dp).height(190.dp).background(AtlasColors.Line))
                        }
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(AtlasColors.Card).clickable{onVisit(visit)}.padding(10.dp)) {
                            Box(Modifier.fillMaxWidth().height(if(wide) 240.dp else 180.dp)) {
                                if(visit.photoPaths.isNotEmpty()) com.mylittleatlas.app.platform.LocalPhoto(visit.photoPaths.first(),Modifier.fillMaxSize().clip(RoundedCornerShape(3.dp)))
                                else Photo(place.cover,Modifier.fillMaxSize().clip(RoundedCornerShape(3.dp)))
                                Tape(Modifier.align(Alignment.TopCenter).offset(y=(-8).dp).rotate(-5f))
                            }
                            Row(Modifier.padding(top=12.dp),verticalAlignment=Alignment.CenterVertically) {
                                Text(place.name,style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                                AtlasIcon(Glyph.HEART,color=AtlasColors.Peach,size=17.dp)
                            }
                            Text(visit.note.ifBlank{"在这里，留下一段属于自己的时光。"},fontSize=12.sp,lineHeight=22.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=6.dp,bottom=10.dp))
                        }
                    }
                }
            }
            if(data.visits.isEmpty()) EmptyPage("回忆，从一次出发开始","选一家想去的小店，留下你的第一篇手账。","看看我的收藏",onExplore)
            else if(visits.isEmpty())EmptyPage("这段时间，还没有留下足迹","换一个城市或月份，再回看其他记忆。")
            Handwritten("And so the little adventures begin. ♡",Modifier.align(Alignment.CenterHorizontally),size=16)
        }
    }
    if(selecting.isNotBlank())AlertDialog(onDismissRequest={selecting=""},title={Text(if(selecting=="city")"按城市回看" else "按月份回看")},text={Column(Modifier.heightIn(max=380.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Tag("全部",if(selecting=="city")city==null else month==null,{if(selecting=="city")city=null else month=null;selecting=""})
        (if(selecting=="city")data.places.map{it.city}.distinct() else data.visits.map{it.date.take(7)}.distinct().sortedDescending()).forEach{value->Tag(value,if(selecting=="city")city==value else month==value,{if(selecting=="city")city=value else month=value;selecting=""})}
    }},confirmButton={TextButton(onClick={selecting=""}){Text("收好")}},containerColor=AtlasColors.Card)
}

@Composable
fun ProfileScreen(controller:AtlasController,onShare:()->Unit,onJournal:()->Unit) {
    val data=controller.data
    var rename by remember{mutableStateOf(false)}
    var restore by remember{mutableStateOf(false)}
    var clear by remember{mutableStateOf(false)}
    var about by remember{mutableStateOf(false)}
    var badgeIndex by remember{mutableStateOf<Int?>(null)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),horizontalAlignment=Alignment.Start) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("A PERSONAL ATLAS",fontSize=10.sp,letterSpacing=2.sp,color=AtlasColors.Muted,modifier=Modifier.weight(1f))
            IconButton(Glyph.SETTINGS,"关于应用",{about=true})
        }
        Box(Modifier.padding(top=10.dp).fillMaxWidth().height(160.dp)) {
            Photo(data.places.firstOrNull()?.cover ?: "city",Modifier.fillMaxSize().clip(RoundedCornerShape(11.dp)))
            Tape(Modifier.align(Alignment.TopStart).offset(x=20.dp,y=(-5).dp).rotate(-5f))
            TextButton(onClick={rename=true},modifier=Modifier.align(Alignment.BottomEnd)) { Text("编辑资料",fontSize=10.sp,color=AtlasColors.Card) }
        }
        Box(Modifier.offset(y=(-30).dp).padding(start=14.dp).size(64.dp).clip(CircleShape).background(AtlasColors.SagePale).clickable{rename=true},contentAlignment=Alignment.Center) {
            Text(data.nickname.take(1),style=MaterialTheme.typography.headlineLarge,color=AtlasColors.Green)
        }
        Text(data.nickname+"的城市手账",style=MaterialTheme.typography.titleLarge,modifier=Modifier.offset(y=(-14).dp).clickable{rename=true})
        Text("收集生活，也收集自己。",fontSize=12.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=6.dp))
        Spacer(Modifier.height(25.dp))
        Row(Modifier.fillMaxWidth().background(AtlasColors.Card,RoundedCornerShape(20.dp)).padding(vertical=21.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
            listOf(data.places.size to "收藏",data.places.count{data.isVisited(it.id)} to "去过",data.places.map{it.city}.distinct().size to "城市").forEach{(n,label)->
                Column(horizontalAlignment=Alignment.CenterHorizontally){Text(n.toString(),fontFamily=FontFamily.Serif,fontSize=29.sp);Text(label,fontSize=11.sp,color=AtlasColors.Muted)}
            }
        }
        Spacer(Modifier.height(20.dp))
        SectionTitle("我的小小成就","勋章墙",{badgeIndex=0})
        Row(Modifier.fillMaxWidth().padding(top=13.dp,bottom=20.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
            listOf("城市探索家","咖啡爱好者","留住光影").forEachIndexed { index,label ->
                Column(Modifier.weight(1f).clickable{badgeIndex=index},horizontalAlignment=Alignment.CenterHorizontally) {
                    ScrapbookBadge(index,label,Modifier.size(66.dp))
                    Text(label,fontSize=10.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=8.dp))
                }
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AtlasColors.SagePale).padding(18.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically){AtlasIcon(Glyph.SPARKLE,color=AtlasColors.Green);Spacer(Modifier.width(10.dp));Text("我的城市护照",style=MaterialTheme.typography.titleLarge)}
            Text("让每一段小小的探索，都有自己的模样。",fontSize=12.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=9.dp,bottom=18.dp))
            PrimaryButton("生成我的图鉴",onShare,Modifier.fillMaxWidth(),Glyph.SHARE)
        }
        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AtlasColors.Card)) {
            SettingRow(Glyph.HEART,"足迹日记","留住去过的地方和那一天的心情",onJournal)
            ThinDivider(Modifier.padding(horizontal=17.dp))
            SettingRow(Glyph.EDIT,"修改昵称","你的图鉴署名"){rename=true}
            ThinDivider(Modifier.padding(horizontal=17.dp))
            SettingRow(Glyph.DOWNLOAD,"导出备份","地点、收藏夹与文字记录"){controller.exportBackup()}
            ThinDivider(Modifier.padding(horizontal=17.dp))
            SettingRow(Glyph.FOLDER,"恢复备份","从 JSON 文件恢复"){restore=true}
            if(data.demoContent) {ThinDivider(Modifier.padding(horizontal=17.dp));SettingRow(Glyph.CLOSE,"开始我的空白图鉴","移除示例地点与演示记录"){clear=true}}
        }
        Spacer(Modifier.height(24.dp));Row(verticalAlignment=Alignment.CenterVertically){AtlasIcon(Glyph.LOCK,size=14.dp,color=AtlasColors.Sage);Spacer(Modifier.width(7.dp));Text("默认私密 · 记录保存在此设备",fontSize=11.sp,color=AtlasColors.Muted)}
        Handwritten("Collect a better life ♡",Modifier.padding(top=24.dp),size=21)
        Text("My Little Atlas  ·  0.1.0",fontSize=9.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=12.dp))
    }
    badgeIndex?.let { index ->
        val count=when(index){0->data.places.filter{data.isVisited(it.id)}.map{it.city}.distinct().size;1->data.places.count{it.kind==PlaceKind.COFFEE&&data.isVisited(it.id)};else->data.visits.count{it.photoPaths.isNotEmpty()}}
        AlertDialog(onDismissRequest={badgeIndex=null},title={Text(listOf("城市探索家","咖啡爱好者","留住光影")[index])},
            text={Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth()){
                ScrapbookBadge(index,null,Modifier.size(140.dp),unlocked=count>0)
                Text(when(index){0->"已留下足迹的城市：";1->"已到访的咖啡馆：";else->"有照片的到访记录："}+count,fontSize=13.sp,modifier=Modifier.padding(top=18.dp))
                Text("每一次出发，都在慢慢丰富这本图鉴。",fontSize=12.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=9.dp))
            }},confirmButton={TextButton(onClick={badgeIndex=null}){Text("收好了")}},containerColor=AtlasColors.Card)
    }
    if(rename) TextEditDialog("我的昵称",data.nickname,{controller.rename(it);rename=false},{rename=false})
    if(restore) ConfirmDialog("恢复备份","恢复将替换当前地点和记录。建议先导出一份当前备份。照片路径仅在原设备文件仍存在时可用。","选择备份",{restore=false;controller.restoreBackup()},{restore=false})
    if(clear) ConfirmDialog("翻开属于你的第一页","将移除当前图鉴中的所有地点和记录，包括你在示例中新增的内容。请先导出需要保留的数据。","清空并开始",{if(controller.clearDemo())clear=false},{clear=false})
    if(about) AlertDialog(onDismissRequest={about=false},title={Text("关于这本小图鉴",style=MaterialTheme.typography.titleLarge)},
        text={Text("Kotlin Compose Multiplatform 原生渲染初稿。\n\n示例地点为虚构，照片为 Unsplash 氛围素材，非店铺实拍。\n\n地图为离线布局示意，未接入真实底图与 POI 服务。文本导入支持“店名｜地址｜城市”；小红书链接先保存为来源，未接入自动解析。\n\n使用本机系统照片选择器。导入照片独立保存于应用目录。JSON 备份暂不包含照片二进制内容。",fontSize=13.sp)},
        confirmButton={TextButton(onClick={about=false}){Text("知道了")}},containerColor=AtlasColors.Card)
}

@Composable
fun SettingRow(glyph:Glyph,title:String,subtitle:String,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=18.dp,vertical=17.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        AtlasIcon(glyph,color=AtlasColors.Green,size=20.dp)
        Column(Modifier.weight(1f)){Text(title,fontSize=13.sp);Text(subtitle,fontSize=10.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=4.dp))}
        AtlasIcon(Glyph.ARROW,size=16.dp,color=AtlasColors.Muted)
    }
}
