package com.mylittleatlas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import com.mylittleatlas.app.platform.AtlasService
import com.mylittleatlas.app.platform.PlatformServices
import com.mylittleatlas.app.presentation.AtlasController
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportSheet(controller: AtlasController, initialText: String, onClose: () -> Unit) {
    var text by remember { mutableStateOf(initialText) }
    var tab by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("上海") }
    var source by remember { mutableStateOf(SourceLinks.extract(initialText).orEmpty()) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var folders by remember { mutableStateOf(emptySet<String>()) }
    var note by remember { mutableStateOf<NoteImport?>(null) }
    var phase by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var serviceSettings by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf<NoteShop?>(null) }
    var manualLocator by remember { mutableStateOf(false) }
    var manualPoi by remember { mutableStateOf<ResolvedPoi?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var cityPicker by remember { mutableStateOf(false) }
    var requestVersion by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val service = remember(controller) { AtlasService(controller.platform.transport) }
    val structured = remember(text) { TextPlaceImporter.extract(text) }
    val candidates = note?.shops?.map { it.candidate(city, note!!.source) } ?: structured
    val busy = phase.isNotEmpty()
    val key: (ImportCandidate) -> String = { "${it.city}|${it.name}|${it.address}" }
    fun reset() { requestVersion++; job?.cancel(); phase = ""; note = null; error = null; selected = emptySet() }
    fun parse() {
        val version = ++requestVersion
        job?.cancel(); selected = emptySet(); note = null; error = null
        source = SourceLinks.extract(text).orEmpty()
        phase = if (source.isBlank()) "正在识别笔记中的地点…" else "正在读取小红书公开笔记…"
        job = scope.launch {
            try {
                val parsed = service.parse(text.trim(), city.trim())
                note = parsed
                if (parsed.shops.isNotEmpty()) {
                    phase = "识别到 ${parsed.shops.size} 处，正在核对高德门店…"
                    try { note = service.match(parsed, city.trim()) }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { error = "店名已保留。${e.message ?: "地址匹配失败，可手动确认门店。"}" }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "读取失败，请重试或粘贴笔记正文。" }
            finally { if(version == requestVersion) phase = "" }
        }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 620.dp).fillMaxWidth().fillMaxHeight(.94f).padding(12.dp),
            shape = RoundedCornerShape(22.dp), color = AtlasColors.Paper) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("收藏一处新去处", style = MaterialTheme.typography.titleLarge)
                        Text("慢慢收集，按自己的节奏出发", fontSize = 11.sp, color = AtlasColors.Muted)
                    }
                    IconButton(Glyph.CLOSE, "关闭", onClose)
                }
                Row(Modifier.padding(horizontal = 22.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag("粘贴笔记 / 链接", tab == 0, { tab = 0 })
                    Tag("手动添加", tab == 1, { tab = 1 })
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    if (tab == 0) {
                        Text("粘贴小红书分享链接或笔记正文，识别后由你挑选收藏。也支持每行「店名｜地址｜城市」。", fontSize = 12.sp, color = AtlasColors.Muted)
                        BasicTextField(text, { reset(); text = it }, Modifier.fillMaxWidth().heightIn(min = 110.dp, max = 180.dp)
                            .clip(RoundedCornerShape(13.dp)).background(AtlasColors.Card).padding(14.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AtlasColors.Ink),
                            decorationBox = { field -> if (text.isBlank()) Text("粘贴小红书链接、完整分享内容或笔记正文…", fontSize = 12.sp, color = AtlasColors.Muted); field() })
                        AtlasField(city, { reset(); city = it }, "店铺所在城市（用于核对分店）")
                        TextButton(onClick = { cityPicker = true }, enabled = !busy) { Text("选择常用城市", fontSize = 11.sp) }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!busy) PrimaryButton(if(note == null) "解析笔记与门店" else "重新解析", { parse() }, Modifier.weight(1f), Glyph.PIN,
                                enabled = text.trim().length >= 4 && city.isNotBlank())
                            else {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = AtlasColors.Green)
                                Text(phase, fontSize = 11.sp, color = AtlasColors.Green, modifier = Modifier.weight(1f))
                                TextButton(onClick = { job?.cancel(); phase = "" }) { Text("停止") }
                            }
                        }
                        TextButton(onClick = { serviceSettings = true }) { Text("解析服务", fontSize = 11.sp) }
                        error?.let { message ->
                            Text(message, fontSize = 12.sp, color = AtlasColors.Ink)
                            TextButton(onClick = { tab = 1; source = SourceLinks.extract(text).orEmpty() }) { Text("保留来源，手动补充地点") }
                        }
                        note?.let { result ->
                            if (result.title.isNotBlank()) Text(result.title, style = MaterialTheme.typography.titleMedium)
                            val located = result.shops.count { it.place != null }
                            Text("${result.shops.size} 处地点 · $located 处已定位 · ${if(result.method == "ai") "模型识别" else "规则识别"}", fontSize = 11.sp, color = AtlasColors.Green)
                            result.warnings.distinct().forEach { Text(it, fontSize = 11.sp, color = AtlasColors.Muted) }
                            if (result.shops.isEmpty()) Text("没有识别到地点，可补充正文或手动填写。", fontSize = 12.sp, color = AtlasColors.Muted)
                        }
                        if (candidates.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("挑选想去的地方", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                TextButton(enabled = !busy, onClick = {
                                    selected = if (selected.isNotEmpty()) emptySet() else candidates.filterNot { TextPlaceImporter.isDuplicate(it, controller.data.places) }.map(key).toSet()
                                }) { Text(if(selected.isEmpty()) "全选未收藏" else "清除选择", fontSize = 11.sp) }
                            }
                            candidates.forEachIndexed { index, candidate ->
                                val duplicate = TextPlaceImporter.isDuplicate(candidate, controller.data.places)
                                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AtlasColors.Card).padding(10.dp)) {
                                    Row(Modifier.fillMaxWidth().clickable(enabled = !duplicate && !busy) {
                                        selected = if(key(candidate) in selected) selected - key(candidate) else selected + key(candidate)
                                    }, verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(key(candidate) in selected, { checked -> selected = if(checked) selected + key(candidate) else selected - key(candidate) }, enabled = !duplicate && !busy)
                                        Column(Modifier.weight(1f)) {
                                            Text(candidate.name, style = MaterialTheme.typography.titleMedium)
                                            Text(candidate.address.ifBlank { "地址待确认 · ${candidate.city}" }, fontSize = 11.sp, color = AtlasColors.Muted)
                                            Text(if(duplicate) "已收藏" else if(candidate.latitude != null) "已定位 · 收藏后显示在地图" else "待定位 · 可先收藏", fontSize = 10.sp, color = AtlasColors.Green)
                                        }
                                    }
                                    note?.shops?.getOrNull(index)?.let { shop ->
                                        if (shop.evidence.isNotBlank()) Text(shop.evidence, fontSize = 10.sp, color = AtlasColors.Muted, maxLines = 3, modifier = Modifier.padding(start = 12.dp, top = 4.dp))
                                        if(shop.matchReason.isNotBlank()) Text(shop.matchReason, fontSize = 10.sp, color = AtlasColors.Muted, maxLines = 2, modifier = Modifier.padding(start = 12.dp, top = 4.dp))
                                        TextButton(enabled = !busy, onClick = { locating = shop }) { Text(if(shop.place == null) "确认门店位置" else "调整门店", fontSize = 11.sp) }
                                    }
                                }
                            }
                        }
                    } else {
                        Text("记录你自己挑中的地点，来源链接会一并保留。", fontSize = 12.sp, color = AtlasColors.Muted)
                        AtlasField(name, { name = it; manualPoi = null }, "地点名称")
                        AtlasField(address, { address = it; manualPoi = null }, "地址 / 街道")
                        AtlasField(city, { reset(); city = it; manualPoi = null }, "城市")
                        AtlasField(source, { source = it }, "灵感来源链接（选填）")
                        TextButton(onClick = { manualLocator = true }, enabled = name.isNotBlank() && city.isNotBlank()) { Text(if(manualPoi == null) "在高德确认门店位置" else "已确认位置 · 重新选择") }
                    }
                    Text("放进收藏夹", fontSize = 12.sp, color = AtlasColors.Muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        controller.data.collections.forEach { folder ->
                            Tag(folder.name, folder.id in folders, { folders = if(folder.id in folders) folders - folder.id else folders + folder.id })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Row(Modifier.fillMaxWidth().background(AtlasColors.Card).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onClose, Modifier.height(50.dp).weight(1f), shape = RoundedCornerShape(14.dp)) { Text("先不收藏") }
                    PrimaryButton(if(tab == 0) "收藏已选 ${selected.size} 处" else "收藏这个地点", {
                        if(tab == 0) {
                            if(controller.importSelected(candidates.filter { key(it) in selected }, folders.toList()) > 0) onClose()
                        } else {
                            val p = manualPoi
                            val place = Place(controller.platform.uniqueId(), name.trim(), city.trim(), address = address.trim(),
                                district = p?.district.orEmpty(), latitude = p?.latitude, longitude = p?.longitude,
                                gcjLatitude = p?.gcjLatitude, gcjLongitude = p?.gcjLongitude, poiId = p?.id.orEmpty(),
                                collectionIds = folders.toList(), source = SourceLinks.persistent(source.trim()), addedOn = controller.platform.today())
                            if(controller.addPlace(place)) onClose()
                        }
                    }, Modifier.weight(1.4f), Glyph.PLUS, enabled = !busy && if(tab == 0) selected.isNotEmpty() else name.isNotBlank() && address.isNotBlank() && city.isNotBlank())
                }
            }
        }
    }
    locating?.let { shop -> PoiPicker(controller.platform, city, shop.name, shop.candidates, { p ->
        selected = emptySet(); note = note?.copy(shops = note!!.shops.map { if(it.id == shop.id) it.copy(place = p, matchReason = "手动确认高德门店") else it }); locating = null
    }, { locating = null }) }
    if(manualLocator) PoiPicker(controller.platform, city, name, emptyList(), { p -> manualPoi = p; address = p.address; manualLocator = false }, { manualLocator = false })
    if(serviceSettings) ServiceSettings(controller.platform, { serviceSettings = false })
    if(cityPicker) AlertDialog(onDismissRequest = { cityPicker = false }, containerColor = AtlasColors.Paper,
        title = { Text("店铺所在城市") }, text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("上海", "北京", "深圳", "广州", "杭州", "成都", "南京", "苏州", "重庆", "武汉").forEach { value ->
                    Tag(value, value == city, { reset(); city = value; manualPoi = null; cityPicker = false })
                }
            }
        }, confirmButton = { TextButton(onClick = { cityPicker = false }) { Text("返回填写其他城市") } })
}

