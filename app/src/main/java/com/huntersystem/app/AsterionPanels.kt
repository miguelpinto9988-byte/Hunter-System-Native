package com.huntersystem.app

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.huntersystem.app.asterion.*
import java.time.LocalDate

/**
 * Panel integrado sin alterar el motor histórico ni los activos gráficos existentes.
 * Todas las acciones de resultado y esfuerzo requieren una pulsación explícita.
 */
@Composable
fun AsterionMissionSection(ui: NativeUi, date: String) {
    val store = ui.store
    val revision = store.revision.intValue
    val day = remember(date) { runCatching { LocalDate.parse(date) }.getOrNull() }
        ?: return
    val engine = remember(store) { AsterionTrainingSessionCoordinator(store) }
    var result by remember(date) { mutableStateOf<CoordinatedResult?>(null) }
    var rpe by remember(date) { mutableIntStateOf(5) }
    LaunchedEffect(date, revision) {
        result = runCatching { engine.current(day) }.getOrNull()
    }
    val current = result ?: return
    val mission = current.mission
    val progress = engine.progress
    val participations = remember(revision, date) { progress.participation() }
    val completed = participations.any { it.date == day &&
        it.status == ParticipationStatus.COMPLETED && it.variantId in mission.ids }
    HPanel(padding = 16) {
        Eyebrow("ASTERION · MISIÓN FUNCIONAL")
        Heading(if (mission.category == null) "Descanso programado"
            else mission.category.code.replace('_', ' '), size = 23)
        HText(mission.explanation, size = 14, color = LocalHunter.current.muted)
        current.checkpoint?.let { checkpoint ->
            HText("Hito técnico: día " + checkpoint.day +
                " · decisión " + checkpoint.decision.name.lowercase().replace('_', ' ') +
                " · fase " + checkpoint.phase.phase,
                size = 12, color = LocalHunter.current.muted)
            if (checkpoint.decision == TechnicalDecision.ADVANCE_PHASE &&
                mission.category != null && date == today()) {
                HButton("ACEPTAR FASE TÉCNICA OPCIONAL", Modifier.fillMaxWidth()) {
                    ui.attempt { progress.acceptPhase(mission.category, checkpoint) }
                }
            }
        }
        val note = if (current.goal.recognized)
            "Objetivo reconocido: " + current.goal.tags.joinToString(", ")
            else "Objetivo personal conservado; no hay afinidad reconocida."
        HText(note, size = 13, color = LocalHunter.current.muted)
        if (mission.ids.isEmpty()) {
            HNotice(if (mission.category == null) "No se asignan actividades en descanso."
                else "No se inventan actividades para cubrir opciones incompatibles.")
        }
        mission.ids.forEach { id ->
            val status = participations.firstOrNull { it.date == day && it.variantId == id }
            val label = store.catalog.byId[id]?.name ?: id
            HText(label, size = 16, bold = true)
            HText("Estado: " + (status?.status?.name ?: "PENDING") +
                ". Referencias numéricas sujetas a validación y consentimiento.",
                size = 12, color = LocalHunter.current.muted)
            current.volume[id]?.let { recommendation ->
                HText(if (recommendation.reference == null)
                    recommendation.explanation
                else "Referencia orientativa: " + recommendation.sets + " series · " +
                    recommendation.reference + " " + recommendation.unit?.name +
                    ". " + recommendation.explanation,
                    size = 12, color = LocalHunter.current.muted)
            }
            if (date == today()) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    HButton("COMPLETADA", Modifier.weight(1f)) {
                        ui.attempt {
                            progress.setParticipation(day, id, ParticipationStatus.COMPLETED)
                        }
                    }
                    HButton("OMITIDA", Modifier.weight(1f)) {
                        ui.attempt {
                            progress.setParticipation(day, id, ParticipationStatus.SKIPPED)
                        }
                    }
                }
                HButton("REGISTRAR INTERRUPCIÓN", Modifier.fillMaxWidth()) {
                    ui.attempt {
                        progress.setParticipation(day, id, ParticipationStatus.SKIPPED,
                            "Interrupción declarada", interrupted = true)
                    }
                }
            }
        }
        if (date == today() && mission.category != null && mission.ids.isNotEmpty()) {
            Eyebrow("ESFUERZO PERCIBIDO · REGISTRO VOLUNTARIO")
            HText("Valor seleccionado: " + rpe + "/10. No se guarda hasta confirmar.",
                size = 13)
            (1..10).toList().chunked(5).forEach { values ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    values.forEach { value ->
                        HChip(value.toString(), rpe == value, Modifier.weight(1f)) { rpe = value }
                    }
                }
            }
            HButton("GUARDAR ESFUERZO DEL DÍA", Modifier.fillMaxWidth()) {
                ui.attempt {
                    progress.addEffort(day, mission.category, rpe, completed)
                }
            }
            HText("Solo se usará en una próxima sesión de esta misma categoría. Puedes mantener el nivel o descansar.",
                size = 12, color = LocalHunter.current.muted)
        }
    }
}

