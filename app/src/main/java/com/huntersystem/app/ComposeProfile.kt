package com.huntersystem.app

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

private const val profileArt = "profile-monarca/"
private val backgrounds = listOf("identity", "training", "appearance", "activation")
private val titles = listOf("Identidad del cazador", "Entrenamiento", "Personalización",
    "Activación del sistema", "Sistema activado")
private val dayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
private val categories = listOf("Tren superior", "Tren central", "Tren inferior", "Movilidad", "Cardio", "Descanso")
private val categoryIcons = listOf("↑", "◆", "↓", "≈", "♥", "–")
private val categoryColors = listOf(0xFF258FE6, 0xFF9A68F2, 0xFF34BC91, 0xFF168887, 0xFFD8732D, 0xFF62738B).map(::Color)
private val gearChoices = listOf("body" to "Peso corporal / sin equipo") + materials +
    listOf("db" to "Mancuernas", "gym" to "Gimnasio")
private val companions = listOf("knight" to "${profileArt}igris.webp", "insect" to "${profileArt}beru.webp",
    "bear" to "${profileArt}tank.webp", "tusk" to "ui33/companion-tusk.webp",
    "warrior" to "${profileArt}iron.webp", "scientist" to "ui33/companion-dokja.webp")
private val figures = listOf("hunter-male", "hunter-female", "reader", "regressor", "reader-female")
private val scenes = listOf("templo-del-ocaso" to "Ruinas arcanas", "biblioteca-lunar" to "Niebla violeta",
    "trono-volcanico" to "Obsidiana", "reino-de-las-nubes" to "Aurora dorada", "jardin-sagrado" to "Bosque ancestral")
private val edges = listOf("ocean" to "Azul", "purple" to "Morado", "pink" to "Rosa",
    "green" to "Verde", "red" to "Rojo", "white" to "Blanco")
private val edgeColors = listOf(0xFF258FE6, 0xFF9A68F2, 0xFFED72B8, 0xFF34BC91, 0xFFE65260, 0xFFF2F5FF).map(::Color)
private val gender = listOf("male" to "Masculino", "female" to "Femenino", "unspecified" to "No especificado")
private val levels = listOf("basic" to "Básico", "intermediate" to "Intermedio", "advanced" to "Avanzado")
private val modes = listOf("challenges" to "Misiones diarias", "routines" to "Rutinas")
private val formats = listOf("rounds" to "Por rondas", "exercise" to "Por series")
private val themes = listOf("light" to "Blanco", "dark" to "Negro")
private val rests = listOf(30, 60, 90).map { "$it" to "$it segundos" }
private val times = listOf(10, 15, 20, 30, 45, 60).map { "$it" to "$it minutos" }
private val missions = (1..7).map { "$it" to if (it == 1) "1 misión" else "$it misiones" }

private class ProfilePalette(val dark: Boolean) {
    val top = if (dark) Color(0xFF102037) else Color(0xFFFFFBF2)
    val bottom = if (dark) Color(0xFF071323) else Color(0xFFE9F0FA)
    val card = if (dark) Color(0xFF13243B) else Color(0xFFF8FAFD)
    val ink = if (dark) Color(0xFFEEF3FD) else Color(0xFF17283E)
    val muted = if (dark) Color(0xFFB9CCE7) else Color(0xFF405471)
    val line = if (dark) Color(0xFF476184) else Color(0xFF9AAFC8)
    val highlight = if (dark) Color(0xFF79D9FF) else Color(0xFF17659A)
    val gold = if (dark) Color(0xFFF4D292) else Color(0xFF614219)
}
private val LocalProfilePalette = staticCompositionLocalOf { ProfilePalette(false) }
private val goldRim = Brush.linearGradient(listOf(Color(0xFF835829), Color(0xFFFFEBC0),
    Color(0xFFA7723D), Color(0xFFF7D798), Color(0xFF8E602F)))

