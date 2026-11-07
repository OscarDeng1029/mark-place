package com.mylittleatlas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mylittleatlas.app.domain.*

internal val PersonalBackground = Color(0xFFF5F6F3)
private val PersonalSurface = Color.White
private val PersonalSecondary = Color(0xFF73796F)
private val PersonalDivider = Color(0xFFEEF0EA)

/** A readable account and tools overview; scrapbook compositions belong to the journal. */
@Composable
fun PersonalCenterV4(
    data: AtlasData, onEdit: () -> Unit, onSettings: () -> Unit, onBadges: () -> Unit,
    onDrafts: () -> Unit, onShare: () -> Unit, onJournal: () -> Unit,
    onCity: () -> Unit, onCollections: () -> Unit,
) {
    val visitedCities = data.places.filter { data.isVisited(it.id) }
        .map { it.city }.filter { it.isNotBlank() }.distinct().size
    val achievements = listOf(
        visitedCities > 0,
        data.places.any { it.kind == PlaceKind.COFFEE && data.isVisited(it.id) },
        data.visits.any { it.photoPaths.isNotEmpty() },
        data.places.count { data.isVisited(it.id) } >= 100,
    ).count { it }
    Box(Modifier.fillMaxSize().background(PersonalBackground), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 620.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 28.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("我的", Modifier.weight(1f), fontFamily = FontFamily.Default,
                    fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                IconButton(Glyph.SETTINGS, "设置", onSettings)
            }
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(PersonalSurface).clickable(role = Role.Button, onClick = onEdit)
                    .padding(18.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Photo(data.profileCover, Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)))
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f)) {
                    Text(data.nickname, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(data.signature.ifBlank { "添加一句话介绍" }, fontSize = 12.sp,
                        lineHeight = 18.sp, color = PersonalSecondary, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                    Text("编辑个人资料", fontSize = 11.sp, color = AtlasColors.Green,
                        modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.width(8.dp))
                AtlasIcon(Glyph.ARROW, color = PersonalSecondary, size = 18.dp)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(PersonalSurface)
                    .height(IntrinsicSize.Min).padding(vertical = 18.dp),
            ) {
                PersonalStat(data.places.size, "收藏地点", onCollections, Modifier.weight(1f))
                VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 7.dp), color = PersonalDivider)
                PersonalStat(data.visits.size, "到访记录", onJournal, Modifier.weight(1f))
                VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 7.dp), color = PersonalDivider)
                PersonalStat(visitedCities, "探索城市", onCity, Modifier.weight(1f))
            }
            Spacer(Modifier.height(22.dp))
            PersonalSectionLabel("我的内容")
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(PersonalSurface)) {
                PersonalMenuRow(Glyph.FOLDER, "我的收藏夹", data.collections.size.toString() + " 个", onCollections)
                PersonalMenuDivider()
                PersonalMenuRow(Glyph.BOOK, "足迹记录", data.visits.size.toString() + " 条", onJournal)
                PersonalMenuDivider()
                PersonalMenuRow(Glyph.MAP, "城市图鉴", visitedCities.toString() + " 座已到访", onCity)
                PersonalMenuDivider()
                PersonalMenuRow(Glyph.SPARKLE, "我的勋章", achievements.toString() + " 枚已解锁", onBadges)
            }
            Spacer(Modifier.height(22.dp))
            PersonalSectionLabel("管理与分享")
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(PersonalSurface)) {
                PersonalMenuRow(Glyph.EDIT, "草稿箱", if (data.drafts.isEmpty()) "暂无草稿" else data.drafts.size.toString() + " 篇待完成", onDrafts)
                PersonalMenuDivider()
                PersonalMenuRow(Glyph.SHARE, "分享我的图鉴", "", onShare)
                PersonalMenuDivider()
                PersonalMenuRow(Glyph.SETTINGS, "设置与备份", "", onSettings)
            }
            Row(Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                AtlasIcon(Glyph.LOCK, color = PersonalSecondary, size = 13.dp)
                Text("数据保存在此设备", fontSize = 11.sp, color = PersonalSecondary)
            }
        }
    }
}

@Composable
private fun PersonalStat(value: Int, label: String, onClick: () -> Unit, modifier: Modifier) {
    Column(modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
        Text(label, fontSize = 11.sp, color = PersonalSecondary, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun PersonalSectionLabel(title: String) {
    Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PersonalSecondary,
        modifier = Modifier.padding(start = 3.dp, bottom = 10.dp))
}

@Composable
private fun PersonalMenuRow(icon: Glyph, title: String, detail: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        AtlasIcon(icon, size = 21.dp, color = AtlasColors.Green)
        Text(title, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
        if (detail.isNotEmpty()) Text(detail, fontSize = 11.sp, color = PersonalSecondary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 115.dp))
        AtlasIcon(Glyph.BACK, Modifier.padding(start = 9.dp).rotate(180f),
            color = PersonalSecondary, size = 14.dp)
    }
}

@Composable
private fun PersonalMenuDivider() {
    HorizontalDivider(Modifier.padding(start = 49.dp, end = 16.dp), color = PersonalDivider)
}
