package com.huntersystem.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate

/**
 * Retratos aislados Monarca V43.12: selección persistida en appearance33.
 * No modifican ni el XP ni la progresión. Las cuatro versiones HD se generan
 * en app/src/main/assets/hunter-hd/{reader,regressor,hunter-male,hunter-female}.webp.
 * Se aceptan futuras variantes por rango (-e, -d, -c, -b, -a, -s).
 * Mantiene las 24 ilustraciones históricas ui33/ui34 como fallback local.
 */
internal fun hunterPortraitKey(collection: String, gender: String, day: Long = LocalDate.now().toEpochDay()): String {
    val actual = if (collection == "alternate") {
        listOf("hunter", "reader", "regressor")[(day % 3).toInt()]
    } else collection
    return when (actual) {
        "reader" -> "reader"
        "regressor" -> "regressor"
        else -> if (gender == "female") "hunter-female" else "hunter-male"
    }
}

internal fun hunterPortraitTitle(collection: String, gender: String): String =
    when (hunterPortraitKey(collection, gender)) {
        "reader" -> "Kim Dokja"
        "regressor" -> "Yoo Joonghyuk"
        "hunter-female" -> "Cazadora"
        else -> "Cazador"
    }

@Composable
internal fun resolvedHunterArtwork(collection: String, gender: String, rank: String = "e"): String {
    val context = LocalContext.current
    val key = hunterPortraitKey(collection, gender)
    val grade = rank.lowercase().takeIf { it in setOf("e", "d", "c", "b", "a", "s") } ?: "e"
    return remember(context, key, grade) {
        // Para el rango E se muestra el retrato HD optimizado. Los rangos D-S
        // mantienen sus evoluciones propias hasta incorporar nuevas ilustraciones HD para cada rango.
        val candidates = if (grade == "e") listOf(
            "hunter-hd/$key-e.webp",
            "hunter-hd/$key.webp",
            "ui34/$key-e.webp",
            "ui33/$key-e.webp"
        ) else listOf(
            "hunter-hd/$key-$grade.webp",
            "ui34/$key-$grade.webp",
            "ui33/$key-$grade.webp",
            "hunter-hd/$key.webp",
            "ui33/$key-e.webp"
        )
        candidates.firstOrNull { path ->
            runCatching { context.assets.open(path).use { true } }.getOrDefault(false)
        } ?: "ui33/$key-e.webp"
    }
}
