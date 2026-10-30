package com.huntersystem.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Usa las ilustraciones del cazador que YA existen en la APK.
 * E utiliza las versiones más detalladas ui34; el resto usa ui33.
 * Nunca depende del compañero escogido en la creación del perfil.
 */
internal fun monarchHunterHeroArtwork(gender:String,rank:String):String {
    val body=if(gender=="female")"female" else "male"
    val safeRank=rank.lowercase().takeIf { it in setOf("e","d","c","b","a","s") } ?: "e"
    val folder=if(safeRank=="e")"ui34" else "ui33"
    return "$folder/hunter-$body-$safeRank.webp"
}

/**
 * El personaje guardado en Perfil gobierna también el arte del Panel.
 * Para los rangos siguientes se reutilizan ilustraciones existentes.
 */
internal fun profileMonarchHunterArtwork(collection:String,gender:String,rank:String):String{
 val safe=rank.lowercase().takeIf{it in setOf("e","d","c","b","a","s")} ?: "e"
 // La portada Monarca del nivel inicial coincide con el selector HD.
 // En Reader se conservan las variantes HD por rango al evolucionar.
 return if(collection=="reader" && safe!="e")"ui34/reader-"+safe+".webp"
  else chosenHunterArtwork(collection,gender)
}
/**
 * Fase 04: dashboard Monarca. Solo lee NativeStore y enlaza acciones existentes.
 * No altera el perfil, las sesiones, las estadísticas, las restricciones ni el catálogo.
 */
@Composable
fun MonarchDashboardHero(ui: NativeUi, stats: JSONObject) {
    val c = LocalHunter.current
    val store = ui.store
    val profile = store.profile ?: JSONObject()
    val level = stats.i("level", 1).coerceAtLeast(1)
    val xp = stats.i("xp").coerceAtLeast(0)
    val xpInLevel = xp % 300
    val rank = rankKey(level)
    val appearance = store.state.o("appearance33")
    val hunterArtwork = profileMonarchHunterArtwork(
        appearance.s("collection","hunter"),profile.s("gender"),rank)
    val selectedScene = appearance.s("scene","auto")
    val name = profile.s("name", "Cazador").ifBlank { "Cazador" }.take(36)
    val shape = RoundedCornerShape(26.dp)

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val expansive = maxWidth >= 680.dp
        val compact = maxWidth < 355.dp
        Box(Modifier.fillMaxWidth().height(
                if(expansive)520.dp else if(compact)420.dp else 468.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c.surface,c.bg)))
            .monarchOutline(shape)) {
            AssetImage(chosenSceneArtwork(selectedScene),Modifier.matchParentSize(),
                ContentScale.Crop,description=null)
            // Protagonista vertical HD en toda la altura; no una miniatura lateral.
            HunterEvolutionImage(appearance.s("collection","hunter"),profile.s("gender"),rank,hunterArtwork,
                Modifier.align(Alignment.BottomEnd)
                    .fillMaxWidth(if(expansive).68f else .88f)
                    .fillMaxHeight(.96f),
                ContentScale.Fit,description="Cazador Monarca · rango $rank",
                alignment=Alignment.BottomEnd)
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(
                c.bg.copy(alpha=.99f),c.bg.copy(alpha=.92f),
                c.bg.copy(alpha=.28f),Color.Transparent))))
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
                Color.Transparent,Color.Transparent,c.bg.copy(alpha=.89f)))))
            Column(Modifier.fillMaxSize().padding(if(compact)15.dp else 20.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.SpaceBetween,
                    verticalAlignment=Alignment.CenterVertically) {
                    Eyebrow("◆  SISTEMA MONARCA")
                    HText("RANGO $rank",size=14,color=c.accent,bold=true)
                }
                Column(Modifier.weight(1f)
                    .fillMaxWidth(if(expansive).47f else .53f),
                    verticalArrangement=Arrangement.spacedBy(if(compact)9.dp else 13.dp)){
                    HText("TU EVOLUCIÓN",size=13,color=c.accent,bold=true)
                    Heading("NIVEL\n"+level.toString().padStart(2,'0'),
                        size=if(expansive)54 else if(compact)35 else 43)
                    HText(name,size=if(compact)16 else 19,bold=true,maxLines=2)
                    HText("Cada sesión cuenta.",size=14,color=c.muted,maxLines=2)
                }
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.SpaceBetween){
                    HText("EXPERIENCIA",color=c.text,size=13,bold=true)
                    HText("$xp XP",color=c.accent,size=15,bold=true)
                }
                HProgress(xpInLevel/300f)
                HText("$xpInLevel / 300 XP hacia el siguiente nivel",
                    size=13,color=c.muted)
            }
            Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter)
                .background(Brush.horizontalGradient(listOf(
                    Color.Transparent,c.accent,c.secondary,Color.Transparent))))
        }
    }
}

