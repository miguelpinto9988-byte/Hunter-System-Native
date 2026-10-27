package com.huntersystem.app

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sin

/**
 * Arte y movimiento de arranque de Hunter System.
 * La imagen aprobada contiene su propio logotipo, tipografía y barra decorativa.
 * No se dibujan otros logos ni porcentajes encima; el arranque termina al cargar datos.
 * Adaptado a preferencias de movimiento reducido.
 */
@Composable
fun HunterAnimatedBootArtwork(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reducedMotion = LocalMonarchMotion.current.reduced
    val image = remember(context) {
        runCatching {
            context.assets.open("monarch_splash_background.webp").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
    val transition = rememberInfiniteTransition(label = "hunter-splash-ambient")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hunter-splash-breathing"
    )
    val motion = if (reducedMotion) 0f else pulse
    Box(modifier.background(Color(0xFF06091A))) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    val scale = 1.007f + 0.010f * motion
                    scaleX = scale
                    scaleY = scale
                    translationY = -3.5f * motion
                }
            )
        }
        if (!reducedMotion) {
            Canvas(Modifier.fillMaxSize()) {
                val unit = size.minDimension
                drawCircle(
                    color = Color(0xFF3F78FB).copy(alpha = 0.025f + .05f * motion),
                    radius = unit * .22f,
                    center = Offset(size.width * .50f, size.height * .27f),
                    blendMode = BlendMode.Screen
                )
                drawCircle(
                    color = Color(0xFF37CFFF).copy(alpha = .035f + .075f * motion),
                    radius = unit * .11f,
                    center = Offset(size.width * .49f, size.height * .50f),
                    blendMode = BlendMode.Screen
                )
                // Destello visual sobre la barra impresa en la ilustración.
                drawCircle(
                    color = Color(0xFF93F5FF).copy(alpha = .2f + .40f * motion),
                    radius = unit * .010f,
                    center = Offset(size.width * (.22f + .56f * motion), size.height * .708f),
                    blendMode = BlendMode.Screen
                )
                // Partículas de energía discretas; sin carga de imágenes adicionales.
                for (i in 0 until 20) {
                    val fx = ((i * 79 + 19) % 101) / 101f
                    val fy = ((i * 43 + 31) % 97) / 97f
                    val phase = (i * .37f) + motion * 3.14159f
                    val x = size.width * fx
                    val y = size.height * (.12f + .78f * fy) +
                        sin(phase.toDouble()).toFloat() * unit * .014f
                    drawCircle(
                        color = Color(0xFF77BEFF).copy(alpha = .12f + .11f * motion),
                        radius = unit * (.0012f + (i % 3) * .0007f),
                        center = Offset(x, y),
                        blendMode = BlendMode.Screen
                    )
                }
            }
        }
    }
}
