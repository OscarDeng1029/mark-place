package com.mylittleatlas.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mylittleatlas.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import kotlin.random.Random

object AtlasColors {
    val Paper = Color(0xFFF7F2E9)
    val Card = Color(0xFFFFFAF1)
    val Ink = Color(0xFF3E4639)
    val Green = Color(0xFF53634E)
    val Sage = Color(0xFF75836A)
    val SagePale = Color(0xFFE7EADD)
    val Peach = Color(0xFFC58360)
    val PeachPale = Color(0xFFF0DAC2)
    val Blue = Color(0xFFA8BBC1)
    val BluePale = Color(0xFFE2E8E8)
    val Muted = Color(0xFF8B877C)
    val Line = Color(0xFFE5DFD2)
}

@Composable
fun AtlasTheme(content: @Composable () -> Unit) {
    val serif = FontFamily(Font(Res.font.atlas_serif))
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AtlasColors.Green, onPrimary = AtlasColors.Card,
            secondary = AtlasColors.Peach, background = AtlasColors.Paper,
            surface = AtlasColors.Card, onSurface = AtlasColors.Ink,
            onBackground = AtlasColors.Ink, outline = AtlasColors.Line,outlineVariant=AtlasColors.Line,
            surfaceVariant = AtlasColors.SagePale,
        ),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = serif, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp),
            headlineMedium = TextStyle(fontFamily = serif, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 34.sp),
            titleLarge = TextStyle(fontFamily = serif, fontWeight = FontWeight.Medium, fontSize = 21.sp, lineHeight = 29.sp),
            titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp),
            bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 21.sp),
            labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
            labelMedium = TextStyle(fontSize = 11.sp, letterSpacing = 0.4.sp),
        ),
        shapes = Shapes(medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
        content = {
            CompositionLocalProvider(LocalContentColor provides AtlasColors.Ink) {
                ProvideTextStyle(TextStyle(fontSize=13.sp,lineHeight=18.sp,color=AtlasColors.Ink)) { content() }
            }
        },
    )
}

@Composable
fun Paper(modifier: Modifier = Modifier, textured: Boolean = false, color: Color = AtlasColors.Paper, content: @Composable BoxScope.() -> Unit) {
    com.mylittleatlas.app.platform.AtlasWindowStyle()
    Box(modifier.background(color)) {
        if (textured) AtlasPaperTexture(Modifier.matchParentSize())
        content()
    }
}

enum class Glyph { HOME, MAP, BOOKMARK, HEART, PERSON, PLUS, BOOK, SEARCH, PIN, ARROW, BACK, CLOSE, CHECK, MORE, LAYERS, NAVIGATE, SHARE, CAMERA, FOLDER, COFFEE, DOWNLOAD, SETTINGS, LOCK, SPARKLE, LIST, EDIT }

