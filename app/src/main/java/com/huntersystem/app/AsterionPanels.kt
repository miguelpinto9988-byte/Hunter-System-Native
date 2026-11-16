package com.huntersystem.app

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.huntersystem.app.asterion.*
import java.time.LocalDate

/** Componentes para el ÚNICO planificador nativo de Monarca Eclipse. */
@Composable
fun MonarchCategoryReview(ui: NativeUi) {
    val store = ui.store
    val revision = store.revision.intValue
    val progress = remember(store) { AsterionProgressRepository(store) }
    var window by remember { mutableIntStateOf(7) }
    val date = LocalDate.now()
    val records = remember(revision) { progress.plannedParticipation() }
    val efforts = remember(revision) { progress.efforts() }
    val visible = records.filter {
        !it.date.isAfter(date) && (window == 0 || !it.date.isBefore(date.minusDays((window - 1).toLong())))
    }
    val summary = ProgressSummary.calculate(visible)
    val weeklyMonday = date.minusDays((date.dayOfWeek.value - 1).toLong())
    val weekly = ProgressSummary.calculate(records.filter {
        !it.date.isBefore(weeklyMonday) && !it.date.isAfter(date)
    })
    HPanel(padding = 16) {
        Eyebrow("MONARCA · RESULTADOS POR CATEGORÍA")
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            HChip("7 DÍAS", window == 7, Modifier.weight(1f)) { window = 7 }
            HChip("30 DÍAS", window == 30, Modifier.weight(1f)) { window = 30 }
            HChip("TODO", window == 0, Modifier.weight(1f)) { window = 0 }
        }
        Heading(summary.label, size = 28)
        HText("Planificadas: " + summary.planned + " · Completadas: " +
            summary.completed + " · Omitidas: " + summary.skipped +
            " · Pendientes: " + summary.pending, size = 14)
        HText("Las pendientes no se consideran incumplimientos. Tasa semanal: " +
            weekly.label, size = 12, color = LocalHunter.current.muted)
        Category.values().forEach { category ->
            val count = visible.count { it.category == category &&
                it.status == ParticipationStatus.COMPLETED }
            val categoryRatings = efforts.filter { it.category == category && it.date <= date &&
                (window == 0 || !it.date.isBefore(date.minusDays((window - 1).toLong()))) }
            val mean = categoryRatings.map { it.rpe }.takeIf { it.isNotEmpty() }?.average()
            val review = WeeklyCategoryChallengePolicy.review(efforts, date, category)
            HText(category.code.replace('_', ' ') + ": " + count +
                " completadas · esfuerzo " +
                (mean?.let { "%.1f".format(it) + "/10" } ?: "sin datos") +
                " · revisión " + review.name.lowercase().replace('_', ' '),
                size = 13, color = LocalHunter.current.muted)
        }
        val globalEfforts = efforts.filter { it.category == null }
        val first = globalEfforts.minOfOrNull { it.date } ?: date
        val global = EffortReviewEngine.review(first, date, globalEfforts)
        HText("Ciclo de RPE global: día " + global.cycleDay + "/21 · " +
            global.observations + " valoraciones · " +
            (global.average?.let { "%.1f".format(it) + "/10" } ?: "sin datos suficientes"),
            size = 13, color = LocalHunter.current.muted)
        HText("El RPE global heredado es descriptivo y nunca se traslada a una categoría.",
            size = 12, color = LocalHunter.current.muted)
        if (visible.isNotEmpty()) {
            Eyebrow("RESULTADOS POR FECHA · ÚLTIMOS REGISTROS")
            visible.sortedWith(compareByDescending<Participation> { it.date }
                .thenBy { it.variantId }).take(12).forEach { entry ->
                HText(entry.date.toString() + " · " +
                    (store.catalog.byId[entry.variantId]?.name ?: entry.variantId) +
                    " · " + entry.status.name.lowercase() +
                    (if (entry.interrupted) " · interrumpida" else ""),
                    size = 12, color = LocalHunter.current.muted)
            }
        } else HText("Sin actividades registradas para el período seleccionado.",
            size = 13, color = LocalHunter.current.muted)
    }
}