@Composable
internal fun ComposeHunterProfile(ui: NativeUi, registration: ProfileRegistration,
    overlayOpen: Boolean, onSupplement: (String) -> Unit) {
    val initial = remember(registration) { registration.initialData() }
    var draftJson by rememberSaveable {
        mutableStateOf(obj("fields" to initial.o("fields"), "gear" to initial.o("gear"), "week" to initial.a("week")).toString())
    }
    val draft = remember(draftJson) { JSONObject(draftJson) }
    val fields = draft.o("fields")
    val gear = draft.o("gear")
    val week = draft.a("week")
    fun field(id: String, value: String) { draftJson = JSONObject(draftJson).apply { getJSONObject("fields").put(id, value) }.toString() }
    fun equipment(id: String, enabled: Boolean) {
        draftJson = JSONObject(draftJson).apply { getJSONObject("gear").put(id, enabled) }.toString()
    }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var confirmed by rememberSaveable { mutableStateOf(false) }
    var issue by rememberSaveable { mutableStateOf("") }
    fun navigate(target: Int) { step = target; confirmed = false; issue = "" }
    fun back() = when {
        step == 4 -> ui.profileSaved()
        step > 0 -> navigate(step - 1)
        registration.editing -> ui.closeProfile()
        else -> Unit
    }
    BackHandler(enabled = !overlayOpen) { back() }

    val palette = ProfilePalette(fields.s("theme") == "dark")
    CompositionLocalProvider(LocalProfilePalette provides palette) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF081527)).imePadding()) {
            ProfileArt(profileArt + backgrounds[step.coerceAtMost(3)] + ".webp", Modifier.matchParentSize(), ContentScale.FillBounds)
            val left = maxWidth * if (step == 0) .263f else .287f
            val right = maxWidth * .025f
            val top = (maxHeight * if (step == 0) .115f else .096f).coerceAtLeast(66.dp)
            val bottom = maxHeight * .075f
            val wordmarkSize = (maxWidth.value * .052f).coerceIn(16f, 31f).sp
            val titleSize = (maxWidth.value * (if (step == 0) .0405f else .0435f)).toInt().coerceIn(15, 25)
            Column(Modifier.fillMaxWidth().padding(start = left, end = right, top = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✦", color = Color(0xFFF6D491), fontSize = 20.sp)
                Text("HUNTER SYSTEM", color = Color(0xFFFFF4DA), fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold, fontSize = wordmarkSize, textAlign = TextAlign.Center)
                Text("MONARCA ECLIPSE", color = Color(0xFFF6D491), letterSpacing = 2.sp, fontSize = 9.sp)
            }
            val cut = CutCornerShape(19.dp)
            Column(Modifier.fillMaxSize().padding(start = left, end = right, top = top, bottom = bottom)
                .clip(cut).background(goldRim).padding(2.dp).clip(CutCornerShape(17.dp))
                .background(Brush.verticalGradient(listOf(palette.top, palette.bottom)))) {
                key(step) {
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileHeader(step, titleSize)
                        when (step) {
                            0 -> ProfileIdentity(fields, ui.pendingAvatar ?: ui.store.state.s("avatar"), ::field, ui.chooseAvatar)
                            1 -> ProfileTraining(fields, gear, List(7) { week.getInt(it) }, ::field, ::equipment,
                                { day -> draftJson = JSONObject(draftJson).apply {
                                    val days = getJSONArray("week")
                                    days.put(day, (days.getInt(day) + 1) % categories.size)
                                }.toString() }, { onSupplement("health") })
                            2 -> ProfileAppearance(fields, initial.o("legacyArtwork"), ::field,
                                if (registration.editing) ({ onSupplement("settings") }) else null)
                            3 -> ProfileReview(fields, gear, List(7) { week.getInt(it) }, confirmed) { confirmed = it }
                            4 -> Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                ProfileTitle(if (registration.editing) "Cambios guardados" else "Registro completado", 18)
                            }
                        }
                        if (issue.isNotBlank()) Text(issue, color = if (palette.dark) Color(0xFFFFB7C0) else Color(0xFF972143))
                    }
                }
                HorizontalDivider(color = palette.line)
                Row(Modifier.fillMaxWidth().padding(7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (step in 1..3 || step == 0 && registration.editing)
                        FrameAction("‹ ATRÁS", Modifier.weight(.8f), false, onClick = ::back)
                    val label = when {
                        step < 3 -> "SIGUIENTE ›"
                        step == 4 -> "ENTRAR AL SISTEMA"
                        registration.editing -> "GUARDAR CAMBIOS"
                        else -> "ACTIVAR PERFIL"
                    }
                    FrameAction(label, Modifier.weight(1.2f), true, step != 3 || confirmed) {
                        when (step) {
                            0 -> {
                                issue = profileIdentityIssue(fields).orEmpty()
                                if (issue.isBlank()) navigate(1)
                            }
                            1, 2 -> navigate(step + 1)
                            3 -> runCatching {
                                registration.submit(JSONObject(draftJson).put("confirmed", confirmed))
                            }.onSuccess { navigate(4) }
                                .onFailure { issue = it.message ?: "No se pudo guardar el perfil." }
                            4 -> ui.profileSaved()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileArt(path: String, modifier: Modifier, scale: ContentScale = ContentScale.Crop) {
    val context = LocalContext.current
    val image = remember(path, context) {
        runCatching { context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
    }
    if (image != null) Image(image, null, modifier, contentScale = scale)
}

@Composable
private fun ProfileTitle(text: String, size: Int = 15, modifier: Modifier = Modifier) {
    Text(text, modifier, color = LocalProfilePalette.current.gold, fontSize = size.sp,
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
}

@Composable
private fun ProfileHeader(step: Int, titleSize: Int) {
    val p = LocalProfilePalette.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (step < 4) "◆" else "◇", color = p.highlight, fontSize = if (step < 4) 11.sp else 29.sp,
            lineHeight = if (step < 4) 11.sp else 29.sp)
        if (step < 4) Box(Modifier.border(1.dp, goldRim, CutCornerShape(8.dp)).padding(horizontal = 18.dp, vertical = 3.dp)) {
            ProfileTitle("PASO ${step + 1} DE 4", 12)
        }
        ProfileTitle(titles[step].uppercase(), titleSize)
        HorizontalDivider(color = p.gold.copy(alpha = .55f))
        if (step < 4) Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(4) { index ->
                if (index > 0) HorizontalDivider(Modifier.weight(1f), color = p.line)
                val selected = index == step
                Box(Modifier.size(28.dp).clip(CircleShape)
                    .background(if (selected) p.gold.copy(alpha = .22f) else p.card)
                    .border(if (selected) 2.dp else 1.dp, if (selected) p.gold else p.line, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text("${index + 1}", color = if (selected) p.gold else p.muted, fontFamily = FontFamily.Serif)
                }
            }
        }
    }
}

@Composable
private fun FrameAction(label: String, modifier: Modifier, primary: Boolean, enabled: Boolean = true,
    onClick: () -> Unit) {
    val p = LocalProfilePalette.current
    val shape = CutCornerShape(11.dp)
    val finish = when {
        !enabled -> listOf(p.line, p.card)
        primary && p.dark -> listOf(Color(0xFF805123), Color(0xFF362519))
        primary -> listOf(Color(0xFFFFEFC5), Color(0xFFDFB16B))
        else -> listOf(p.card, p.bottom)
    }
    Box(modifier.heightIn(min = 48.dp).clip(shape).background(if (primary && enabled) goldRim else Brush.verticalGradient(listOf(p.line, p.muted)))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(2.dp)
        .clip(shape).background(Brush.verticalGradient(finish)).padding(5.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (primary && enabled && p.dark) Color(0xFFFFF2CA) else p.ink,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 12.sp)
    }
}

@Composable
private fun ProfileModule(title: String, icon: String, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalProfilePalette.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(p.card)
        .border(1.dp, p.line.copy(alpha = .65f), RoundedCornerShape(9.dp)).padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (title.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ProfileGlyph(icon)
            ProfileTitle(title.uppercase(), 13)
        }
        content()
    }
}

@Composable
private fun ProfileChoice(label: String, modifier: Modifier = Modifier, selected: Boolean = false,
    icon: String? = null, onClick: () -> Unit) {
    val p = LocalProfilePalette.current
    val shape = RoundedCornerShape(7.dp)
    Column(modifier.heightIn(min = 48.dp).clip(shape).background(if (selected) p.highlight.copy(alpha = .13f) else p.top)
        .border(if (selected) 2.dp else 1.dp, if (selected) p.highlight else p.line, shape)
        .selectable(selected, role = Role.Button, onClick = onClick).padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (icon != null) ProfileGlyph(icon)
        Text(label, color = p.ink, fontSize = 11.sp, lineHeight = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ProfileChoices(options: List<Pair<String, String>>, value: String, icons: List<String>,
    select: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { index, (id, label) ->
            ProfileChoice(label, Modifier.weight(1f), value == id, icons.getOrNull(index)) { select(id) }
        }
    }
}

@Composable
private fun ProfilePicker(title: String, options: List<Pair<String, String>>, value: String, icon: String,
    modifier: Modifier = Modifier, artwork: ((String) -> String)? = null, hideNames: Boolean = false,
    optionIcons: List<String> = emptyList(), swatches: List<Color> = emptyList(), select: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val p = LocalProfilePalette.current
    Box(modifier) {
        ProfileChoice("$title\n${if (hideNames) "Elegir retrato" else options.toMap()[value] ?: "Opción guardada"}  ⌄",
            Modifier.fillMaxWidth(), icon = icon) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false },
            modifier = Modifier.widthIn(max = 270.dp).background(p.card)) {
            options.forEachIndexed { index, (id, label) ->
                DropdownMenuItem(
                    text = {
                        if (!hideNames) Text(label, color = p.ink)
                        else if (id == value) Text("✓", color = p.highlight)
                    },
                    leadingIcon = {
                        when {
                            artwork != null -> ProfileArt(artwork(id), Modifier.size(64.dp, 56.dp))
                            swatches.getOrNull(index) != null -> Box(Modifier.size(20.dp).background(swatches[index], CircleShape))
                            else -> ProfileGlyph(optionIcons.getOrNull(index) ?: icon)
                        }
                    },
                    trailingIcon = if (id == value && !hideNames) ({ Text("✓", color = p.highlight) }) else null,
                    modifier = if (hideNames) Modifier.semantics { contentDescription = "Silueta ${index + 1}" } else Modifier,
                    onClick = { select(id); open = false }
                )
            }
        }
    }
}

@Composable
private fun ProfileInput(label: String, placeholder: String, value: String, keyboard: KeyboardType = KeyboardType.Text,
    multiline: Boolean = false, change: (String) -> Unit) {
    val p = LocalProfilePalette.current
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, fontSize = 12.sp) },
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        singleLine = !multiline, minLines = if (multiline) 2 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p.highlight, unfocusedBorderColor = p.line,
            focusedTextColor = p.ink, unfocusedTextColor = p.ink, focusedLabelColor = p.highlight,
            unfocusedLabelColor = p.muted, focusedPlaceholderColor = p.muted, unfocusedPlaceholderColor = p.muted),
        shape = RoundedCornerShape(7.dp))
}