@Composable
fun AtlasIcon(glyph: Glyph, modifier: Modifier = Modifier, color: Color = AtlasColors.Ink, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val scale = this.size.width / 24f
        fun pt(x: Float, y: Float) = Offset(x * scale, y * scale)
        val stroke = Stroke(1.55f * scale, cap = StrokeCap.Round)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color, pt(x1,y1), pt(x2,y2), stroke.width, StrokeCap.Round)
        fun path(vararg p: Pair<Float, Float>, close: Boolean = false) {
            val shape = Path().apply { moveTo(p[0].first * scale, p[0].second * scale); p.drop(1).forEach { lineTo(it.first * scale,it.second * scale) }; if(close) close() }
            drawPath(shape, color, style = stroke)
        }
        when(glyph) {
            Glyph.HOME -> { path(3f to 10f,12f to 3f,21f to 10f); path(5f to 9f,5f to 21f,10f to 21f,10f to 15f,14f to 15f,14f to 21f,19f to 21f,19f to 9f) }
            Glyph.MAP -> { path(3f to 5f,9f to 3f,15f to 5f,21f to 3f,21f to 19f,15f to 21f,9f to 19f,3f to 21f,close=true);line(9f,3f,9f,19f);line(15f,5f,15f,21f) }
            Glyph.BOOKMARK -> { path(6f to 3f,18f to 3f,18f to 21f,12f to 17f,6f to 21f,close=true) }
            Glyph.HEART -> { val p=Path().apply {moveTo(12*scale,21*scale);cubicTo(3*scale,15*scale,0f,9*scale,5*scale,5*scale);cubicTo(8*scale,2*scale,11*scale,5*scale,12*scale,7*scale);cubicTo(15*scale,1*scale,22*scale,4*scale,22*scale,9*scale);cubicTo(22*scale,14*scale,16*scale,18*scale,12*scale,21*scale)};drawPath(p,color,style=stroke) }
            Glyph.PERSON -> {drawCircle(color,3.4f*scale,pt(12f,7f),style=stroke);drawArc(color,180f,180f,false,pt(4f,14f),Size(16*scale,14*scale),style=stroke);line(4f,21f,20f,21f)}
            Glyph.BOOK -> { path(3f to 4f,10f to 4f,12f to 6f,14f to 4f,21f to 4f,21f to 21f,14f to 21f,12f to 23f,10f to 21f,3f to 21f,close=true);line(12f,6f,12f,22f) }
            Glyph.PLUS -> {line(5f,12f,19f,12f);line(12f,5f,12f,19f)}
            Glyph.SEARCH -> {drawCircle(color,6.7f*scale,pt(10f,10f),style=stroke);line(15f,15f,21f,21f)}
            Glyph.PIN -> {val p=Path().apply {moveTo(12*scale,22*scale);cubicTo(8*scale,17*scale,4*scale,13*scale,4*scale,9*scale);cubicTo(4*scale,-1f*scale,20*scale,-1f*scale,20*scale,9*scale);cubicTo(20*scale,13*scale,16*scale,17*scale,12*scale,22*scale)};drawPath(p,color,style=stroke);drawCircle(color,2.5f*scale,pt(12f,9f),style=stroke)}
            Glyph.ARROW -> {line(4f,12f,20f,12f);path(15f to 7f,20f to 12f,15f to 17f)}
            Glyph.BACK -> {path(15f to 5f,8f to 12f,15f to 19f)}
            Glyph.CLOSE -> {line(6f,6f,18f,18f);line(6f,18f,18f,6f)}
            Glyph.CHECK -> path(5f to 12f,10f to 17f,20f to 6f)
            Glyph.MORE -> (5..19 step 7).forEach {drawCircle(color,1.4f*scale,pt(it.toFloat(),12f))}
            Glyph.LAYERS -> {path(3f to 8f,12f to 3f,21f to 8f,12f to 13f,close=true);path(3f to 12f,12f to 17f,21f to 12f);path(3f to 16f,12f to 21f,21f to 16f)}
            Glyph.NAVIGATE -> path(3f to 11f,21f to 3f,14f to 21f,11f to 13f,close=true)
            Glyph.SHARE -> {path(8f to 7f,12f to 3f,16f to 7f);line(12f,3f,12f,15f);path(6f to 11f,4f to 11f,4f to 21f,20f to 21f,20f to 11f,18f to 11f)}
            Glyph.CAMERA -> {drawRoundRect(color,pt(3f,7f),Size(18*scale,14*scale),CornerRadius(2*scale),style=stroke);path(7f to 7f,9f to 3f,15f to 3f,17f to 7f);drawCircle(color,4*scale,pt(12f,14f),style=stroke)}
            Glyph.FOLDER -> path(3f to 6f,10f to 6f,12f to 9f,21f to 9f,21f to 20f,3f to 20f,close=true)
            Glyph.COFFEE -> {path(4f to 8f,4f to 16f,6f to 19f,14f to 19f,16f to 16f,16f to 8f,close=true);path(16f to 9f,21f to 9f,21f to 14f,16f to 14f);line(3f,22f,18f,22f);line(7f,2f,7f,5f);line(12f,2f,12f,5f)}
            Glyph.DOWNLOAD -> {line(12f,3f,12f,16f);path(7f to 11f,12f to 16f,17f to 11f);path(3f to 17f,3f to 21f,21f to 21f,21f to 17f)}
            Glyph.SETTINGS -> {drawCircle(color,8*scale,pt(12f,12f),style=stroke);drawCircle(color,3*scale,pt(12f,12f),style=stroke);line(12f,1f,12f,4f);line(12f,20f,12f,23f);line(1f,12f,4f,12f);line(20f,12f,23f,12f)}
            Glyph.LOCK -> {drawRoundRect(color,pt(5f,10f),Size(14*scale,12*scale),CornerRadius(2*scale),style=stroke);drawArc(color,180f,180f,false,pt(8f,2f),Size(8*scale,14*scale),style=stroke);drawCircle(color,1*scale,pt(12f,16f))}
            Glyph.SPARKLE -> path(12f to 2f,15f to 9f,22f to 12f,15f to 15f,12f to 22f,9f to 15f,2f to 12f,9f to 9f,close=true)
            Glyph.LIST -> (6..18 step 6).forEach {line(8f,it.toFloat(),21f,it.toFloat());drawCircle(color,1f*scale,pt(3f,it.toFloat()))}
            Glyph.EDIT -> {path(4f to 16f,16f to 4f,20f to 8f,8f to 20f,3f to 21f,close=true);line(13f,7f,17f,11f)}
        }
    }
}

