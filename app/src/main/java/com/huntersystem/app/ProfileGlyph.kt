package com.huntersystem.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private val glyphs = mapOf(
    "person" to (Color(0xFF258FE6) to "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4 21a8 6 0 0 1 16 0"),
    "calendar" to (Color(0xFF9A68F2) to "M4 6h16v15H4zM4 11h16M8 3v5m8-5v5m-8 7h2m4 0h2"),
    "ruler" to (Color(0xFF34BC91) to "M7 3h10v18H7zM7 7h5M7 12h3M7 17h5"),
    "weight" to (Color(0xFFE65260) to "M4 21 6 8h12l2 13ZM9 8a3 3 0 1 1 6 0"),
    "camera" to (Color(0xFFD9A24E) to "M3 7h5l2-3h4l2 3h5v13H3zM12 16a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"),
    "shield" to (Color(0xFF34BC91) to "M12 3 4 6v5c0 5 3 8 8 10 5-2 8-5 8-10V6zM9 12l2 2 4-4"),
    "bars" to (Color(0xFF258FE6) to "M4 20h4v-5H4zM10 20h4v-9h-4zM16 20h4V5h-4z"),
    "crown" to (Color(0xFFD9A24E) to "m3 7 5 5 4-7 4 7 5-5-2 12H5z"),
    "heart" to (Color(0xFFED72B8) to "M12 20 4 12C-1 6 7 2 12 8c5-6 13-2 8 4z"),
    "dumbbell" to (Color(0xFFD8732D) to "M3 10v4m4-7v10m10-10v10m4-7v4M7 12h10"),
    "pulse" to (Color(0xFFE65260) to "M3 13h4l3-6 4 12 3-6h4"),
    "move" to (Color(0xFF168887) to "M17 5a2 2 0 1 0 0-.01M5 19l5-4 2-5m-7 1 4-3 4 1 4 4h4m-11 2 5 5"),
    "check" to (Color(0xFF34BC91) to "M5 4h14v16H5zm3 8 3 3 5-6"),
    "flag" to (Color(0xFF258FE6) to "M5 21V4m0 1 5-2 4 2 5-2v10l-5 2-4-2-5 2"),
    "clock" to (Color(0xFFD9A24E) to "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Zm0-14v6l4 2"),
    "cycle" to (Color(0xFF168887) to "M18 8a7 7 0 1 0 1 8m0-10v5h-5"),
    "pen" to (Color(0xFF34BC91) to "m4 20 4-1L20 7l-3-3L5 16zM14 7l3 3"),
    "frame" to (Color(0xFFD9A24E) to "M4 9V4h5m6 0h5v5m0 6v5h-5m-6 0H4v-5"),
    "landscape" to (Color(0xFF34BC91) to "M3 5h18v14H3zM4 17l5-5 4 4 3-7 4 8"),
    "moon" to (Color(0xFF728EAE) to "M13 3a9 9 0 1 0 8 12 8 8 0 0 1-8-12Z"),
    "mat" to (Color(0xFF9A68F2) to "M4 18h14a3 3 0 1 0-3-3V6H7a3 3 0 0 0-3 3zM15 15a3 3 0 0 0 3 3"),
    "wall" to (Color(0xFFD8732D) to "M3 5h18v14H3zM3 10h18M3 14h18M9 5v5m6 0v4m-6 0v5"),
    "chair" to (Color(0xFFD9A24E) to "M7 4h10v8H7zM5 16h14M7 12v9m10-9v9"),
    "gym" to (Color(0xFF258FE6) to "M3 9 12 3l9 6M5 8v12h14V8m-9 12v-7h4v7"),
    "bike" to (Color(0xFF168887) to "M4 17a3 3 0 1 0 0-.01m13 .01a3 3 0 1 0 0-.01M7 17l4-7 4 7H7m2-7h6m2-3 2 10")
)

@Composable
internal fun ProfileGlyph(name: String, boxSize: Int = 20) {
    val (tone, data) = glyphs.getValue(name)
    val path = remember(data) { PathParser().parsePathString(data).toPath() }
    val shape = CutCornerShape(4.dp)
    Canvas(Modifier.size(boxSize.dp).background(tone.copy(alpha = .15f), shape)
        .border(1.dp, tone.copy(alpha = .66f), shape)) {
        val factor = size.width / 30f
        scale(factor, factor, pivot = center) {
            translate(left = center.x - 12f, top = center.y - 12f) {
                drawPath(path, tone, style = Stroke(width = 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
