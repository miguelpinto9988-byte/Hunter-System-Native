package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/** Mantiene el contrato de MonarchGlyph pero utiliza exclusivamente PNG Pixel Art transparentes.
 * Las funciones y los accesos rápidos conservan sus claves históricas.
 */
internal fun monarchGlyphPixelKey(kind:String):String = when(kind) {
    "home" -> "panel"
    "plan" -> "missions"
    "routines" -> "routines"
    "book" -> "academy"
    "profile" -> "profile"
    else -> kind
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun MonarchGlyph(kind: String, modifier: Modifier = Modifier, color: Color = MonarchV42Palette.Cyan) {
    HunterPngIcon(monarchGlyphPixelKey(kind), modifier, kind)
}

/** Mantiene la API del piloto de fase 01, reemplazando su implementación por Fase 04. */
@Composable
fun MonarchHero(ui:NativeUi,stats:JSONObject) {
    MonarchDashboardHero(ui,stats)
}
