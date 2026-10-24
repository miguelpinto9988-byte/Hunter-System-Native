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

/** Mantiene la API del piloto de fase 01, reemplazando su implementación por Fase 04. */
@Composable
fun MonarchHero(ui:NativeUi,stats:JSONObject) {
    MonarchDashboardHero(ui,stats)
}