@Composable
fun MonarchDashboardWelcome(ui: NativeUi) {
    val c = LocalHunter.current
    val first = ui.store.profile?.s("name", "Cazador")
        ?.trim()?.substringBefore(" ")?.take(24).orEmpty().ifBlank { "Cazador" }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Eyebrow("PANEL DEL CAZADOR  ·  SIN CONEXIÓN")
        Heading("Bienvenido, $first", size = 28)
        HText("Tu avance real, tus misiones y tu próxima meta.",
            color = c.muted, size = 15)
    }
}

@Composable
fun MonarchDashboardMission(ui: NativeUi) {
    val c = LocalHunter.current
    val s = ui.store
    val currentDate = today()
    val day = s.day(currentDate)
    val log = s.logs.optJSONObject(currentDate)
    val progress = s.fraction(log).toFloat().coerceIn(0f, 1f)
    val finished = log?.b("complete") == true
    val paused = log?.b("pain") == true
    val rest = day.s("kind") == "rest"
    val count = if (log == null) {
        day.a("items").length() + day.a("tasks").length()
    } else {
        log.a("items").length() + log.a("missionTasks").length()
    }
    val status = when {
        finished -> "COMPLETADA"
        paused -> "REVISIÓN PENDIENTE"
        rest -> "DESCANSO"
        count == 0 -> "SIN ACTIVIDADES"
        else -> "ACTIVA"
    }
    HPanel(padding = 18) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("MISIÓN PRINCIPAL  ·  HOY")
            HText(status, size = 12, color = if (paused) MonarchV42Palette.Lower else c.accent,
                bold = true, maxLines = 2)
        }
        Heading(day.s("title", "Tu sesión de hoy"), size = 28)
        HText(when {
            finished -> "Has completado esta sesión. Tu progreso está registrado."
            paused -> "La sesión requiere revisión antes de continuar."
            rest -> "Hoy tienes descanso en el calendario. Puedes consultar tu plan."
            count == 0 -> "No hay actividades asignadas. Revisa tu planificación."
            else -> "Tu progreso se conserva al cerrar la aplicación."
        }, color = c.muted, size = 15)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HText("PROGRESO DE LA SESIÓN", size = 12, color = c.muted, bold = true)
            HText("${(progress * 100f).roundToInt()} %", size = 16, bold = true,
                color = c.accent)
        }
        HProgress(progress)
        HButton(when {
            finished -> "CONSULTAR MISIÓN COMPLETADA  →"
            paused -> "REVISAR MI SESIÓN  →"
            rest -> "VER DESCANSO DEL DÍA  →"
            else -> "ABRIR MISIÓN DE HOY  →"
        }, Modifier.fillMaxWidth(), primary = true) {
            ui.open("mission", obj("date" to currentDate))
        }
        if (!finished && !paused && !s.activeLog(log)) {
            HButton("✎ PERSONALIZAR MISIÓN", Modifier.fillMaxWidth()) {
                ui.open("custom", obj("date" to currentDate))
            }
        }
    }
}

private data class MonarchMetric(val title: String, val value: String, val glyph: String)

@Composable
private fun MonarchMetricCard(ui: NativeUi, metric: MonarchMetric, modifier: Modifier) {
    val c = LocalHunter.current
    HPanel(modifier, onClick = { ui.go("stats") }, padding = 13, decor = false) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MonarchGlyph(metric.glyph, Modifier.size(23.dp), color = c.accent)
            HText(metric.title, size = 13, color = c.muted, maxLines = 2)
        }
        Heading(metric.value, size = 30)
    }
}

