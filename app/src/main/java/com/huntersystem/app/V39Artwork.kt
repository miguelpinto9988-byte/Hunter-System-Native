package com.huntersystem.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Miniaturas 39: asociación por ID EXACTO y carga diferida, sin alterar ejercicios.
 *
 * El manifiesto `catalogo_miniaturas_1200.json` contiene las rutas WebP reales.
 * Se carga una sola vez y se cachean las imágenes redimensionadas en un LRU acotado.
 * Si falta un recurso, se conserva la demostración SVG original.
 * Las imágenes son ilustraciones orientativas, no instrucciones técnicas de ejecución.
 */
private object ExerciseThumbnailCache {
    private var loaded = false
    private var paths: Map<String, String> = emptyMap()
    private val bitmapCache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    @Synchronized
    private fun getPaths(context: Context): Map<String, String> {
        if (!loaded) {
            paths = runCatching {
                val text = context.assets.open("catalogo_miniaturas_1200.json")
                    .bufferedReader().use { it.readText() }
                val json = JSONArray(text)
                buildMap {
                    for (i in 0 until json.length()) {
                        val row = json.getJSONObject(i)
                        val id = row.getString("id")
                        val imagePath = row.getString("imagen")
                        if (id.isNotBlank() &&
                            imagePath.matches(Regex("exercise_thumbnails/[A-Za-z0-9_-]+\\.webp"))) {
                            put(id, imagePath)
                        }
                    }
                }
            }.getOrDefault(emptyMap())
            loaded = true
        }
        return paths
    }

    fun load(context: Context, id: String): Bitmap? {
        synchronized(bitmapCache) { bitmapCache.get(id)?.let { return it } }
        val path = getPaths(context)[id] ?: return null
        val bitmap = runCatching {
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                    inScaled = false
                })
            }
        }.getOrNull() ?: return null
        synchronized(bitmapCache) {
            bitmapCache.get(id)?.let { return it }
            bitmapCache.put(id, bitmap)
        }
        return bitmap
    }
}

/**
 * Utilizar tanto en el listado como en el detalle y sesiones.
 * Compatible con ejercicios base, variantes y el selector de tema existente.
 */
@Composable
fun V39Artwork(exercise: Exercise, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val colors = LocalHunter.current
    val bitmap by produceState<Bitmap?>(initialValue = null, exercise.id, context) {
        value = withContext(Dispatchers.IO) {
            ExerciseThumbnailCache.load(context, exercise.id)
        }
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(colors.surface),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Ilustración orientativa de ${exercise.name}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (exercise.demo.isNotBlank()) {
            OriginalSvg(
                exercise.demo,
                Modifier.fillMaxSize(),
                accent = colors.hex("accent")
            )
        }
    }
}