@Composable
fun AsterionProgressSection(ui: NativeUi) {
    val s = ui.store
    val revision = s.revision.intValue
    val records = remember(revision) { AsterionProgressRepository(s).participation() }
    val efforts = remember(revision) { AsterionProgressRepository(s).efforts() }
    val date = LocalDate.now()
    val pending = remember(revision) {
        val pinned = AsterionProgressRepository(s).pinned(date)
        pinned?.ids?.filterNot { id -> records.any { it.date == date && it.variantId == id } }
            ?.mapNotNull { id -> pinned.category?.let { Participation(date, id, it, ParticipationStatus.PENDING) } }
            ?: emptyList()
    }
    val summary = ProgressSummary.calculate(records + pending)
    HPanel(padding = 16) {
        Eyebrow("ASTERION · PARTICIPACIÓN POR ACTIVIDAD")
        Heading(summary.label, size = 28)
        HText("Planificadas: " + summary.planned + " · Completadas: " +
            summary.completed + " · Omitidas: " + summary.skipped +
            " · Pendientes: " + summary.pending, size = 14)
        HText("Porcentaje calculado solo con completadas y omitidas; las pendientes no cuentan.",
            size = 12, color = LocalHunter.current.muted)
        Category.values().forEach { category ->
            val count = records.count { it.category == category &&
                it.status == ParticipationStatus.COMPLETED }
            val ratings = efforts.filter { it.category == category }
            val average = ratings.map { it.rpe }.takeIf { it.isNotEmpty() }?.average()
            HText(category.code.replace('_', ' ') + ": " + count +
                " completadas · esfuerzo: " +
                (average?.let { "%.1f".format(it) + "/10" } ?: "sin datos"),
                size = 13, color = LocalHunter.current.muted)
        }
        HText("Los registros globales antiguos permanecen separados. Las referencias de volumen no se calculan sin parámetros validados.",
            size = 12, color = LocalHunter.current.muted)
    }
}

