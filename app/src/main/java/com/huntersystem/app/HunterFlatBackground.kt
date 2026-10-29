package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Fondo global uniforme de Hunter System.
 *
 * Usa exclusivamente el color del tema seleccionado. Sin ilustraciones,
 * personajes ni sombras superpuestas en el fondo de las pantallas o diálogos.
 * Las imágenes propias de tarjetas, misiones Monarca e íconos no se alteran.
 */
@Composable
fun HunterFlatBackground(modifier:Modifier=Modifier) {
    Box(modifier.background(LocalHunter.current.bg))
}