@Composable
fun IconButton(glyph: Glyph, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = false) {
    Box(modifier.size(44.dp).clip(CircleShape)
        .background(if(filled) AtlasColors.Card.copy(alpha=.94f) else Color.Transparent)
        .clickable(onClick = onClick).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        AtlasIcon(glyph)
    }
}

@Composable
fun Photo(cover: String, modifier: Modifier = Modifier, description: String? = null) {
    if(cover.startsWith("local:")) {
        com.mylittleatlas.app.platform.LocalPhoto(cover.removePrefix("local:"),modifier)
        return
    }
    val resource = when(cover) {
        "coffee" -> Res.drawable.atlas_photo_coffee_table; "bookshop" -> Res.drawable.atlas_photo_bookshop
        "dessert" -> Res.drawable.atlas_photo_dessert; "city" -> Res.drawable.atlas_photo_city_walk
        "interior" -> Res.drawable.atlas_photo_cafe_interior; else -> Res.drawable.atlas_photo_cafe_exterior
    }
    Image(painterResource(resource), description, modifier, contentScale = ContentScale.Crop)
}

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if(action != null) TextButton(onClick = onAction, contentPadding = PaddingValues(start=8.dp)) {
            Text(action, color=AtlasColors.Muted, fontSize=12.sp); Spacer(Modifier.width(4.dp)); AtlasIcon(Glyph.ARROW,size=14.dp,color=AtlasColors.Muted)
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Glyph? = null, enabled: Boolean = true) {
    Button(onClick, modifier.height(48.dp), enabled=enabled, shape=RoundedCornerShape(15.dp),
        contentPadding=PaddingValues(horizontal=20.dp), colors=ButtonDefaults.buttonColors(containerColor=AtlasColors.Green)) {
        if(icon != null) {AtlasIcon(icon,color=AtlasColors.Card,size=18.dp); Spacer(Modifier.width(9.dp))}
        Text(text, fontSize=14.sp,color=AtlasColors.Card)
    }
}

@Composable
fun Tag(text: String, selected: Boolean = false, onClick: (() -> Unit)? = null, tint: Color = AtlasColors.SagePale) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(if(selected) AtlasColors.Green else tint)
        .then(if(onClick != null) Modifier.clickable(onClick=onClick) else Modifier)
        .padding(horizontal=14.dp,vertical=8.dp)) {
        Text(text, fontSize=12.sp, color=if(selected) AtlasColors.Card else AtlasColors.Ink)
    }
}

@Composable
fun EmptyPage(title: String, subtitle: String, action: String? = null, onAction: () -> Unit = {}, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical=56.dp,horizontal=24.dp), horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(76.dp).background(AtlasColors.SagePale,CircleShape), contentAlignment=Alignment.Center) {AtlasIcon(Glyph.BOOKMARK,size=30.dp,color=AtlasColors.Green)}
        Spacer(Modifier.height(20.dp));Text(title,style=MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp));Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=AtlasColors.Muted)
        if(action != null) {Spacer(Modifier.height(22.dp));PrimaryButton(action,onAction)}
    }
}

@Composable
fun Handwritten(text: String, modifier: Modifier = Modifier, color: Color = AtlasColors.Green, size: Int = 17) {
    Text(text,modifier,fontFamily=FontFamily.Cursive,fontStyle=FontStyle.Italic,fontSize=size.sp,color=color,lineHeight=(size+5).sp)
}

@Composable
fun Tape(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.atlas_tape_oat), null, modifier.size(88.dp,22.dp), contentScale=ContentScale.FillBounds)
}

@Composable
fun ThinDivider(modifier: Modifier = Modifier) = HorizontalDivider(modifier,color=AtlasColors.Line,thickness=1.dp)
