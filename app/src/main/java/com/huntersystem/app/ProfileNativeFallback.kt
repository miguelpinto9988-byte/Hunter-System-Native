package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

/**
 * Offline profile recovery: does not create a new store or clear saved data.
 * Calls the same ProfileRegistration.submit() used by the original HTML profile.
 */
@Composable
internal fun ProfileNativeFallback(
    ui: NativeUi,
    registration: ProfileRegistration,
    onRetry: () -> Unit
) {
    val initial = remember(registration) { registration.initialData() }
    val saved = initial.o("fields")
    var name by remember(registration) { mutableStateOf(saved.s("name")) }
    var age by remember(registration) { mutableStateOf(saved.s("age")) }
    var goal by remember(registration) { mutableStateOf(saved.s("objective", "general")) }
    var difficulty by remember(registration) { mutableStateOf(saved.s("difficulty", "basic")) }
    var mode by remember(registration) { mutableStateOf(saved.s("trainingMode", "challenges")) }
    var minutes by remember(registration) { mutableStateOf(saved.s("minutes", "30")) }
    var theme by remember(registration) { mutableStateOf(saved.s("theme", "light")) }
    var bodyGear by remember(registration) { mutableStateOf(initial.o("gear").b("body", true)) }
    var dumbbells by remember(registration) { mutableStateOf(initial.o("gear").b("db", false)) }
    var gym by remember(registration) { mutableStateOf(initial.o("gear").b("gym", false)) }
    var week by remember(registration) {
        mutableStateOf((0..6).map { initial.a("week").getInt(it) })
    }
    var issue by remember(registration) { mutableStateOf<String?>(null) }

    val bg = Color(0xFF071321)
    val ink = Color(0xFFF1F7FF)
    val muted = Color(0xFFBED0E6)
    val accent = Color(0xFF52CCFF)
    val dayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    val groups = listOf(
        "Superior", "Central", "Inferior", "Movilidad", "Cardio", "Descanso"
    )

    Column(
        Modifier.fillMaxSize().background(bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("PERFIL NATIVO DE RECUPERACIÓN", color = accent, fontSize = 18.sp)
        Text("Hunter System 43.12.2 · compilación 132", color = muted, fontSize = 13.sp)
        Text(
            "El navegador integrado no pudo mostrar el perfil. Puedes continuar aquí " +
                "sin borrar tus datos. Los ajustes no modificados se conservan.",
            color = muted, fontSize = 14.sp
        )
        TextButton(onClick = onRetry) { Text("Volver a intentar el perfil visual") }
        if (registration.editing) {
            TextButton(onClick = { ui.closeProfile() }) { Text("Volver al panel") }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            label = { Text("Nombre del cazador") },
            textStyle = TextStyle(color = ink),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        OutlinedTextField(
            value = age,
            onValueChange = { value -> age = value.filter(Char::isDigit).take(3) },
            label = { Text("Edad en años") },
            textStyle = TextStyle(color = ink),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )

        NativeProfileChoice("Objetivo", goal,
            listOf("general" to "Bienestar", "strength" to "Fuerza", "endurance" to "Resistencia",
                "mobility" to "Movilidad", "habit" to "Hábito")) { goal = it }
        NativeProfileChoice("Nivel", difficulty,
            listOf("basic" to "Básico", "intermediate" to "Intermedio",
                "advanced" to "Avanzado")) { difficulty = it }
        NativeProfileChoice("Modalidad", mode,
            listOf("challenges" to "Misiones", "routines" to "Rutinas")) { mode = it }
        NativeProfileChoice("Duración", minutes,
            listOf("10" to "10 min", "15" to "15 min", "20" to "20 min", "30" to "30 min",
                "45" to "45 min", "60" to "60 min")) { minutes = it }
        NativeProfileChoice("Tema", theme,
            listOf("light" to "Claro", "dark" to "Oscuro")) { theme = it }

        Text("Equipo disponible", color = ink, fontSize = 17.sp)
        NativeProfileCheckbox("Peso corporal", bodyGear) { bodyGear = it }
        NativeProfileCheckbox("Mancuernas", dumbbells) { dumbbells = it }
        NativeProfileCheckbox("Gimnasio", gym) { gym = it }
        Text("Calendario de entrenamiento", color = ink, fontSize = 17.sp)
        dayNames.forEachIndexed { index, day ->
            var expanded by remember(index) { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(day, modifier = Modifier.weight(1f), color = muted)
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(groups[week[index]], color = ink)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        groups.forEachIndexed { choice, label ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                week = week.toMutableList().also { it[index] = choice }
                                expanded = false
                            })
                        }
                    }
                }
            }
        }

        if (issue != null) {
            Text(issue ?: "", color = Color(0xFFFFB4B4))
        }
        Button(
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            onClick = {
                runCatching {
                    val payload = registration.initialData()
                    val fields = payload.o("fields")
                    fields.put("name", name.trim())
                        .put("age", age)
                        .put("objective", goal)
                        .put("difficulty", difficulty)
                        .put("trainingMode", mode)
                        .put("minutes", minutes)
                        .put("theme", theme)
                    val gear = payload.o("gear")
                    gear.put("body", bodyGear).put("db", dumbbells).put("gym", gym)
                    payload.put("fields", fields)
                        .put("gear", gear)
                        .put("week", arr(week))
                        .put("confirmed", true)
                    registration.submit(payload)
                    ui.profileSaved()
                }.onFailure { issue = it.message ?: "No se pudo guardar el perfil." }
            }
        ) {
            Text(if (registration.editing) "Guardar perfil" else "Crear perfil y abrir Hunter System")
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun NativeProfileChoice(
    title: String, selected: String, options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    Text(title, color = Color.White, fontSize = 17.sp)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        for ((id, label) in options) {
            if (id == selected) Button(onClick = { onSelect(id) }) { Text(label) }
            else OutlinedButton(onClick = { onSelect(id) }) {
                Text(label, color = Color.White)
            }
        }
    }
}

@Composable
private fun NativeProfileCheckbox(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = value, onCheckedChange = onChange)
        Text(label, color = Color.White)
    }
}
