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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mylittleatlas.app.domain.*
import com.mylittleatlas.app.platform.PlatformServices
import com.mylittleatlas.app.platform.AtlasBackHandler
import com.mylittleatlas.app.presentation.AtlasController
import kotlinx.coroutines.delay

enum class AtlasTab(val title: String, val glyph: Glyph) {
    HOME("手账",Glyph.BOOK), MAP("地图",Glyph.MAP), COLLECTIONS("收藏",Glyph.BOOKMARK), PROFILE("我的",Glyph.PERSON)
}

private val primaryTabs = listOf(AtlasTab.HOME, AtlasTab.MAP, AtlasTab.COLLECTIONS, AtlasTab.PROFILE)

@Composable
fun AtlasApp(platform: PlatformServices, incomingText: String? = null, onIncomingConsumed: () -> Unit = {}) {
    val controller = remember(platform) { AtlasController(platform) }
    var tab by remember { mutableStateOf(AtlasTab.HOME) }
    var detailId by remember { mutableStateOf<String?>(null) }
    var showImport by remember { mutableStateOf(false) }
    var importTab by remember { mutableStateOf(0) }
    var showAddMenu by remember { mutableStateOf(false) }
    var writing by remember { mutableStateOf(false) }
    var recordingId by remember { mutableStateOf<String?>(null) }
    var recordingDraft by remember { mutableStateOf<VisitDraft?>(null) }
    var editingVisit by remember { mutableStateOf<Visit?>(null) }
    var importedText by remember { mutableStateOf("") }
    var collectionId by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var sharePlace by remember { mutableStateOf<Place?>(null) }
    var shareAtlas by remember { mutableStateOf(false) }
    var shareVisit by remember { mutableStateOf<Visit?>(null) }
    var shareSubset by remember { mutableStateOf<AtlasData?>(null) }
    var shareTitle by remember { mutableStateOf<String?>(null) }
    var mapFolder by remember { mutableStateOf<String?>(null) }
    var mapCity by remember { mutableStateOf<String?>(null) }
    var journalVisitId by remember { mutableStateOf<String?>(null) }
    var bookPage by remember { mutableStateOf(0) }
    var bookTarget by remember { mutableStateOf<String?>(null) }
    var directoryCity by remember { mutableStateOf<String?>(null) }
    var collectionCity by remember { mutableStateOf<String?>(null) }
    var collectionFilter by remember { mutableStateOf(PlaceFilter.ALL) }
    var mapFilter by remember { mutableStateOf(PlaceFilter.ALL) }
    LaunchedEffect(incomingText) {
        if(!incomingText.isNullOrBlank()) { importedText=incomingText;showImport=true }
    }
    LaunchedEffect(controller.message) {
        if(controller.message != null) {delay(4200);controller.message=null}
    }
    AtlasTheme {
        AtlasBackHandler(tab!=AtlasTab.HOME){if(tab==AtlasTab.COLLECTIONS&&collectionId!=null){collectionId=null;search=""}else{tab=AtlasTab.HOME;search="";searching=false}}
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 900.dp
            Paper(Modifier.fillMaxSize(), textured = tab == AtlasTab.HOME || tab == AtlasTab.PROFILE) {
                Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    if(wide) DesktopRail(tab,{tab=it;collectionId=null}, {showAddMenu=true})
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            Box(Modifier.fillMaxSize().widthIn(max=1200.dp).align(Alignment.TopCenter)) {
                                when(tab) {
                                    AtlasTab.HOME -> JournalHomeSimple(controller.data,{journalVisitId=it.id},
                                        {editingVisit=it;recordingId=it.placeId;writing=true},
                                        {recordingId=null;recordingDraft=null;editingVisit=null;writing=true},
                                        {importedText="";importTab=1;showImport=true},wide,bookPage,{bookPage=it},
                                        bookTarget,{bookTarget=null},directoryCity,{directoryCity=it})
                                    AtlasTab.MAP -> key(mapFolder,mapCity,mapFilter){MapScreen(controller,{detailId=it.id}, {tab=AtlasTab.COLLECTIONS},wide,mapFolder,mapCity,mapFilter)}
                                    AtlasTab.COLLECTIONS -> CollectionsV2(controller, collectionId, {collectionId=it;search="";searching=false}, {detailId=it.id},
                                        search,{search=it},searching,{searching=it}, {importedText="";importTab=1;showImport=true},wide,{tab=AtlasTab.HOME},
                                        {mapFolder=it;mapCity=null;mapFilter=PlaceFilter.ALL;tab=AtlasTab.MAP},{id->val folder=controller.data.collections.find{it.id==id};shareSubset=controller.data.copy(places=controller.data.places.filter{id in it.collectionIds});shareTitle=folder?.name;shareAtlas=true},
                                        collectionCity,{collectionCity=it},collectionFilter,{collectionFilter=it})
                                    AtlasTab.PROFILE -> ProfileV2(controller,{shareAtlas=true},{tab=AtlasTab.HOME},
                                        {mapFolder=null;mapCity=null;mapFilter=PlaceFilter.VISITED;tab=AtlasTab.MAP},
                                        {recordingDraft=it;recordingId=it.placeId.takeIf { id->id.isNotBlank() };editingVisit=null;writing=true})
                                }
                            }
                            if(controller.message != null) {
                                Surface(Modifier.align(Alignment.BottomCenter).padding(16.dp).widthIn(max=500.dp),
                                    shape=RoundedCornerShape(16.dp),color=AtlasColors.Ink,shadowElevation=8.dp) {
                                    Text(controller.message!!,Modifier.padding(16.dp),color=AtlasColors.Card,style=MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        if(!wide) BottomBar(tab,{tab=it;collectionId=null;search="";searching=false},{showAddMenu=true})
                    }
                }
            }
        }
        if(showAddMenu) SimpleAddSheet({showAddMenu=false},
            {showAddMenu=false;recordingId=null;recordingDraft=null;editingVisit=null;writing=true},
            {showAddMenu=false;importedText="";importTab=1;showImport=true})
        if(writing)JournalEntryEditor(controller,controller.data.places.find{it.id==recordingId},
            {writing=false;recordingId=null;recordingDraft=null;editingVisit=null},recordingDraft,
            {bookTarget=it.id;tab=AtlasTab.HOME;detailId=null;journalVisitId=null},editingVisit)
        detailId?.let { id ->
            controller.data.places.find { it.id == id }?.let { place ->
                PlaceDetail(controller,place,{detailId=null},{sharePlace=place},onShareVisit={shareVisit=it;sharePlace=place},
                    onJournalSaved={detailId=null;bookTarget=it.id;tab=AtlasTab.HOME})
            }
        }
        if(showImport) key(importedText) {
            ImportSheet(controller,importedText,{showImport=false;importedText="";importTab=0;onIncomingConsumed()},importTab)
        }
        journalVisitId?.let { id -> controller.data.visits.find{it.id==id}?.let{visit->
            controller.data.places.find{it.id==visit.placeId}?.let{place->
                JournalDetailV2(controller,place,visit,{journalVisitId=null},{journalVisitId=null;detailId=place.id},{shareVisit=visit;sharePlace=place})
            }
        } }
        if(shareAtlas || sharePlace != null) ShareSheet(shareSubset ?: controller.data,sharePlace,platform,{shareAtlas=false;sharePlace=null;shareVisit=null;shareSubset=null;shareTitle=null},shareVisit,shareTitle)
        if(!controller.data.onboarded)WelcomeV2(controller)
    }
}

