package com.mylittleatlas.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mylittleatlas.app.domain.*

@Composable
fun VisitSavedV2(place:Place,visit:Visit,onJournal:()->Unit,onClose:()->Unit){
    AtlasFullPage("这一刻，已经收好",onClose){
        ScrapbookBadge(3,null,Modifier.size(85.dp).align(Alignment.CenterHorizontally))
        Text("又留住了一段小美好",style=MaterialTheme.typography.headlineMedium)
        Text(visit.date+" · "+place.name,fontSize=12.sp,color=AtlasColors.Muted)
        PolaroidPhoto(place.cover,Modifier.fillMaxWidth().height(225.dp),caption="a little memory to keep",localPath=visit.photoPaths.firstOrNull())
        Text("这次到访已经出现在你的足迹中。想再添一点自己的样子，可以到手账里调整排版。",fontSize=13.sp,lineHeight=23.sp,color=AtlasColors.Muted)
        PrimaryButton("看看这一页手账",onJournal,Modifier.fillMaxWidth(),Glyph.BOOK)
        TextButton(onClick=onClose,modifier=Modifier.align(Alignment.CenterHorizontally)){Text("先收好，继续逛逛")}
    }
}
