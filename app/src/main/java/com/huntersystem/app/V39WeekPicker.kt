package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Selector visual V39. Conserva el esquema weekPlan/weekFocus del generador nativo:
 * train+upper/lower/core, cardio, mobility, rest.
 */
enum class V39Training(val title: String, val symbol: String, val accent: Color) {
    UPPER("Tren superior", "▲", Color(0xFF237CFF)),
    LOWER("Tren inferior", "▼", Color(0xFFF44295)),
    CARDIO("Cardio", "♥", Color(0xFF00B79A)),
    MOBILITY("Movilidad", "✦", Color(0xFFF1B837)),
    CORE("Tren central", "◎", Color(0xFF8B51F5)),
    REST("Descanso", "☾", Color(0xFF7184A6));

    fun next(): V39Training = entries[(ordinal + 1) % entries.size]

    fun update(day: Int, week: SnapshotStateList<String>, focus: SnapshotStateList<String>) {
        when (this) {
            UPPER -> { week[day] = "train"; focus[day] = "upper" }
            LOWER -> { week[day] = "train"; focus[day] = "lower" }
            CORE -> { week[day] = "train"; focus[day] = "core" }
            CARDIO -> { week[day] = "cardio"; focus[day] = "cardio" }
            MOBILITY -> { week[day] = "mobility"; focus[day] = "mobility" }
            REST -> { week[day] = "rest"; focus[day] = "rest" }
        }
    }

    companion object {
        fun of(plan: String, focus: String): V39Training = when (plan) {
            "train" -> when (focus) {
                "lower" -> LOWER
                "core" -> CORE
                else -> UPPER
            }
            "cardio" -> CARDIO
            "mobility" -> MOBILITY
            else -> REST
        }
    }
}

@Composable
fun V39WeekPicker(
    week: SnapshotStateList<String>,
    focus: SnapshotStateList<String>,
    onChanged: () -> Unit
) {
    val days = listOf(1 to "Lun", 2 to "Mar", 3 to "Mié", 4 to "Jue",
        5 to "Vie", 6 to "Sáb", 0 to "Dom")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Heading("Elige tu semana", size = 26)
        Eyebrow("TOCA UN DÍA PARA CAMBIAR SU ENTRENAMIENTO")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for ((day, label) in days) {
                val active = V39Training.of(week[day], focus[day])
                Column(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(active.accent.copy(alpha = .21f))
                        .border(1.dp, active.accent.copy(alpha = .90f), RoundedCornerShape(12.dp))
                        .semantics { contentDescription = "$label: ${active.title}. Pulsar para cambiar" }
                        .clickable {
                            active.next().update(day, week, focus)
                            onChanged()
                        }
                        .padding(vertical = 10.dp, horizontal = 1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = Color.White, maxLines = 1)
                    Text(active.symbol, fontSize = 20.sp, color = active.accent)
                }
            }
        }
        // Los colores de actividad tienen significado fijo en todos los temas.
        for (pair in V39Training.entries.toList().chunked(2)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (item in pair) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(14.dp)
                            .clip(RoundedCornerShape(4.dp)).background(item.accent))
                        HText(item.title, size = 13, color = LocalHunter.current.text)
                    }
                }
            }
        }
        HText("Los cambios se aplican a sesiones pendientes; las actividades registradas se conservan.",
            size = 13, color = LocalHunter.current.muted)
    }
}