@Composable
private fun BottomBar(tab: AtlasTab,onTab:(AtlasTab)->Unit,onAdd:()->Unit) {
    Surface(color=AtlasColors.Card,shadowElevation=0.dp) {
        val selectedTab = tab
        Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            BottomNavItem(AtlasTab.HOME,selectedTab==AtlasTab.HOME,onTab,Modifier.weight(1f))
            BottomNavItem(AtlasTab.MAP,selectedTab==AtlasTab.MAP,onTab,Modifier.weight(1f))
            Box(Modifier.weight(1f).fillMaxHeight().clickable(role=Role.Button,onClick=onAdd).semantics {
                contentDescription = "添加"
            }, contentAlignment=Alignment.Center) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(AtlasColors.Green),contentAlignment=Alignment.Center) {
                    AtlasIcon(Glyph.PLUS,color=AtlasColors.Card,size=24.dp)
                }
            }
            BottomNavItem(AtlasTab.COLLECTIONS,selectedTab==AtlasTab.COLLECTIONS,onTab,Modifier.weight(1f))
            BottomNavItem(AtlasTab.PROFILE,selectedTab==AtlasTab.PROFILE,onTab,Modifier.weight(1f))
        }
    }
}

@Composable
private fun BottomNavItem(tab: AtlasTab,selected: Boolean,onTab:(AtlasTab)->Unit,modifier:Modifier=Modifier) {
    Column(modifier.fillMaxHeight().clip(RoundedCornerShape(12.dp)).clickable(role=Role.Tab,onClick={onTab(tab)})
        .semantics { contentDescription=tab.title; role=Role.Tab }.padding(top=6.dp,bottom=3.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)) {
        Box(Modifier.size(42.dp,30.dp).clip(RoundedCornerShape(11.dp))
            .background(if(selected) AtlasColors.SagePale else Color.Transparent),contentAlignment=Alignment.Center) {
            AtlasIcon(tab.glyph,color=if(selected) AtlasColors.Green else AtlasColors.Muted,size=20.dp)
        }
        Text(tab.title,fontSize=10.sp,color=if(selected) AtlasColors.Green else AtlasColors.Muted,
            fontWeight=if(selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun DesktopRail(tab: AtlasTab,onTab:(AtlasTab)->Unit,onAdd:()->Unit) {
    Column(Modifier.width(224.dp).fillMaxHeight().background(AtlasColors.Card.copy(alpha=.65f)).padding(26.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("My Little\nAtlas",style=MaterialTheme.typography.headlineLarge,lineHeight=35.sp)
        Text("我的城市收藏图鉴",fontSize=11.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=5.dp,bottom=38.dp))
        primaryTabs.forEach { item ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if(tab==item) AtlasColors.SagePale else Color.Transparent)
                .clickable{onTab(item)}.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(15.dp)) {
                AtlasIcon(item.glyph,color=if(tab==item) AtlasColors.Green else AtlasColors.Muted)
                Text(item.title,fontSize=14.sp,color=if(tab==item) AtlasColors.Green else AtlasColors.Muted)
            }
        }
        Spacer(Modifier.height(24.dp));PrimaryButton("添加",onAdd,Modifier.fillMaxWidth(),Glyph.PLUS)
        Spacer(Modifier.weight(1f));AtlasIcon(Glyph.LOCK,color=AtlasColors.Sage,size=18.dp)
        Text("一份只属于你的城市收藏。",fontSize=11.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=10.dp))
        Handwritten("Collect a better life ♡",Modifier.padding(top=18.dp),size=16)
    }
}

@Composable
fun PageHeader(eyebrow:String,title:String,subtitle:String?=null,trailing:@Composable RowScope.()->Unit={}) {
    Column(Modifier.fillMaxWidth().padding(horizontal=24.dp).padding(top=26.dp,bottom=18.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(eyebrow,letterSpacing=2.sp,fontSize=10.sp,color=AtlasColors.Muted,modifier=Modifier.weight(1f))
            trailing()
        }
        Text(title,style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(top=9.dp))
        if(subtitle != null) Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=AtlasColors.Muted,modifier=Modifier.padding(top=7.dp))
    }
}

