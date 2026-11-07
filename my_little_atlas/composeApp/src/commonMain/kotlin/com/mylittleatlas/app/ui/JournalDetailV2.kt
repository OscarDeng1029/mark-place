package com.mylittleatlas.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mylittleatlas.app.domain.*
import com.mylittleatlas.app.presentation.AtlasController

@Composable
fun JournalDetailV2(controller:AtlasController,place:Place,visit:Visit,onClose:()->Unit,onPlace:()->Unit,onShare:()->Unit) {
    var edit by remember{mutableStateOf(false)}
    var options by remember{mutableStateOf(false)}
    var deleting by remember{mutableStateOf(false)}
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Paper(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), textured = true) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().height(54.dp).padding(horizontal=9.dp),verticalAlignment=Alignment.CenterVertically) {
                    IconButton(Glyph.BACK,"返回足迹",onClose)
                    Text("我的手账",fontSize=15.sp,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(Glyph.MORE,"到访记录更多操作",{options=true})
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=22.dp).padding(top=16.dp,bottom=25.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(visit.date.takeLast(2),fontFamily=FontFamily.Serif,fontSize=29.sp,color=AtlasColors.Sage)
                        Column(Modifier.padding(start=12.dp)) {
                            Text(visit.date.take(7).replace('-','.'),fontSize=10.sp,color=AtlasColors.Muted)
                            Text(place.city,fontSize=10.sp,color=AtlasColors.Muted)
                        }
                        Spacer(Modifier.weight(1f))
                        Tag("第 "+controller.data.visits.filter{it.placeId==place.id}.sortedWith(compareBy<Visit>{it.date}.thenBy{it.id}).indexOfFirst{it.id==visit.id}.let{it+1}+" 次到访")
                    }
                    Spacer(Modifier.height(20.dp))
                    JournalCoverV2(place,visit,Modifier.fillMaxWidth())
                    Spacer(Modifier.height(24.dp))
                    Text(place.name,style=MaterialTheme.typography.headlineMedium)
                    Row(Modifier.padding(top=8.dp).clickable(onClick=onPlace),verticalAlignment=Alignment.CenterVertically) {
                        AtlasIcon(Glyph.PIN,size=13.dp,color=AtlasColors.Sage)
                        Text(place.city+" · "+place.district+"    查看地点 →",fontSize=11.sp,color=AtlasColors.Muted,modifier=Modifier.padding(start=5.dp))
                    }
                    Spacer(Modifier.height(22.dp))
                    Handwritten(visit.note.ifBlank{"把这一天，留在这一页。"},size=17)
                    ThinDivider(Modifier.padding(top=17.dp))
                    Spacer(Modifier.height(25.dp))
                    Box(Modifier.fillMaxWidth().height(if(visit.photoPaths.size>1)156.dp else 86.dp)) {
                        if(visit.photoPaths.size>1)PolaroidPhoto(place.cover,Modifier.offset(x=7.dp).size(135.dp,128.dp),rotation=-7f,showTape=false,localPath=visit.photoPaths.getOrNull(1))
                        if(visit.photoPaths.size>2)PolaroidPhoto(place.cover,Modifier.offset(x=129.dp,y=8.dp).size(135.dp,128.dp),rotation=6f,showTape=false,localPath=visit.photoPaths.getOrNull(2))
                        if(visit.photoPaths.size<=1)Handwritten("good things take time ♡",Modifier.padding(top=24.dp),size=14)
                        CoffeePostmark(Modifier.align(Alignment.BottomEnd).size(76.dp))
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Tag("这一天的记忆");if(place.wantRevisit)Tag("还想再来",tint=AtlasColors.PeachPale)}
                }
                Row(Modifier.fillMaxWidth().background(AtlasColors.Card).padding(16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick={edit=true},Modifier.weight(1f).height(48.dp),shape=RoundedCornerShape(10.dp)){Text("调整这一页",fontSize=12.sp)}
                    PrimaryButton("分享这份美好",onShare,Modifier.weight(1.4f),Glyph.SHARE)
                }
            }
        }
    }
    if(edit) JournalEditorV2(controller,place,visit,{edit=false})
    if(options)AlertDialog(onDismissRequest={options=false},title={Text("这一页的小记")},text={Column{
        SettingRow(Glyph.EDIT,"编辑记录与排版","调整照片顺序、日期和文字"){options=false;edit=true}
        SettingRow(Glyph.SHARE,"分享这一页","只带入当前这次到访"){options=false;onShare()}
        SettingRow(Glyph.CLOSE,"删除本次到访","保留地点与其他到访记录"){options=false;deleting=true}
    }},confirmButton={TextButton(onClick={options=false}){Text("收好")}},containerColor=AtlasColors.Card)
    if(deleting)ConfirmDialog("删除这次到访？","只删除 "+visit.date+" 的这次记录；地点、其他到访与原照片文件继续保留。","删除记录",{if(controller.deleteVisit(visit.id)){deleting=false;onClose()}},{deleting=false})
}

