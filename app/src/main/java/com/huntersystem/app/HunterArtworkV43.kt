package com.huntersystem.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate

/**
 * Retratos aislados Monarca V43.12: selección persistida en appearance33.
 * No modifican ni el XP ni la progresión. Los cuatro maestros 4K se colocan
 * en app/src/main/assets/hunter-4k/{reader,regressor,hunter-male,hunter-female}.webp.
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
        val candidates = listOf(
            "hunter-4k/$key-$grade.webp", // evolución 4K específica, si está instalada
            "hunter-4k/$key.webp",        // retrato maestro 4K
            "ui34/$key-$grade.webp",      // legado de alta resolución
            "ui33/$key-$grade.webp",      // legado de todos los rangos
            "ui33/$key-e.webp"
        )
        candidates.firstOrNull { path ->
            runCatching { context.assets.open(path).use { true } }.getOrDefault(false)
        } ?: "ui33/$key-e.webp"
    }
}
