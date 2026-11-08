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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mylittleatlas.app.domain.*
import com.mylittleatlas.app.platform.LocalPhoto
import com.mylittleatlas.app.presentation.AtlasController

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaceDetail(controller:AtlasController,place:Place,onClose:()->Unit,onShare:()->Unit,onShareVisit:(Visit)->Unit={onShare()},onJournalSaved:(Visit)->Unit={}) {
    var checkIn by remember{mutableStateOf(false)}
    var edit by remember{mutableStateOf(false)}
    var delete by remember{mutableStateOf(false)}
    var savedVisit by remember{mutableStateOf<Visit?>(null)}
    val data=controller.data
    val visits=data.visits.filter{it.placeId==place.id}.sortedByDescending{it.date}
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Paper(Modifier.widthIn(max=620.dp).fillMaxWidth().fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal=13.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                    IconButton(Glyph.BACK,"返回",onClose);Spacer(Modifier.weight(1f))
                    Text(if(visits.isEmpty())"一张待探索卡" else "一张小小纪念卡",fontSize=11.sp,color=AtlasColors.Muted)
                    Spacer(Modifier.weight(1f));IconButton(Glyph.SHARE,"生成分享卡",onShare)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp)) {
                    Box(Modifier.fillMaxWidth().height(233.dp).padding(top=10.dp)) {
                        PolaroidPhoto(place.cover,Modifier.fillMaxSize(),caption=if(visits.isEmpty())"a quiet corner in the city" else "a moment to remember",rotation=-1.5f)
                        if(visits.isNotEmpty()) Column(Modifier.align(Alignment.BottomEnd).padding(end=9.dp,bottom=21.dp).rotate(4f)
                            .background(AtlasColors.Paper).padding(horizontal=15.dp,vertical=9.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(visits.first().date.takeLast(2),fontSize=31.sp,fontFamily=FontFamily.Serif)
                            Text(visits.first().date.take(7).replace('-','.'),fontSize=9.sp,color=AtlasColors.Muted)
                        }
                    }
                    Text(place.name,style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(top=22.dp))
                    Row(Modifier.padding(top=9.dp),verticalAlignment=Alignment.CenterVertically) {
                        AtlasIcon(Glyph.PIN,size=14.dp,color=AtlasColors.Green);Spacer(Modifier.width(6.dp))
                        Text("${place.city} · ${place.district.ifBlank{place.kind.title}}",fontSize=12.sp,color=AtlasColors.Muted)
                        Spacer(Modifier.weight(1f));Tag(if(visits.isNotEmpty())"去过 ${visits.size} 次" else "想去")
                    }
                    Text(place.address,fontSize=12.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=9.dp))
                    if(data.isDemoPlace(place)) Text("示例地点 · 配图为氛围素材",fontSize=9.sp,color=AtlasColors.Muted,modifier=Modifier.padding(top=6.dp))
                    Spacer(Modifier.height(23.dp));ThinDivider();Spacer(Modifier.height(19.dp))
                    Row(verticalAlignment=Alignment.CenterVertically){AtlasIcon(Glyph.BOOKMARK,size=16.dp,color=AtlasColors.Sage);Spacer(Modifier.width(8.dp));Text("为什么想来",fontSize=11.sp,color=AtlasColors.Muted)}
                    PaperNote(Modifier.fillMaxWidth().padding(top=10.dp,bottom=14.dp)) {
                        Handwritten(place.reason.ifBlank{"把第一次心动的理由，留在这里。"},size=16)
                    }
                    if(place.collectionIds.isNotEmpty()) FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        data.collections.filter{it.id in place.collectionIds}.forEach{Tag(it.name)}
                    }
                    if(visits.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp));SectionTitle("在这里的小记")
                        visits.forEach{visit->
                            Column(Modifier.padding(top=12.dp).fillMaxWidth().background(AtlasColors.Card,RoundedCornerShape(16.dp)).clickable{savedVisit=visit}.padding(16.dp)) {
                                Row(verticalAlignment=Alignment.CenterVertically){Text(visit.date.replace('-','.'),fontFamily=FontFamily.Serif,fontSize=13.sp,color=AtlasColors.Green);Spacer(Modifier.weight(1f));AtlasIcon(Glyph.COFFEE,size=17.dp,color=AtlasColors.Peach)}
                                Text(visit.displayTitle(place.name),fontSize=14.sp,lineHeight=25.sp,modifier=Modifier.padding(top=10.dp))
                                Text(visit.displayBody().ifBlank{"在这里，留下一段属于自己的时光。"},fontSize=12.sp,lineHeight=21.sp,modifier=Modifier.padding(top=6.dp))
                                if(visit.photoPaths.isNotEmpty()) FlowRow(Modifier.padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    visit.photoPaths.forEach{LocalPhoto(it,Modifier.size(100.dp).clip(RoundedCornerShape(9.dp)))}
                                }
                            }
                        }
                        Row(Modifier.padding(top=14.dp).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Text("还想再来一次",fontSize=13.sp,modifier=Modifier.weight(1f))
                            Switch(place.wantRevisit,{controller.updatePlace(place.copy(wantRevisit=it))})
                        }
                    }
                    if(place.source.isNotBlank()) {
                        Spacer(Modifier.height(20.dp));Text("最初的灵感",fontSize=11.sp,color=AtlasColors.Muted)
                        Text(place.source,fontSize=11.sp,color=AtlasColors.Green,modifier=Modifier.padding(top=6.dp).clickable {
                            if(place.source.startsWith("https://")) controller.platform.openSource(place.source)
                        })
                    }
                    Spacer(Modifier.height(16.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        TextButton(onClick={edit=true}){Text("编辑地点",fontSize=12.sp)}
                        TextButton(onClick={delete=true}){Text("移出收藏",fontSize=12.sp,color=AtlasColors.Muted)}
                    }
                    Spacer(Modifier.height(15.dp))
                }
                Row(Modifier.fillMaxWidth().background(AtlasColors.Card).padding(horizontal=20.dp,vertical=14.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick={controller.navigate(place)},Modifier.height(50.dp),shape=RoundedCornerShape(15.dp),contentPadding=PaddingValues(horizontal=17.dp)) {
                        AtlasIcon(Glyph.NAVIGATE,size=18.dp,color=AtlasColors.Green);Spacer(Modifier.width(7.dp));Text("导航",fontSize=13.sp)
                    }
                    PrimaryButton("在这里写小记",{checkIn=true},Modifier.weight(1f),Glyph.BOOK)
                }
            }
        }
    }
    if(checkIn) JournalEntryEditor(controller,place,{checkIn=false},onSaved={checkIn=false;onJournalSaved(it)})
    savedVisit?.let{old->controller.data.visits.find{it.id==old.id}?.let{visit->
        JournalDetailV2(controller,place,visit,{savedVisit=null},{savedVisit=null},{onShareVisit(visit)})
    }}
    if(edit) EditPlaceDialog(controller,place,{edit=false})
    if(delete) ConfirmDialog("移出这个去处？","将同时删除这个地点的到访文字记录。导入的照片文件仍保留在应用目录；需要保留记录时请先导出备份。","移出收藏",{if(controller.deletePlace(place)){delete=false;onClose()}},{delete=false})
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInDialog(controller:AtlasController,place:Place,onClose:()->Unit) {
    var date by remember{mutableStateOf(controller.platform.today())}
    var note by remember{mutableStateOf("")}
    var photos by remember{mutableStateOf<List<String>>(emptyList())}
    var revisit by remember{mutableStateOf(true)}
    val validDate=isValidAtlasDate(date)
    AlertDialog(onDismissRequest=onClose,title={Column{Text("收下一段小小回忆",style=MaterialTheme.typography.titleLarge);Text(place.name,fontSize=12.sp,color=AtlasColors.Muted)}},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(date,{date=it},label={Text("到访日期 · YYYY-MM-DD")},singleLine=true,isError=!validDate,shape=RoundedCornerShape(12.dp))
            OutlinedTextField(note,{note=it},label={Text("那天的心情、喜欢的味道…")},minLines=4,shape=RoundedCornerShape(12.dp))
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                photos.forEach{path->Box{LocalPhoto(path,Modifier.size(76.dp).clip(RoundedCornerShape(10.dp)));IconButton(Glyph.CLOSE,"移除此照片",{photos=photos-path},Modifier.align(Alignment.TopEnd).size(30.dp),filled=true)}}
                if(photos.size<6) Box(Modifier.size(76.dp).clip(RoundedCornerShape(10.dp)).background(AtlasColors.SagePale).clickable {
                    controller.platform.pickPhoto{if(it!=null)photos=photos+it}
                },contentAlignment=Alignment.Center){AtlasIcon(Glyph.CAMERA,color=AtlasColors.Green)}
            }
            Text("照片通过系统选择，单独保存到应用目录。",fontSize=10.sp,color=AtlasColors.Muted)
            Row(verticalAlignment=Alignment.CenterVertically){Text("还想再来",Modifier.weight(1f),fontSize=13.sp);Switch(revisit,{revisit=it})}
        }},confirmButton={TextButton(onClick={if(controller.addVisit(place,date,note,photos,revisit))onClose()},enabled=validDate){Text("保存这次到访")}},
        dismissButton={TextButton(onClick=onClose){Text("以后再记")}},containerColor=AtlasColors.Card)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditPlaceDialog(controller:AtlasController,place:Place,onClose:()->Unit) {
    var name by remember{mutableStateOf(place.name)}
    var city by remember{mutableStateOf(place.city)}
    var address by remember{mutableStateOf(place.address)}
    var reason by remember{mutableStateOf(place.reason)}
    var folders by remember{mutableStateOf(place.collectionIds)}
    AlertDialog(onDismissRequest=onClose,title={Text("好好整理这处心动",style=MaterialTheme.typography.titleLarge)},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            AtlasField(name,{name=it},"地点名称");AtlasField(city,{city=it},"城市");AtlasField(address,{address=it},"地址／分店")
            AtlasField(reason,{reason=it},"为什么想去",singleLine=false)
            Text("收进收藏夹",fontSize=12.sp,color=AtlasColors.Muted)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                controller.data.collections.forEach{c->Tag(c.name,c.id in folders,{folders=if(c.id in folders)folders-c.id else folders+c.id})}
            }
        }
    },confirmButton={TextButton(onClick={
        val changedAddress=address.trim()!=place.address||city.trim()!=place.city
        if(controller.updatePlace(place.copy(name=name.trim(),city=city.trim(),address=address.trim(),reason=reason.trim(),collectionIds=folders,
            latitude=if(changedAddress)null else place.latitude,longitude=if(changedAddress)null else place.longitude,
            gcjLatitude=if(changedAddress)null else place.gcjLatitude,gcjLongitude=if(changedAddress)null else place.gcjLongitude,
            poiId=if(changedAddress)"" else place.poiId)))onClose()
    },enabled=name.isNotBlank()&&city.isNotBlank()){Text("保存")}},dismissButton={TextButton(onClick=onClose){Text("取消")}},containerColor=AtlasColors.Card)
}

@Composable
fun AtlasField(value:String,onValue:(String)->Unit,label:String,singleLine:Boolean=true,modifier:Modifier=Modifier) {
    OutlinedTextField(value,onValue,modifier.fillMaxWidth(),label={Text(label,fontSize=12.sp)},singleLine=singleLine,minLines=if(singleLine)1 else 3,
        shape=RoundedCornerShape(12.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=AtlasColors.Line))
}
