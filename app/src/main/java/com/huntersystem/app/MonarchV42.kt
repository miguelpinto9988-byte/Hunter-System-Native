package com.huntersystem.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/** Monarca A+ V42: identidad original. No cambia lógica de entrenamientos ni datos. */
object MonarchV42Palette {
    val Obsidian = Color(0xFF080D19)
    val Night = Color(0xFF111C31)
    val Surface = Color(0xFF18263F)
    val Cyan = Color(0xFF25C8FF)
    val Violet = Color(0xFF7F5CFF)
    val Text = Color(0xFFE8F1FF)
    val Muted = Color(0xFFB4C6DB)
    val Border = Color(0xFF36506C)
    val Lower = Color(0xFFF39DB8)
    val Cardio = Color(0xFFEDA55C)
    val Mobility = Color(0xFF6DDFB5)
}

object MonarchV42Metrics {
    val Padding = 18.dp
    val Radius = 26.dp
    val ButtonHeight = 48.dp
    const val TransitionMs = 220
}

/** Iconografía coherente, sin fuentes externas ni símbolos copiados de franquicias. */
@Composable
fun MonarchGlyph(kind: String, modifier: Modifier = Modifier, color: Color = MonarchV42Palette.Cyan) {
    Canvas(modifier) {
        val unit = minOf(size.width, size.height) / 24f
        val width = 1.8f * unit
        fun line(x0: Float, y0: Float, x1: Float, y1: Float) {
            drawLine(color, Offset(x0 * unit, y0 * unit), Offset(x1 * unit, y1 * unit),
                strokeWidth = width, cap = StrokeCap.Round)
        }
        fun outline(vararg coords: Float) {
            val p = Path().apply {
                moveTo(coords[0] * unit, coords[1] * unit)
                for (i in 2 until coords.size step 2) {
                    lineTo(coords[i] * unit, coords[i + 1] * unit)
                }
            }
            drawPath(p, color, style = Stroke(width = width,
                cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        when (kind) {
            "home" -> {
                outline(3f, 11f, 12f, 3f, 21f, 11f, 21f, 20f, 3f, 20f, 3f, 11f)
                line(10f, 20f, 10f, 14f); line(14f, 20f, 14f, 14f)
            }
            "plan" -> {
                outline(12f, 2f, 21f, 11f, 12f, 22f, 3f, 11f, 12f, 2f)
                line(9f, 12f, 11f, 14f); line(11f, 14f, 16f, 9f)
            }
            "routines" -> {
                line(4f, 8f, 20f, 8f); line(4f, 16f, 20f, 16f)
                line(7f, 5f, 7f, 11f); line(17f, 13f, 17f, 19f)
            }
            "book" -> {
                outline(3f, 5f, 10f, 4f, 12f, 6f, 14f, 4f, 21f, 5f, 21f, 20f,
                    14f, 19f, 12f, 21f, 10f, 19f, 3f, 20f, 3f, 5f)
                line(12f, 6f, 12f, 21f)
            }
            "profile" -> {
                drawCircle(color, 3.6f * unit, Offset(12f * unit, 7.8f * unit),
                    style = Stroke(width))
                outline(4f, 20f, 4f, 17f, 7f, 14f, 12f, 13f, 17f, 14f,
                    20f, 17f, 20f, 20f)
            }
            else -> drawCircle(color, 6f * unit, Offset(12f * unit, 12f * unit),
                style = Stroke(width))
        }
    }
}

/** Piloto de pantalla principal con datos reales y acompañante seleccionado en Perfil. */
@Composable
fun MonarchHero(ui: NativeUi, stats: JSONObject) {
    val store = ui.store
    val profile = store.profile ?: JSONObject()
    val palette = MonarchV42Palette
    val level = stats.i("level", 1).coerceAtLeast(1)
    val rank = rankKey(level)
    val xp = stats.i("xp").coerceAtLeast(0)
    val day = store.day(today())
    val progress = (xp % 300) / 300f
    val characterAsset = "ui33/companion-" + companionKey(store.profile) + ".webp"

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(MonarchV42Metrics.Radius))
            .background(Brush.verticalGradient(listOf(palette.Night, palette.Obsidian)))
            .border(1.dp, palette.Border, RoundedCornerShape(MonarchV42Metrics.Radius))
            .padding(MonarchV42Metrics.Padding),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("◆  SISTEMA DEL CAZADOR")
            HText("RANGO $rank", size = 14, color = palette.Cyan, bold = true)
        }
        Box(Modifier.fillMaxWidth().height(245.dp)) {
            Box(Modifier.align(Alignment.CenterEnd).size(218.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Brush.radialGradient(listOf(
                    palette.Violet.copy(alpha = .45f), palette.Cyan.copy(alpha = .17f),
                    palette.Obsidian.copy(alpha = 0f)))))
            AssetImage(characterAsset,
                Modifier.align(Alignment.CenterEnd).width(178.dp).height(235.dp),
                scale = ContentScale.Fit,
                description = "Acompañante elegido en Perfil")
            Column(Modifier.align(Alignment.BottomStart).widthIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HText("ASCENSO EN CURSO", size = 12, color = palette.Cyan, bold = true)
                Heading("NIVEL\n" + level.toString().padStart(2, '0'), size = 42)
                HText(profile.s("name", "Cazador").take(32), size = 16,
                    color = palette.Text, bold = true)
                HText("Una misión a la vez.", size = 14, color = palette.Muted)
            }
        }
        HText("EXPERIENCIA · $xp XP", size = 14, color = palette.Muted, bold = true)
        HProgress(progress)
        HText((xp % 300).toString() + " / 300 XP para el siguiente umbral",
            size = 13, color = palette.Muted)
        HText("MISIÓN DEL DÍA", size = 13, color = palette.Cyan, bold = true)
        HText(day.s("title", "Entrenamiento del día"), size = 22,
            color = palette.Text, bold = true, maxLines = 2)
        HButton("COMENZAR MISIÓN  →", modifier = Modifier.fillMaxWidth(),
            primary = true) { ui.open("mission", obj("date" to today())) }
    }
}