@Composable
fun JournalCoverV2(place:Place,visit:Visit,modifier:Modifier=Modifier) {
    when(visit.layout){
        "film"->Column(modifier.background(AtlasColors.Ink).padding(9.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){repeat(10){Box(Modifier.padding(bottom=8.dp).size(8.dp,5.dp).background(AtlasColors.Paper))}}
            Photo(visit.photoPaths.firstOrNull()?.let{"local:"+it} ?: place.cover,Modifier.fillMaxWidth().height(195.dp))
            Text("A MOMENT TO KEEP  /  "+visit.date,fontSize=8.sp,color=AtlasColors.Card,modifier=Modifier.padding(top=9.dp,bottom=4.dp))
        }
        "note"->PaperNote(modifier){Column{Photo(visit.photoPaths.firstOrNull()?.let{"local:"+it} ?: place.cover,Modifier.fillMaxWidth().height(180.dp));Handwritten("today is a little gift",Modifier.padding(top=12.dp),size=15)}}
        else->PolaroidPhoto(place.cover,modifier.height(220.dp),caption="a little moment to remember",localPath=visit.photoPaths.firstOrNull())
    }
}

@Composable
fun JournalEditorV2(controller:AtlasController,place:Place,visit:Visit,onClose:()->Unit) {
    var note by remember{mutableStateOf(visit.note)}
    var date by remember{mutableStateOf(visit.date)}
    var photos by remember{mutableStateOf(visit.photoPaths)}
    var layout by remember{mutableStateOf(visit.layout)}
    var calendar by remember{mutableStateOf(false)}
    var discard by remember{mutableStateOf(false)}
    val changed=note!=visit.note||date!=visit.date||photos!=visit.photoPaths||layout!=visit.layout
    val close={if(changed)discard=true else onClose()}
    AtlasFullPage("调整这一页",close,actions={TextButton(onClick={if(controller.updateVisit(visit.copy(note=note.trim(),date=date,photoPaths=photos,layout=layout)))onClose()},enabled=isValidAtlasDate(date)&&date<=controller.platform.today()){Text("保存")}}) {
        Text("给回忆，选一个喜欢的样子",style=MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("warm" to "暖色手账","film" to "胶片日记","note" to "纸上小记").forEach{(id,label)->Tag(label,layout==id,{layout=id})}}
        JournalCoverV2(place,visit.copy(layout=layout,photoPaths=photos),Modifier.fillMaxWidth())
        TextButton(onClick={calendar=true}){AtlasIcon(Glyph.BOOK,size=17.dp);Spacer(Modifier.width(8.dp));Text(date+"  ·  修改日期")}
        AtlasField(note,{note=it},"这一天的小记",singleLine=false)
        Text("照片顺序 · 第一张为封面",fontSize=12.sp,color=AtlasColors.Muted)
        photos.forEachIndexed{index,path->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Photo("local:"+path,Modifier.size(64.dp).clip(RoundedCornerShape(5.dp)))
            Text("第 "+(index+1)+" 张",fontSize=12.sp,modifier=Modifier.weight(1f).padding(start=12.dp))
            TextButton(onClick={photos=photos.toMutableList().apply{add(index-1,removeAt(index))}},enabled=index>0){Text("前移")}
            IconButton(Glyph.CLOSE,"移除第 "+(index+1)+" 张照片",{photos=photos.filterIndexed{i,_->i!=index}})
        }}
        if(photos.size<9)OutlinedButton(onClick={controller.platform.pickPhoto{if(it!=null&&photos.size<9)photos=photos+it}},modifier=Modifier.fillMaxWidth()){Text("添加照片  "+photos.size+"/9")}
        Text("调整保存在本次到访中，分享时会沿用这份排版。",fontSize=11.sp,color=AtlasColors.Muted)
    }
    if(calendar)AtlasCalendar(date,controller.platform.today(),{date=it;calendar=false},{calendar=false})
    if(discard)ConfirmDialog("放弃这次调整？","原来的到访记录继续保留。","放弃调整",onClose,{discard=false})
}
