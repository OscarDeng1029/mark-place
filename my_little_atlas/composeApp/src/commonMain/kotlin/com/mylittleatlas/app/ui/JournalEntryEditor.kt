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
fun JournalEntryEditor(controller:AtlasController,place:Place?,onClose:()->Unit,draft:VisitDraft?=null,
    onSaved:(Visit)->Unit={},visit:Visit?=null) {
    val draftId=remember { draft?.id ?: controller.platform.uniqueId() }
    val initialName=draft?.placeName?.takeIf { it.isNotBlank() } ?: place?.name.orEmpty()
    val initialCity=draft?.city?.takeIf { it.isNotBlank() } ?: place?.city ?: controller.data.preferredCity
    var name by remember { mutableStateOf(initialName) }
    var city by remember { mutableStateOf(initialCity) }
    var selectedId by remember { mutableStateOf(place?.id) }
    var date by remember { mutableStateOf(visit?.date ?: draft?.date ?: controller.platform.today()) }
    var note by remember { mutableStateOf(visit?.note ?: draft?.note.orEmpty()) }
    var photos by remember { mutableStateOf(visit?.photoPaths ?: draft?.photoPaths ?: emptyList()) }
    var revisit by remember { mutableStateOf(draft?.revisit ?: place?.wantRevisit ?: false) }
    var calendar by remember { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    val changed=name!=initialName || city!=initialCity || date!=(visit?.date ?: draft?.date ?: controller.platform.today()) ||
        note!=(visit?.note ?: draft?.note.orEmpty()) || photos!=(visit?.photoPaths ?: draft?.photoPaths ?: emptyList<String>())
    val valid=name.isNotBlank() && city.isNotBlank() && isValidAtlasDate(date) && date<=controller.platform.today()
    fun close() { if(changed)discard=true else onClose() }
    fun saveDraft() {
        val actualId=selectedId?.takeIf { id -> controller.data.places.any { it.id==id && it.name==name.trim() && it.city==city.trim() } }.orEmpty()
        if(controller.saveDraft(VisitDraft(draftId,actualId,date,note,photos,revisit,name.trim(),city.trim())))onClose()
    }
    Dialog(onDismissRequest={close()},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Paper(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),color=androidx.compose.ui.graphics.Color(0xFFFDFCF9)) {
            Column(Modifier.fillMaxSize().widthIn(max=640.dp).align(Alignment.TopCenter)) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    TextButton(onClick={close()}) { Text("取消",fontSize=12.sp) }
                    Text(if(visit==null)"写手账" else "编辑手账",fontSize=17.sp,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                    if(visit==null)TextButton(onClick={saveDraft()}) { Text("存草稿",fontSize=11.sp) }
                    else Spacer(Modifier.width(64.dp))
                }
                HorizontalDivider(color=AtlasColors.Line)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=23.dp,vertical=22.dp),verticalArrangement=Arrangement.spacedBy(15.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text("去过哪里？",fontSize=13.sp,modifier=Modifier.weight(1f))
                        if(controller.data.places.isNotEmpty())TextButton(onClick={choosing=true}) { Text("从已有地点选",fontSize=10.sp) }
                    }
                    OutlinedTextField(name,{name=it;selectedId=null},Modifier.fillMaxWidth(),label={Text("地点名称",fontSize=11.sp)},
                        placeholder={Text("填写或选择一个地点",fontSize=12.sp)},leadingIcon={AtlasIcon(Glyph.PIN,size=17.dp)},singleLine=true,shape=RoundedCornerShape(10.dp))
                    OutlinedTextField(city,{city=it;selectedId=null},Modifier.fillMaxWidth(),label={Text("所在城市",fontSize=11.sp)},
                        placeholder={Text("例如：上海",fontSize=12.sp)},singleLine=true,shape=RoundedCornerShape(10.dp))
                    Text("可以直接填写新地点，不用先收藏。",fontSize=10.sp,color=AtlasColors.Muted)
                    Row(Modifier.fillMaxWidth().clickable { calendar=true }.padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text("哪一天",fontSize=12.sp,modifier=Modifier.weight(1f))
                        Text(date,fontSize=12.sp,color=AtlasColors.Green);Spacer(Modifier.width(10.dp));AtlasIcon(Glyph.BOOK,size=16.dp)
                    }
                    HorizontalDivider(color=AtlasColors.Line)
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text("留下几张照片",fontSize=13.sp,modifier=Modifier.weight(1f));Text("${photos.size} / 9 · 选填",fontSize=10.sp,color=AtlasColors.Muted)
                    }
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(9.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                        photos.forEach { path -> Box {
                            LocalPhoto(path,Modifier.size(88.dp).clip(RoundedCornerShape(9.dp)))
                            IconButton(Glyph.CLOSE,"移除此照片",{photos=photos-path},Modifier.align(Alignment.TopEnd).size(30.dp),filled=true)
                        } }
                        if(photos.size<9)Column(Modifier.size(88.dp).clip(RoundedCornerShape(9.dp)).background(AtlasColors.SagePale).clickable {
                            controller.platform.pickPhoto { if(it!=null && photos.size<9)photos=photos+it }
                        },horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                            AtlasIcon(Glyph.PLUS,size=23.dp);Text("添加照片",fontSize=10.sp,modifier=Modifier.padding(top=8.dp))
                        }
                    }
                    Row { Text("想记住什么？",fontSize=13.sp,modifier=Modifier.weight(1f));Text("选填",fontSize=10.sp,color=AtlasColors.Muted) }
                    OutlinedTextField(note,{note=it},Modifier.fillMaxWidth(),placeholder={Text("一杯喜欢的咖啡，或今天的小小心情……",fontSize=12.sp)},minLines=4,shape=RoundedCornerShape(10.dp))
                    Text("排版交给手账，你只管记录。",fontSize=10.sp,color=AtlasColors.Muted,modifier=Modifier.align(Alignment.CenterHorizontally))
                }
                HorizontalDivider(color=AtlasColors.Line)
                PrimaryButton(if(visit==null)"保存，生成手账" else "保存修改",{
                    val saved=controller.saveJournalEntry(selectedId,name,city,date,note,photos,revisit,draft?.id,visit?.id)
                    if(saved!=null) { onSaved(saved);onClose() }
                },Modifier.fillMaxWidth().padding(16.dp),Glyph.CHECK,enabled=valid)
            }
        }
    }
    if(choosing)AlertDialog(onDismissRequest={choosing=false},title={Text("选择地点")},text={
        Column(Modifier.heightIn(max=380.dp).verticalScroll(rememberScrollState())) {
            controller.data.places.filter { name.isBlank() || it.name.contains(name,true) || name==initialName }.forEach { p ->
                TextButton(onClick={name=p.name;city=p.city;selectedId=p.id;revisit=p.wantRevisit;choosing=false}) { Text(p.name+" · "+p.city,fontSize=12.sp) }
            }
            TextButton(onClick={choosing=false}) { Text("直接填写新地点",fontSize=12.sp) }
        }
    },confirmButton={TextButton(onClick={choosing=false}) { Text("关闭") }},containerColor=AtlasColors.Card)
    if(calendar)AtlasCalendar(date,controller.platform.today(),{date=it;calendar=false},{calendar=false})
    if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("这一页，还要留着吗？")},text={Text(if(visit==null)"存为草稿可以下次继续，草稿不会生成正式手账。" else "离开后保留原来的记录。")},
        confirmButton={if(visit==null)TextButton(onClick={saveDraft()}) { Text("保存草稿并离开") } else TextButton(onClick=onClose) { Text("放弃修改") }},
        dismissButton={Row { TextButton(onClick={discard=false}) { Text("继续写") };if(visit==null)TextButton(onClick=onClose) { Text("放弃") } }},containerColor=AtlasColors.Card)
}