@Composable
fun MonarchDashboardMetrics(ui: NativeUi, stats: JSONObject) {
    val metrics = listOf(
        MonarchMetric("Racha actual", "${stats.i("streak")}", "plan"),
        MonarchMetric("Días completados", "${stats.i("days")}", "home"),
        MonarchMetric("Ejercicios realizados", "${stats.i("exerciseCount")}", "routines"),
        MonarchMetric("Series registradas", "${stats.i("series")}", "book")
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Heading("Tus estadísticas", size = 25)
            HText("VER TODO  ↗", Modifier.clickable(role = Role.Button) {
                ui.go("stats")
            }, size = 13, color = LocalHunter.current.accent, bold = true)
        }
        BoxWithConstraints {
            val wide = maxWidth >= 700.dp
            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    metrics.forEach { MonarchMetricCard(ui, it, Modifier.weight(1f)) }
                }
            } else {
                metrics.chunked(2).forEachIndexed { index, row ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { MonarchMetricCard(ui, it, Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
fun MonarchDashboardWeek(ui: NativeUi) {
    val s = ui.store
    val c = LocalHunter.current
    val date = LocalDate.parse(today())
    val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
    val week = (0L..6L).map { offset ->
        val dayDate = monday.plusDays(offset).toString()
        val entry = s.day(dayDate)
        Triple(dayDate, entry.s("kind"), s.logs.o(dayDate).b("complete"))
    }
    val planned = week.count { (dateString,kind,_) -> dateString <= today() && kind != "rest" }
    val completed = week.count { (dayString,kind,done) ->
        dayString <= today() && kind != "rest" && done
    }
    HPanel(padding = 16) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Eyebrow("CICLO SEMANAL")
                Heading("Tu camino", size = 26)
            }
            HText("$completed / $planned días", size = 15, color = c.accent, bold = true)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            val dayNames = listOf("LU", "MA", "MI", "JU", "VI", "SA", "DO")
            week.forEachIndexed { index, (dateString, kind, complete) ->
                val selected = dateString == today()
                val kindColor = when(kind) {
                    "strength" -> c.accent
                    "cardio" -> MonarchV42Palette.Cardio
                    "recovery" -> MonarchV42Palette.Mobility
                    else -> c.muted
                }
                val shape = RoundedCornerShape(12.dp)
                Column(Modifier.weight(1f)
                    .defaultMinSize(minHeight = 78.dp)
                    .clip(shape)
                    .background(if (selected) c.selected else c.input)
                    .monarchOutline(shape,active=selected)
                    .clickable(role = Role.Button) {
                        ui.open("mission", obj("date" to dateString))
                    }
                    .semantics {
                        contentDescription = "$dateString, ${when(kind) {
                            "strength" -> "fuerza"
                            "cardio" -> "cardio"
                            "recovery" -> "movilidad"
                            else -> "descanso"
                        }}, ${if(complete) "completado" else "pendiente"}"
                    }
                    .padding(vertical = 9.dp, horizontal = 1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HText(dayNames[index], size = 12,
                        color = if(selected) c.accent else c.muted, bold = true)
                    HText(if(complete) "✓" else dateString.takeLast(2),
                        size = 15, color = c.text, bold = selected || complete)
                    Box(Modifier.size(6.dp)
                        .clip(RoundedCornerShape(3.dp)).background(kindColor))
                }
            }
        }
        HText("Color del tema: fuerza  ·  Ámbar: cardio  ·  Verde: movilidad",
            size = 12, color = c.muted)
        HButton("CALENDARIO Y REVISIÓN SEMANAL  →", Modifier.fillMaxWidth()) {
            ui.go("plan")
        }
        HButton("REVISAR PROGRESO DE LA SEMANA", Modifier.fillMaxWidth()) {
            ui.open("review")
        }
    }
}

private data class MonarchShortcut(val title:String, val description:String, val glyph:String,
                                    val action: (NativeUi) -> Unit)

@Composable
private fun MonarchShortcutCard(ui:NativeUi, shortcut:MonarchShortcut, modifier:Modifier) {
    val c = LocalHunter.current
    HPanel(modifier, onClick = { shortcut.action(ui) }, padding = 15, decor = false) {
        MonarchGlyph(shortcut.glyph, Modifier.size(28.dp), color = c.accent)
        HText(shortcut.title, size = 17, bold = true, maxLines = 2)
        HText(shortcut.description, size = 13, color = c.muted, maxLines = 2)
    }
}

