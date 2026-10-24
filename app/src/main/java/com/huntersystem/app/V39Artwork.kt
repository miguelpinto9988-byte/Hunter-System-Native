package com.huntersystem.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/**
 * Portadas exclusivas disponibles para 12 ejercicios reales.
 * Si no existe una miniatura V39, conserva la demostración SVG original.
 * No reutiliza imágenes de ejercicios distintos ni reemplaza las animaciones.
 */
@Composable
fun V39Artwork(exercise: Exercise, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = LocalHunter.current
    val picture = remember(exercise.id) {
        runCatching {
            context.assets.open("v39-miniatures/ex_${exercise.id}.webp").use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull()
    }
    Box(modifier.clip(RoundedCornerShape(12.dp))
        .background(colors.surface), contentAlignment = Alignment.Center) {
        if (picture != null) {
            Image(bitmap = picture, contentDescription = "Ilustración de ${exercise.name}",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else if (exercise.demo.isNotBlank()) {
            OriginalSvg(exercise.demo, Modifier.fillMaxSize(),
                accent = colors.hex("accent"))
        }
    }
}