@Composable
fun AsterionRoutineSection(ui: NativeUi) {
    val store = ui.store
    val revision = store.revision.intValue
    var category by remember { mutableStateOf(Category.UPPER) }
    var duration by remember { mutableIntStateOf(20) }
    var level by remember { mutableStateOf(RoutineLevel.BASIC) }
    var proposals by remember { mutableStateOf(emptyList<RoutineProposal>()) }
    var info by remember { mutableStateOf("") }
    val progress = remember(store) { AsterionProgressRepository(store) }
    HPanel(padding = 16) {
        Eyebrow("ASTERION · RUTINAS OPCIONALES")
        Heading("Generación por reglas", size = 22)
        HText("Hasta seis alternativas reproducibles, solo si existen opciones compatibles.",
            size = 13, color = LocalHunter.current.muted)
        Category.values().toList().chunked(3).forEach { group ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                group.forEach { value ->
                    HChip(value.code.removePrefix("tren_"), category == value,
                        Modifier.weight(1f)) { category = value; proposals = emptyList() }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(10, 15, 20).forEach { n ->
                HChip(n.toString(), duration == n, Modifier.weight(1f)) { duration = n }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(30, 45, 60).forEach { n ->
                HChip(n.toString(), duration == n, Modifier.weight(1f)) { duration = n }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RoutineLevel.values().forEach { value ->
                HChip(value.name.lowercase(), level == value, Modifier.weight(1f)) {
                    level = value
                }
            }
        }
        HButton("GENERAR ALTERNATIVAS", Modifier.fillMaxWidth()) {
            val profile = AsterionProfileAdapter.from(store)
            if (profile == null) {
                info = "Completa tu perfil primero."
                proposals = emptyList()
            } else {
                ui.attempt {
                    proposals = RoutineGenerator.generate(
                        RoutineRequest(category, duration, level, LocalDate.now(), "manual"),
                        profile, AsterionActivitySource(store))
                    info = if (proposals.isEmpty()) "No hay alternativas compatibles."
                        else proposals.size.toString() + " alternativas compatibles."
                }
            }
        }
        if (info.isNotBlank()) HNotice(info)
        proposals.forEachIndexed { index, routine ->
            HText("Alternativa " + (index + 1) + ": " + routine.ids.size +
                " pasos · ventana " + duration + " min", size = 15, bold = true)
            routine.ids.forEach { id ->
                HText(store.catalog.byId[id]?.name ?: id, size = 13)
            }
        }
        val points = remember(revision) { progress.routinePoints() }
        HText("Participación declarada en rutinas: " + points +
            " puntos (10 como máximo por fecha).", size = 13)
        HButton("DECLARAR RUTINA COMPLETADA HOY", Modifier.fillMaxWidth(),
            enabled = proposals.isNotEmpty()) {
            ui.attempt {
                val granted = progress.awardRoutine(LocalDate.now(), true)
                info = if (granted == 0) "La participación de hoy ya estaba registrada."
                    else "Participación de hoy registrada: 10 puntos."
            }
        }
        HText("Completar más pasos o más tiempo no concede puntos adicionales.",
            size = 12, color = LocalHunter.current.muted)
    }
}


/** Desafío de cinco días: distinto del evento semanal heredado, siempre optativo. */
@Composable
fun AsterionMonarchSection(ui: NativeUi) {
    val store = ui.store
    val revision = store.revision.intValue
    val start = LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
    val cycle = start.toString()
    val repository = remember(store) { AsterionProgressRepository(store) }
    val accepted = remember(revision, cycle) { repository.monarchConsented(cycle) }
    val profile = AsterionProfileAdapter.from(store)
    val plan = remember(revision, cycle, accepted) {
        profile?.let {
            MonarchChallengePlanner.propose(start, accepted, it,
                AsterionActivitySource(store), repository.efforts())
        }
    }
    val done = remember(revision, cycle) { repository.monarchCompleted(cycle).toSet() }
    HPanel(padding = 16) {
        Eyebrow("ASTERION · DESAFÍO MONARCA DE CINCO DÍAS")
        Heading("Ciclo voluntario", size = 22)
        HText("Las preferencias del jefe nunca modifican la elegibilidad, el equipo ni la complejidad.",
            size = 13, color = LocalHunter.current.muted)
        HButton(if (accepted) "DESACTIVAR CICLO SIN PENALIZACIÓN"
            else "ACEPTAR CICLO DE CINCO DÍAS", Modifier.fillMaxWidth()) {
            ui.attempt { repository.setMonarchConsent(cycle, !accepted) }
        }
        if (accepted) {
            if (plan == null || plan.days.isEmpty()) {
                HNotice("No hay días o actividades compatibles. El descanso se mantiene.")
            } else {
                plan.days.forEach { day ->
                    HText(day.date.toString() + " · " +
                        (day.category?.code?.replace('_', ' ') ?: "Descanso"),
                        size = 15, bold = true)
                    if (day.ids.isEmpty()) {
                        HText("Sin actividades asignadas.", size = 12,
                            color = LocalHunter.current.muted)
                    }
                    day.ids.forEach { id ->
                        val finished = day.date.toString() + "|" + id in done
                        HText((store.catalog.byId[id]?.name ?: id) +
                            if (finished) " · Declarada completada" else " · Opcional",
                            size = 13, color = LocalHunter.current.muted)
                        if (day.date == LocalDate.now() && !finished) {
                            HButton("DECLARAR PARTICIPACIÓN", Modifier.fillMaxWidth()) {
                                ui.attempt {
                                    repository.markMonarchCompletion(cycle, day.date, id)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