@Composable
fun PoiPicker(platform: PlatformServices, city: String, initial: String, initialCandidates: List<ResolvedPoi>, onConfirm: (ResolvedPoi) -> Unit, onClose: () -> Unit) {
    var query by remember { mutableStateOf(initial) }
    var results by remember { mutableStateOf(initialCandidates) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var useAddress by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun search() { scope.launch {
        busy = true; error = null
        try { results = AtlasService(platform.transport).search(query.trim(), city, useAddress); if(results.isEmpty()) error = "没有找到匹配结果，试试分店名或完整地址。" }
        catch(e: CancellationException) { throw e }
        catch(e: Exception) { error = e.message }
        finally { busy = false }
    } }
    AlertDialog(onDismissRequest = onClose, containerColor = AtlasColors.Paper,
        title = { Text("确认门店位置", style = MaterialTheme.typography.titleLarge) },
        text = { Column(Modifier.heightIn(max = 510.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("$city · 选择对应分店后保存真实位置", fontSize = 12.sp, color = AtlasColors.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Tag("搜门店", !useAddress, { useAddress = false }); Tag("查详细地址", useAddress, { useAddress = true }) }
            AtlasField(query, { query = it }, if(useAddress) "完整地址及门牌号" else "店名 / 分店 / 路名")
            TextButton(onClick = { search() }, enabled = !busy && query.isNotBlank()) { Text(if(busy) "查询中…" else "搜索高德") }
            error?.let { Text(it, fontSize = 12.sp, color = AtlasColors.Muted) }
            results.forEach { p -> Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AtlasColors.Card).clickable { onConfirm(p) }.padding(12.dp)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium)
                Text(p.address, fontSize = 11.sp, color = AtlasColors.Muted)
                if(p.level.isNotBlank()) Text("地址精度：${p.level}，请核对后选择", fontSize = 10.sp, color = AtlasColors.Muted)
                Text("选择这个位置", fontSize = 11.sp, color = AtlasColors.Green, modifier = Modifier.padding(top = 8.dp))
            } }
        } }, confirmButton = { TextButton(onClick = onClose) { Text("返回") } })
}