@Composable
private fun ProfileIdentityField(label: String, placeholder: String, icon: String, value: String,
    keyboard: KeyboardType = KeyboardType.Text, required: Boolean = false, change: (String) -> Unit) {
    val p = LocalProfilePalette.current
    ProfileModule("", icon) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ProfileGlyph(icon, 30)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(label, color = p.ink, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (required) Text("*", color = if (p.dark) Color(0xFFFF98A8) else Color(0xFFAC3045), fontSize = 13.sp)
                }
                val shape = RoundedCornerShape(6.dp)
                BasicTextField(value, change, Modifier.fillMaxWidth().semantics { contentDescription = label },
                    singleLine = true, textStyle = TextStyle(color = p.ink, fontSize = 13.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboard), cursorBrush = SolidColor(p.highlight),
                    decorationBox = { editor ->
                        Box(Modifier.fillMaxWidth().heightIn(min = 36.dp).clip(shape).background(p.top)
                            .border(1.dp, p.line, shape).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) Text(placeholder, color = p.muted, fontSize = 12.sp)
                            editor()
                        }
                    })
            }
        }
    }
}

@Composable
private fun ProfileIdentity(fields: JSONObject, photo: String, set: (String, String) -> Unit, pickPhoto: () -> Unit) {
    ProfileIdentityField("Nombre del cazador", "Escribe tu nombre", "person", fields.s("name"), required = true) {
        set("name", it.take(40))
    }
    ProfileIdentityField("Edad en años", "Ingresa tu edad", "calendar", fields.s("age"), KeyboardType.Number, true) {
        set("age", it.filter(Char::isDigit))
    }
    ProfileIdentityField("Estatura (cm) · Opcional", "Ej. 175", "ruler", fields.s("height"), KeyboardType.Decimal) {
        set("height", it)
    }
    ProfileIdentityField("Peso (kg) · Opcional", "Ej. 70", "weight", fields.s("weight"), KeyboardType.Decimal) {
        set("weight", it)
    }
    ProfileModule("Género", "person") {
        ProfileChoices(gender, fields.s("gender"), listOf("shield", "heart", "person")) { set("gender", it) }
    }
    ProfileModule("", "camera") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            val avatar = Modifier.size(56.dp).clip(CircleShape).border(1.dp, goldRim, CircleShape)
            if (photo.isBlank()) ProfileArt(profileArt + "avatar.webp", avatar)
            else Avatar(photo, fields.s("name"), avatar)
            ProfileChoice("Elegir fotografía", Modifier.weight(1f), icon = "camera", onClick = pickPhoto)
        }
    }
    ProfileNote("El nombre y la edad son obligatorios. La edad no limita el acceso a los ejercicios.")
}