@Composable
fun MonarchFiveDayChallenge(ui: NativeUi) {
    val store = ui.store
    val revision = store.revision.intValue
    val start = LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
    val cycle = start.toString()
    val repository = remember(store) { AsterionProgressRepository(store) }
    val accepted = remember(revision, cycle) { repository.monarchConsented(cycle) }
    val profile = AsterionProfileAdapter.from(store)
    var preferred by remember(cycle) { mutableStateOf("none") }
    val canOffer = profile != null && profile.weekly.any { it != 5 }
    val plan = remember(revision, cycle, accepted) {
        if (profile == null) null else repository.monarchPlan(start)
    }
    val done = remember(revision, cycle) { repository.monarchCompleted(cycle).toSet() }
    HPanel(padding = 16) {
        Eyebrow("MONARCA · DESAFÍO VOLUNTARIO DE CINCO DÍAS")
        Heading("Ciclo voluntario", size = 22)
        HText("Las preferencias del jefe nunca modifican la elegibilidad, el equipo ni la complejidad.",
            size = 13, color = LocalHunter.current.muted)
        if (!accepted && canOffer) {
            HSelect("Prioridad de categoría opcional", preferred,
                listOf("none" to "Sin prioridad") +
                    Category.values().map { it.code to it.code.replace('_', ' ') }) {
                preferred = it
            }
        }
        if (!accepted && !canOffer)
            HNotice("Tu calendario contiene únicamente descanso o falta configurar el perfil.")
        HButton(if (accepted) "DESACTIVAR CICLO SIN PENALIZACIÓN"
            else "ACEPTAR CICLO DE CINCO DÍAS", Modifier.fillMaxWidth(),
            enabled = accepted || canOffer) {
            ui.attempt {
                repository.setMonarchConsent(cycle, !accepted,
                    CategoryMapper.normalize(preferred))
            }
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


/** Entrada voluntaria: sin referencias automáticas, sin series ni RPE inventados. */
@Composable
private fun MonarchVolumeEntry(
    ui: NativeUi, progress: AsterionProgressRepository,
    day: LocalDate, id: String, category: Category?
) {
    if (category == null) return
    val exercise = ui.store.catalog.byId[id] ?: return
    val unit = when (exercise.unit) {
        "sec" -> VolumeUnit.SECONDS
        "reps" -> VolumeUnit.REPETITIONS
        else -> return
    }
    val metadata = remember(ui.store, id) { AsterionActivitySource(ui.store).byId(id) }
        ?: return
    if (metadata.technicalLevel !in 1..3 || metadata.category != category) return
    var sets by remember(day, id) { mutableStateOf("") }
    var amount by remember(day, id) { mutableStateOf("") }
    val saved = progress.volume().firstOrNull {
        it.date == day && it.variantId == id
    }
    HText("Volumen realizado opcional (" +
        (if (unit == VolumeUnit.SECONDS) "segundos" else "repeticiones") +
        "). Sin registrar no se presume cantidad.", size = 12,
        color = LocalHunter.current.muted)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HInput(sets, { sets = it }, "Series realizadas", Modifier.weight(1f), number = true)
        HInput(amount, { amount = it }, "Cantidad por serie", Modifier.weight(1f), number = true)
    }
    HButton("REGISTRAR CANTIDAD REAL", Modifier.fillMaxWidth()) {
        ui.attempt {
            val numberSets = sets.toIntOrNull() ?: error("Indica las series reales")
            val perSet = amount.toIntOrNull() ?: error("Indica una cantidad real")
            require(numberSets in 1..30 && perSet in 1..3600) { "Cantidad fuera de rango" }
            val p = AsterionProfileAdapter.from(ui.store) ?: error("Perfil no disponible")
            val block = progress.blocks()[id] ?: VolumeResetPolicy.block(
                category, id, p.experience, p.experienceStart)
            val dayRpe = progress.efforts().firstOrNull {
                it.date == day && it.category == category
            }?.rpe
            progress.recordVolume(VolumeObservation(
                "asterion|" + day + "|" + id,
                day, category, id, metadata.typeId, metadata.technicalLevel,
                unit, numberSets, perSet, dayRpe, true, false, block))
        }
    }
    if (saved != null)
        HText("Registro real guardado: " + saved.sets + " series · " +
            saved.quantityPerSet + " " + saved.unit.name, size = 12,
            color = LocalHunter.current.muted)
}


/** Misma misión fijada que ve la pantalla Misiones; nunca genera un segundo plan. */

/** La participación numérica se anota en la MISMA sesión nativa. */
@Composable
fun MonarchVolumeEditor(ui: NativeUi, date: String, variantId: String) {
    if (date != today()) return
    val store = ui.store
    val revision = store.revision.intValue
    val day = LocalDate.parse(date)
    val repository = remember(store) { AsterionProgressRepository(store) }
    val metadata = remember(store, revision, variantId) {
        AsterionActivitySource(store).byId(variantId)
    } ?: return
    val status = remember(revision, date, variantId) {
        repository.participation().firstOrNull {
            it.date == day && it.variantId == variantId
        }
    }
    if (status?.status != ParticipationStatus.COMPLETED) return
    MonarchVolumeEntry(ui, repository, day, variantId, metadata.category)
    val profile = AsterionProfileAdapter.from(store) ?: return
    val block = repository.blocks()[variantId]
        ?: VolumeResetPolicy.block(metadata.category, variantId,
            profile.experience, profile.experienceStart)
    val recommendation = remember(revision, date, variantId) {
        VolumeProgressionEngine.review(day, metadata, repository.find(variantId),
            repository.volume(), block, repository.efforts())
    }
    HText(recommendation.explanation, size = 12, color = LocalHunter.current.muted)
    if (recommendation.optional && recommendation.reference != null &&
        recommendation.decision in setOf(VolumeDecision.OPTIONAL_INCREASE,
            VolumeDecision.REVIEW_EXTRA_SET, VolumeDecision.OPTIONAL_MINIMUM_ADJUSTMENT)) {
        HButton("ACEPTAR REFERENCIA ORIENTATIVA (SIN APLICARLA)",
            Modifier.fillMaxWidth()) {
            ui.attempt { repository.acceptVolumeReference(day, variantId) }
        }
    }
}

@Composable
fun MonarchTechnicalSection(ui: NativeUi, date: String) {
    if (date != today()) return
    val store = ui.store
    val revision = store.revision.intValue
    val today = LocalDate.parse(date)
    val profile = AsterionProfileAdapter.from(store) ?: return
    val category = profile.category(today) ?: return
    val repository = remember(store) { AsterionProgressRepository(store) }
    val phases = remember(revision) { repository.phases() }
    val checkpoint = remember(revision, date) {
        TechnicalCheckpointPolicy.review(profile, category, today,
            repository.efforts(), phases[category] ?: TechnicalPhase())
    }
    Eyebrow("REVISIÓN TÉCNICA · MISMA SESIÓN")
    HText("Fase actual: " + ((phases[category]?.phase ?: 0) + 1) +
        "/3 · día de experiencia " + checkpoint.day +
        " · " + checkpoint.decision.name.lowercase().replace('_',' '),
        size = 13, color = LocalHunter.current.muted)
    if (checkpoint.decision == TechnicalDecision.ADVANCE_PHASE) {
        HButton("ACEPTAR FASE OPCIONAL", Modifier.fillMaxWidth()) {
            ui.attempt { repository.acceptPhase(category, checkpoint) }
        }
    }
    if ((phases[category]?.phase ?: 0) > 0) {
        HButton("VOLVER A FASE ANTERIOR", Modifier.fillMaxWidth()) {
            ui.attempt { repository.revertPhase(category) }
        }
    }
}

/** No hay segundo generador. Solo acredita una rutina histórica ya terminada. */
@Composable
fun MonarchRoutineParticipation(ui: NativeUi) {
    val s = ui.store
    val revision = s.revision.intValue
    val repo = remember(s) { AsterionProgressRepository(s) }
    val day = s.day(today())
    val log = s.logs.optJSONObject(today())
    val points = remember(revision) { repo.routinePoints() }
    HPanel(padding = 14) {
        Eyebrow("PARTICIPACIÓN DE RUTINAS")
        HText("Puntos registrados: " + points +
            " · máximo 10 por fecha tras completar una rutina.", size = 13)
        HButton("REGISTRAR RUTINA TERMINADA", Modifier.fillMaxWidth(),
            enabled = day.has("routineId") && log?.b("complete") == true) {
            ui.attempt {
                val added = repo.awardRoutine(LocalDate.now(), true)
                ui.message(if (added > 0) "Participación registrada."
                    else "La fecha ya estaba registrada.")
            }
        }
    }
}
