package com.huntersystem.app

import android.content.Context
import android.graphics.Picture
import android.graphics.RectF
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.caverock.androidsvg.SVG

internal enum class ExerciseGymBackdrop(val asset: String) {
    FRONT("exercise-backdrops/front.svg"),
    SIDE("exercise-backdrops/side.svg"),
    OVERHEAD("exercise-backdrops/overhead.svg");

    fun load(context: Context, light: Boolean): Picture? = runCatching {
        val art = context.assets.open(asset).bufferedReader().use { it.readText() }
        val themed = if (light) paletteColor.replace(art) { lightPalette.getValue(it.value) } else art
        SVG.getFromString(themed).renderToPicture()
    }.getOrNull()

    companion object {
        fun forView(view: ExerciseView): ExerciseGymBackdrop = when (view) {
            ExerciseView.FRONT, ExerciseView.REAR -> FRONT
            ExerciseView.SIDE -> SIDE
            ExerciseView.OVERHEAD -> OVERHEAD
        }

        private val paletteColor = Regex("#[0-9a-f]{6}(?![0-9a-f])")
        private val lightPalette = mapOf(
            "#213748" to "#e1e8f0", "#152836" to "#cad7e2", "#253e4b" to "#cfdae3",
            "#122735" to "#e1e9ef", "#435f70" to "#bacfdd", "#29424f" to "#a6c0d0",
            "#41606c" to "#839ba9", "#557984" to "#8ca5b3", "#8badbc" to "#ffffff",
            "#34505d" to "#a3b4bf", "#172c39" to "#b3c4d0", "#678795" to "#dce6ef",
            "#577580" to "#93a9b7", "#1c303e" to "#aabbc6", "#496876" to "#8ea5b2",
            "#0f212e" to "#a4b8c5"
        )
    }
}

internal fun DrawScope.drawGymBackdrop(picture: Picture) {
    val fillScale = maxOf(size.width / picture.width, size.height / picture.height)
    val width = picture.width * fillScale
    val height = picture.height * fillScale
    val left = (size.width - width) / 2f
    val top = (size.height - height) / 2f
    clipRect {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawPicture(picture, RectF(left, top, left + width, top + height))
        }
    }
}