@Composable
fun MonarchDashboardShortcuts(ui:NativeUi) {
    val shortcuts = listOf(
        MonarchShortcut("Mis rutinas", "Entrena a tu ritmo", "routines") {
            it.go("routines"); it.library = "offline"
        },
        MonarchShortcut("Tus objetivos", "Organiza tus metas", "plan") {
            it.go("objectives")
        },
        MonarchShortcut("Academia", "Aprende y mejora", "book") {
            it.open("academy")
        },
        MonarchShortcut("Recordatorios", "Configura tus avisos", "home") {
            it.open("reminders")
        }
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Heading("Accesos rápidos", size = 25)
        BoxWithConstraints {
            val wide = maxWidth >= 700.dp
            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    shortcuts.forEach { MonarchShortcutCard(ui, it, Modifier.weight(1f)) }
                }
            } else {
                shortcuts.chunked(2).forEachIndexed { index, row ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { MonarchShortcutCard(ui, it, Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
fun MonarchDashboardCompanion(ui: NativeUi) {
    val c = LocalHunter.current
    HPanel(onClick = { ui.open("coach") }, padding = 17) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AssetImage("ui33/companion-${companionKey(ui.store.profile)}.webp", Modifier.width(56.dp).height(78.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow("ACOMPAÑANTE ACTIVO")
                Heading(companionName(ui.store.profile), size = 23)
                HText(CompanionDialogue.message(ui.store.profile,LocalVisualTier.current,
                     DialogueContext(DialogueMoment.WELCOME,
                       group=GroupEvolution.sessionGroup(ui.store.day(today()),ui.store.profile,ui.store.catalog)?:"")),
                    size = 14, color = c.muted)
            }
            HText("↗", size = 23, color = c.accent)
        }
    }
}

@Composable
fun MonarchDashboardGoals(ui:NativeUi) {
    val s = ui.store
    val c = LocalHunter.current
    val goals = s.state.a("goals").objects()
    HPanel(padding = 17) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Heading("Mis metas", size = 25)
            HText("VER TODAS  ↗", Modifier.clickable(role = Role.Button) {
                ui.go("objectives")
            }, size = 13, color = c.accent, bold = true)
        }
        if (goals.isEmpty()) {
            HText("Todavía no tienes objetivos personales. Puedes crear el primero.",
                size = 15, color = c.muted)
        }
        goals.take(3).forEach { goal ->
            val title=goal.s("title",goal.s("text"))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(10.dp)){
                HunterPngIcon(hunterGoalPngKey(title),Modifier.size(53.dp),title)
                Box(Modifier.weight(1f)){
                    HCheck(title.take(100),today() in goal.a("dates").strings()){
                        ui.attempt { s.toggleGoal(goal.s("id")) }
                    }
                }
            }
        }
        HButton("+ AÑADIR META", Modifier.fillMaxWidth()) {
            ui.open("goal-edit")
        }
    }
}

@Composable
fun MonarchDashboardGoalPlan(ui:NativeUi) {
    val s = ui.store
    val p = s.profile ?: JSONObject()
    val c = LocalHunter.current
    val goal = s.catalog.editorial.o("goalGroups").o(p.s("goalGroup", "habit"))
    val plan = s.catalog.editorial.o("goalPlans").o(p.s("finalTarget", "general"))
    val trainingDays = p.a("weekPlan").strings().count { it != "rest" }
    HPanel(padding = 17) {
        Eyebrow("PLAN DE ENTRENAMIENTO")
        Heading(goal.s("name", "Constancia y bienestar"), size = 25)
        HText(plan.s("why", "Tu plan se adapta a tus objetivos."),
            size = 15, color = c.muted)
        HText("${listOf("Básico","Intermedio","Avanzado")[level(p)]}  ·  " +
            "$trainingDays días configurados  ·  " +
            "${(p.n("trainingScale",1.0)*100).roundToInt()} % de dosis",
            size = 14, color = c.accent, bold = true)
        HText(s.rules.gate(p).reason, size = 14, color = c.muted)
        HButton("MODIFICAR PLAN EN PERFIL  →", Modifier.fillMaxWidth()) {
            ui.go("profile")
        }
    }
}
