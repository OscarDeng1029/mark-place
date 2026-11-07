package com.mylittleatlas.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleAddSheet(onClose:()->Unit,onWrite:()->Unit,onCollect:()->Unit) {
    ModalBottomSheet(onDismissRequest=onClose,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=androidx.compose.ui.graphics.Color(0xFFFDFCF8)) {
        Column(Modifier.fillMaxWidth().padding(horizontal=24.dp).padding(bottom=30.dp)) {
            Text("今天，想记下什么？",style=MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))
            SettingRow(Glyph.EDIT,"写手账","记下去过的地方，保存后自动成页",onWrite)
            HorizontalDivider(color=AtlasColors.Line)
            SettingRow(Glyph.BOOKMARK,"收藏地点","先收下喜欢的地方，等哪天去",onCollect)
        }
    }
}