@Composable
fun PlaceRow(place:Place,data:AtlasData,onClick:()->Unit,modifier:Modifier=Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(AtlasColors.Card).clickable(onClick=onClick).padding(10.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)) {
        Photo(place.cover,Modifier.size(78.dp).clip(RoundedCornerShape(12.dp)),place.name)
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text(place.name,style=MaterialTheme.typography.titleMedium,maxLines=1)
            Text("${place.city} · ${place.district.ifBlank { place.kind.title }}",fontSize=11.sp,color=AtlasColors.Muted)
            Text(place.reason.ifBlank {place.address},fontSize=11.sp,color=AtlasColors.Muted,maxLines=1)
        }
        AtlasIcon(if(data.isVisited(place.id)) Glyph.CHECK else Glyph.BOOKMARK,color=if(data.isVisited(place.id)) AtlasColors.Green else AtlasColors.Peach,size=18.dp)
    }
}

@Composable
fun FilterRow(filter:PlaceFilter,onFilter:(PlaceFilter)->Unit,modifier:Modifier=Modifier) {
    Row(modifier,horizontalArrangement=Arrangement.spacedBy(7.dp)) {PlaceFilter.entries.forEach {Tag(it.title,it==filter,{onFilter(it)},AtlasColors.Card)}}
}

@Composable
fun ConfirmDialog(title:String,text:String,confirm:String="确定",onConfirm:()->Unit,onDismiss:()->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={Text(title,style=MaterialTheme.typography.titleLarge)},text={Text(text)},
        confirmButton={TextButton(onClick=onConfirm){Text(confirm)}},dismissButton={TextButton(onClick=onDismiss){Text("取消")}},containerColor=AtlasColors.Card)
}

@Composable
fun TextEditDialog(title:String,initial:String="",onConfirm:(String)->Unit,onDismiss:()->Unit) {
    var text by remember{mutableStateOf(initial)}
    AlertDialog(onDismissRequest=onDismiss,title={Text(title,style=MaterialTheme.typography.titleLarge)},
        text={OutlinedTextField(text,{text=it},singleLine=true,label={Text(title)},shape=RoundedCornerShape(12.dp))},
        confirmButton={TextButton(onClick={onConfirm(text)},enabled=text.isNotBlank()){Text("保存")}},
        dismissButton={TextButton(onClick=onDismiss){Text("取消")}},containerColor=AtlasColors.Card)
}
