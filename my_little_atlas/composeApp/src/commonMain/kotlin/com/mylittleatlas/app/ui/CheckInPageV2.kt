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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mylittleatlas.app.domain.*
import com.mylittleatlas.app.platform.LocalPhoto
import com.mylittleatlas.app.presentation.AtlasController

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInPageV2(controller:AtlasController,place:Place,onClose:()->Unit,draft:VisitDraft?=null,onSaved:(Visit)->Unit = {}) {
    val draftId=remember{draft?.id ?: controller.platform.uniqueId()}
    var date by remember{mutableStateOf(draft?.date ?: controller.platform.today())}
    var note by remember{mutableStateOf(draft?.note ?: "")}
    var photos by remember{mutableStateOf(draft?.photoPaths ?: emptyList())}
    var revisit by remember{mutableStateOf(draft?.revisit ?: place.wantRevisit)}
    var calendar by remember{mutableStateOf(false)}
    var discard by remember{mutableStateOf(false)}
    val changed=note.isNotBlank()||photos.isNotEmpty()||date!=controller.platform.today()||revisit!=place.wantRevisit
    val valid=isValidAtlasDate(date)&&date<=controller.platform.today()
    fun close(){if(changed)discard=true else onClose()}
    Dialog(onDismissRequest={close()},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Paper(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){
                    IconButton(Glyph.BACK,"返回地点",{close()})
                    Text("写下今天的小美好",fontSize=16.sp,modifier=Modifier.weight(1f))
                    TextButton(onClick={if(controller.saveDraft(VisitDraft(draftId,place.id,date,note,photos,revisit)))onClose()}){Text("存草稿",fontSize=11.sp)}
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(17.dp)) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AtlasColors.Card).padding(13.dp),verticalAlignment=Alignment.CenterVertically) {
                        Photo(place.cover,Modifier.size(58.dp).clip(RoundedCornerShape(7.dp)))
                        Column(Modifier.padding(start=12.dp)){Text(place.name,style=MaterialTheme.typography.titleMedium);Text(place.city+" · "+place.kind.title,fontSize=11.sp,color=AtlasColors.Muted)}
                    }
                    Text("到访日期",fontSize=13.sp)
                    OutlinedTextField(date,{date=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("YYYY-MM-DD")},isError=!valid,
                        supportingText={Text(if(valid)"可以补记过去的到访" else "请输入有效日期，不能选择未来日期")},shape=RoundedCornerShape(10.dp))
                    TextButton(onClick={calendar=true}){Text("打开日历选择",fontSize=11.sp)}
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("今天拍到的",fontSize=13.sp,modifier=Modifier.weight(1f));Text(photos.size.toString()+" / 9 · 选填",fontSize=10.sp,color=AtlasColors.Muted)}
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(9.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                        photos.forEach{p->Box{LocalPhoto(p,Modifier.size(96.dp).clip(RoundedCornerShape(9.dp)));IconButton(Glyph.CLOSE,"移除此照片",{photos=photos-p},Modifier.align(Alignment.TopEnd).size(32.dp),filled=true)}}
                        if(photos.size<9) Column(Modifier.size(96.dp).clip(RoundedCornerShape(9.dp)).background(AtlasColors.SagePale).clickable{controller.platform.pickPhoto{if(it!=null)photos=photos+it}},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){AtlasIcon(Glyph.PLUS,size=24.dp);Text("添加照片",fontSize=10.sp,modifier=Modifier.padding(top=8.dp))}
                    }
                    Text("一句话，记住今天",fontSize=13.sp)
                    OutlinedTextField(note,{note=it},Modifier.fillMaxWidth(),placeholder={Text("阳光、味道，或今天的小小心情…",fontSize=13.sp)},minLines=4,shape=RoundedCornerShape(10.dp))
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("还想再来",fontSize=13.sp,modifier=Modifier.weight(1f));Switch(revisit,{revisit=it})}
                    Text("照片与文字都可跳过，保存后会出现在你的足迹中。",fontSize=11.sp,color=AtlasColors.Muted)
                    Spacer(Modifier.height(16.dp))
                }
                PrimaryButton("留住这次到访",{if(controller.addVisit(place,date,note,photos,revisit)){controller.deleteDraft(draftId);onSaved(controller.data.visits.first());onClose()}},Modifier.fillMaxWidth().padding(16.dp),Glyph.CHECK,enabled=valid)
            }
        }
    }
    if(discard) AlertDialog(onDismissRequest={discard=false},title={Text("这一页，还要留着吗？")},text={Text("存为草稿可以下次继续，草稿不会计入到访。")},
        confirmButton={TextButton(onClick={if(controller.saveDraft(VisitDraft(draftId,place.id,date,note,photos,revisit)))onClose()}){Text("保存草稿并离开")}},
        dismissButton={Row{TextButton(onClick={discard=false}){Text("继续写")};TextButton(onClick=onClose){Text("放弃")}}},containerColor=AtlasColors.Card)
    if(calendar) AtlasCalendar(date,controller.platform.today(),{date=it;calendar=false},{calendar=false})
}