@Composable
private fun ProfileNote(text: String) {
    Text(text, color = LocalProfilePalette.current.muted, fontSize = 11.sp, lineHeight = 15.sp)
}

@Composable
private fun ProfileTraining(fields: JSONObject, gear: JSONObject, week: List<Int>, set: (String, String) -> Unit,
    equipment: (String, Boolean) -> Unit, cycleDay: (Int) -> Unit, health: () -> Unit) {
    ProfileModule("Nivel", "bars") {
        ProfileChoices(levels, fields.s("difficulty"), listOf("shield", "bars", "crown")) { set("difficulty", it) }
    }
    ProfileModule("Objetivo principal", "heart") {
        ProfilePicker("Objetivo principal", objectives.toList(), fields.s("objective"), "heart",
            optionIcons = listOf("heart", "dumbbell", "pulse", "move", "calendar")) { set("objective", it) }
    }
    ProfileModule("Objetivo personalizado", "pen") {
        ProfileInput("Objetivo personalizado", "Describe tu objetivo con tus propias palabras",
            fields.s("finalGoal"), multiline = true) { set("finalGoal", it) }
    }
    ProfileModule("Modalidad del sistema", "flag") {
        ProfileChoices(modes, fields.s("trainingMode"), listOf("flag", "calendar")) { set("trainingMode", it) }
        if (fields.s("trainingMode") == "routines") {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ProfilePicker("Descanso", rests, fields.s("restSeconds"), "clock", Modifier.weight(1f)) { set("restSeconds", it) }
                ProfilePicker("Organización", formats, fields.s("blockMode22"), "cycle", Modifier.weight(1f)) { set("blockMode22", it) }
            }
            ProfilePicker("Tiempo disponible", times, fields.s("minutes"), "clock") { set("minutes", it) }
        } else {
            ProfilePicker("Cantidad de misiones", missions, fields.s("dailyMissions"), "flag") { set("dailyMissions", it) }
            ProfileNote("Las misiones pueden incluir actividades ligeras, aprendizaje y descansos.")
        }
    }
    ProfileModule("Planificación semanal", "calendar") {
        val p = LocalProfilePalette.current
        val dayIcons = listOf("dumbbell", "shield", "move", "cycle", "pulse", "moon")
        ProfileNote("Toca cada día para cambiar su categoría.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            week.forEachIndexed { index, group ->
                val color = categoryColors[group]
                Column(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(5.dp))
                    .background(color.copy(alpha = .22f)).border(1.dp, color, RoundedCornerShape(5.dp))
                    .clickable(role = Role.Button) { cycleDay(index) }
                    .semantics { contentDescription = "${dayNames[index]}: ${categories[group]}" },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("LMXJVSD"[index].toString(), color = p.ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    ProfileGlyph(dayIcons[group], 15)
                }
            }
        }
        categories.chunked(2).forEachIndexed { row, pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                pair.forEachIndexed { column, title ->
                    val index = row * 2 + column
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(15.dp).background(categoryColors[index], RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center) { Text(categoryIcons[index], color = Color.White, fontSize = 10.sp) }
                        ProfileNote(title)
                    }
                }
            }
        }
    }
    ProfileModule("Equipamiento disponible", "dumbbell") {
        gearChoices.forEachIndexed { index, (id, label) ->
            ProfileCheck(label, gear.b(id), listOf("move", "mat", "wall", "chair", "dumbbell", "gym")[index]) { equipment(id, it) }
        }
        if (gear.b("db")) ProfileInput("Pesos disponibles (kg), separados por comas", "2, 4, 6",
            fields.s("dumbbellWeights")) { set("dumbbellWeights", it) }
        if (gear.b("gym")) gymOptions.forEachIndexed { index, (id, label) ->
            ProfileCheck(label, gear.b("gym_$id"), listOf("cycle", "gym", "chair", "dumbbell", "bike", "move", "pulse")[index]) {
                equipment("gym_$id", it)
            }
        }
    }
    ProfileChoice("Indicaciones y restricciones", Modifier.fillMaxWidth(), icon = "shield", onClick = health)
}