@Composable
private fun ServiceSettings(platform: PlatformServices, onClose: () -> Unit) {
    var endpoint by remember { mutableStateOf(platform.serviceUrl) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = onClose, containerColor = AtlasColors.Paper, title = { Text("解析服务") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("使用已配置的解析服务读取笔记和查询门店。", fontSize = 12.sp, color = AtlasColors.Muted)
            AtlasField(endpoint, { endpoint = it }, "服务地址")
            status?.let { Text(it, fontSize = 12.sp, color = AtlasColors.Green) }
        }
    }, confirmButton = { TextButton(enabled = !busy && endpoint.isNotBlank(), onClick = {
        val before = platform.serviceUrl
        platform.serviceUrl = endpoint
        scope.launch {
            busy = true
            try {
                val config = platform.transport.request("/api/config")
                val map = (config["mapConfigured"] as? JsonPrimitive)?.booleanOrNull == true
                val ai = (config["aiConfigured"] as? JsonPrimitive)?.booleanOrNull == true
                status = "已连接 · ${if(map) "高德就绪" else "高德未配置"} · ${if(ai) "模型就绪" else "使用规则识别"}"
            } catch(e: CancellationException) { platform.serviceUrl = before; throw e }
            catch(e: Exception) { platform.serviceUrl = before; status = e.message }
            finally { busy = false }
        }
    }) { Text(if(busy) "连接中…" else "保存并检查") } }, dismissButton = { TextButton(onClick = onClose) { Text("完成") } })
}