@Composable
private fun ProfileCheck(label: String, checked: Boolean, icon: String?, change: (Boolean) -> Unit) {
    val p = LocalProfilePalette.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .toggleable(checked, role = Role.Checkbox, onValueChange = change),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Checkbox(checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = p.highlight, uncheckedColor = p.line))
        if (icon != null) ProfileGlyph(icon)
        Text(label, color = p.ink, fontSize = 12.sp)
    }
}

@Composable
private fun ProfileAppearance(fields: JSONObject, legacy: JSONObject, set: (String, String) -> Unit, settings: (() -> Unit)?) {
    val p = LocalProfilePalette.current
    val figureArt: (String) -> String = { id ->
        val index = figures.indexOf(id)
        if (index >= 0) profileArt + "figures-${index + 1}.webp" else legacy.s("figure").removePrefix("/assets/")
    }
    val sceneArt: (String) -> String = { id ->
        val index = scenes.indexOfFirst { it.first == id }
        if (index >= 0) profileArt + "scenes-${index + 1}.webp" else legacy.s("scene").removePrefix("/assets/")
    }
    ProfileModule("Acompañante del sistema", "person") {
        ProfileNote("Desliza para ver todos los acompañantes.")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            companions.forEachIndexed { index, (id, image) ->
                val selected = fields.s("companionCharacter") == id
                val shape = RoundedCornerShape(7.dp)
                ProfileArt(image, Modifier.size(60.dp).clip(shape).background(p.bottom)
                    .border(if (selected) 2.dp else 1.dp, if (selected) p.highlight else p.line, shape)
                    .selectable(selected, role = Role.Button) { set("companionCharacter", id) }
                    .semantics { contentDescription = "Acompañante ${index + 1}" })
            }
        }
    }
    ProfileModule("Apariencia", "frame") {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            ProfilePicker("Tema del sistema", themes, fields.s("theme"), "moon", Modifier.weight(1f)) { set("theme", it) }
            ProfilePicker("Color del borde", edges, fields.s("outlineColor"), "frame", Modifier.weight(1f), swatches = edgeColors) { set("outlineColor", it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            ProfilePicker("Silueta", figures.map { it to "" }, fields.s("collection"), "person",
                Modifier.weight(1f), artwork = figureArt, hideNames = true) { set("collection", it) }
            ProfilePicker("Fondo de tarjeta", scenes, fields.s("scene"), "landscape",
                Modifier.weight(1f), artwork = sceneArt) { set("scene", it) }
        }
    }
    ProfileModule("Vista previa del cazador", "frame") {
        val color = edgeColors.getOrNull(edges.indexOfFirst { it.first == fields.s("outlineColor") }) ?: p.highlight
        val frame = CutCornerShape(11.dp)
        Box(Modifier.fillMaxWidth().height(122.dp).clip(frame).border(2.dp, color, frame)) {
            ProfileArt(sceneArt(fields.s("scene")), Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(p.top, p.top.copy(alpha = .73f), Color.Transparent))))
            ProfileArt(figureArt(fields.s("collection")), Modifier.align(Alignment.BottomEnd).width(85.dp).fillMaxHeight(), ContentScale.Fit)
            Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(.58f).padding(start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                ProfileTitle("◆ SISTEMA MONARCA", 11)
                Text(fields.s("name").ifBlank { "Cazador" }, color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                ProfileNote("${themes.toMap()[fields.s("theme")]} · ${edges.toMap()[fields.s("outlineColor")] ?: "Color guardado"}")
                ProfileNote(scenes.toMap()[fields.s("scene")] ?: "Fondo guardado")
            }
            companions.toMap()[fields.s("companionCharacter")]?.let {
                ProfileArt(it, Modifier.align(Alignment.BottomEnd).padding(4.dp).size(35.dp).clip(CircleShape))
            }
        }
    }
    if (settings != null) ProfileChoice("Ajustes de la aplicación", Modifier.fillMaxWidth(), icon = "frame", onClick = settings)
}

@Composable
private fun ProfileReview(fields: JSONObject, gear: JSONObject, week: List<Int>, confirmed: Boolean, confirm: (Boolean) -> Unit) {
    fun label(id: String, values: List<Pair<String, String>>) = values.toMap()[fields.s(id)] ?: "Opción guardada"
    fun detail(id: String, unit: String) = fields.s(id).takeIf(String::isNotBlank)?.let { "$it $unit" } ?: "Sin indicar"
    val routine = fields.s("trainingMode") == "routines"
    val equipment = buildList {
        gearChoices.filter { gear.b(it.first) }.forEach { add(it.second) }
        if (gear.b("db") && fields.s("dumbbellWeights").isNotBlank()) add(fields.s("dumbbellWeights") + " kg")
        if (gear.b("gym")) gymOptions.filter { gear.b("gym_" + it.first) }.forEach { add(it.second) }
    }
    ProfileTitle("VERIFICACIÓN DEL REGISTRO", 14, Modifier.fillMaxWidth())
    ProfileSummary("Identidad del cazador", "person", listOf(
        fields.s("name"), detail("age", "años"), "Estatura · ${detail("height", "cm")}",
        "Peso · ${detail("weight", "kg")}", label("gender", gender)))
    ProfileSummary("Entrenamiento", "dumbbell", buildList {
        add("Nivel · ${label("difficulty", levels)}")
        add(label("objective", objectives.toList()))
        add(fields.s("finalGoal").ifBlank { "Objetivo personalizado sin indicar" })
        add(label("trainingMode", modes))
        if (routine) {
            add(label("blockMode22", formats))
            add(label("minutes", times))
        } else add(label("dailyMissions", missions))
        add("Descanso configurado · ${label("restSeconds", rests)}")
        week.forEachIndexed { index, group -> add("${dayNames[index]} · ${categories[group]}") }
        add(equipment.joinToString().ifBlank { "Sin equipo declarado" })
    })
    ProfileSummary("Personalización", "frame", listOf(label("theme", themes), label("outlineColor", edges),
        "Acompañante elegido", "Silueta elegida", label("scene", scenes)))
    ProfileModule("", "check") {
        ProfileCheck("He revisado mi información y deseo activar mi perfil de cazador.", confirmed, null, confirm)
    }
}

@Composable
private fun ProfileSummary(title: String, icon: String, lines: List<String>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ProfileModule("", icon) {
        ProfileChoice("$title ${if (expanded) "⌃" else "⌄"}", Modifier.fillMaxWidth(), icon = icon) { expanded = !expanded }
        if (expanded) lines.forEach { ProfileNote(it) }
    }
}
