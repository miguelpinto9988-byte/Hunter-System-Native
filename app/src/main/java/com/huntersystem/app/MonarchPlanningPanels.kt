package com.huntersystem.app

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.huntersystem.app.planning.*
import java.time.LocalDate

/** Componentes para el ÚNICO planificador nativo de Monarca Eclipse. */
@Composable
fun MonarchCategoryReview(ui: NativeUi) {
    val store = ui.store
    val revision = store.revision.intValue
    val progress = remember(store) { MonarchProgressRepository(store) }
    var window by remember { mutableIntStateOf(7) }
    val date = LocalDate.now()
    val records = remember(revision) { progress.plannedParticipation() }
    val efforts = remember(revision) { progress.efforts() }
    val interruptions = remember(revision) { progress.interruptions() }
    val volume = remember(revision) { progress.volume() }
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
            val review = WeeklyCategoryChallengePolicy.review(efforts, date, category, interruptions)
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
            "última revisión " + (if (global.previous == 0) "pendiente" else "día " + global.previous) +
            " · próxima día " + global.next +
            (if (global.previous == 21) " del siguiente ciclo" else "") +
            " · " + global.observations + " valoraciones · " +
            (global.average?.let { "%.1f".format(it) + "/10" } ?: "sin datos suficientes"),
            size = 13, color = LocalHunter.current.muted)
        HText(global.explanation, size = 12, color = LocalHunter.current.muted)
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
        val volumes = volume.filter {
            it.date <= date && (window == 0 || it.date >= date.minusDays((window - 1).toLong()))
        }.sortedByDescending { it.date }.take(12)
        if (volumes.isNotEmpty()) {
            Eyebrow("VOLUMEN DECLARADO")
            volumes.forEach { entry ->
                val name = store.catalog.byId[entry.variantId]?.name ?: entry.variantId
                val unit = if (entry.unit == VolumeUnit.SECONDS) "segundos" else "repeticiones"
                val effort = entry.rpe?.let { " · esfuerzo $it/10" }.orEmpty()
                val interruption = if (entry.interrupted) " · interrumpida" else ""
                HText("${entry.date} · $name · ${entry.sets} × ${entry.quantityPerSet} $unit$effort$interruption",
                    size = 12, color = LocalHunter.current.muted)
            }
        }
    }
}

@Composable
fun MonarchFiveDayChallenge(ui: NativeUi) {
    val store = ui.store
    val revision = store.revision.intValue
    val repository = remember(store) { MonarchProgressRepository(store) }
    val start = remember(revision) { repository.currentMonarchStart(LocalDate.now()) }
    val cycle = start.toString()
    val accepted = remember(revision, cycle) { repository.monarchConsented(cycle) }
    val profile = MonarchProfileAdapter.from(store)
    var preferred by remember(cycle) { mutableStateOf("none") }
    val canOffer = profile != null && profile.weekly.any { it != 5 }
    val plan = remember(revision, cycle, accepted) {
        if (profile == null) null else repository.monarchPlan(start)
    }
    val done = remember(revision, cycle) { repository.monarchCompleted(cycle).toSet() }
    HPanel(padding = 16) {
        Eyebrow("MONARCA · DESAFÍO VOLUNTARIO DE CINCO DÍAS")
        Heading("Ciclo voluntario", size = 22)
        HText("El enfoque del encuentro solo cambia el orden. Se aplican los límites de calendario, equipo y técnica.",
            size = 13, color = LocalHunter.current.muted)
        if (canOffer) HText("Del $start al ${start.plusDays(4)}", size = 13)
        if (!accepted && canOffer) {
            HSelect("Enfoque opcional del encuentro", preferred,
                listOf("none" to "Sin prioridad") +
                    MonarchEncounter.values().map { it.name to it.label } +
                    Category.values().map { it.code to it.code.replace('_', ' ') }) {
                preferred = it
            }
        }
        if (accepted) repository.monarchEncounter(cycle)?.let {
            HText(it.label, size = 14, bold = true)
        }
        if (!accepted && !canOffer)
            HNotice("Tu calendario contiene únicamente descanso o falta configurar el perfil.")
        HButton(if (accepted) "DESACTIVAR CICLO SIN PENALIZACIÓN"
            else "ACEPTAR CICLO DE CINCO DÍAS", Modifier.fillMaxWidth(),
            enabled = accepted || canOffer) {
            ui.attempt {
                repository.setMonarchConsent(cycle, !accepted,
                    CategoryMapper.normalize(preferred),
                    MonarchEncounter.values().firstOrNull { it.name == preferred })
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
                    HText(day.explanation, size = 12, color = LocalHunter.current.muted)
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
    ui: NativeUi, progress: MonarchProgressRepository,
    day: LocalDate, id: String, category: Category?
) {
    if (category == null) return
    val exercise = ui.store.catalog.byId[id] ?: return
    val unit = when (exercise.unit) {
        "sec" -> VolumeUnit.SECONDS
        "reps" -> VolumeUnit.REPETITIONS
        else -> return
    }
    val metadata = remember(ui.store, id) { ui.store.planner.planningSource(ui.store.adjusted()).byId(id) }
        ?: return
    if (metadata.technicalLevel !in 1..3 || metadata.category != category) return
    var sets by remember(day, id) { mutableStateOf("") }
    var amount by remember(day, id) { mutableStateOf("") }
    var effort by remember(day, id) { mutableStateOf("") }
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
    HInput(effort, { effort = it }, "Esfuerzo de esta actividad (opcional, 1–10)", Modifier.fillMaxWidth(), number = true)
    HButton("REGISTRAR CANTIDAD REAL", Modifier.fillMaxWidth()) {
        ui.attempt {
            val numberSets = sets.toIntOrNull() ?: error("Indica las series reales")
            val perSet = amount.toIntOrNull() ?: error("Indica una cantidad real")
            require(numberSets in 1..30 && perSet in 1..3600) { "Cantidad fuera de rango" }
            val p = MonarchProfileAdapter.from(ui.store) ?: error("Perfil no disponible")
            val block = progress.currentVolumeBlock(metadata, p)
            val dayRpe = effort.toIntOrNull()
            require(effort.isBlank() || (dayRpe ?: 0) in 1..10) { "Indica un esfuerzo entre 1 y 10" }
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
    val repository = remember(store) { MonarchProgressRepository(store) }
    val metadata = remember(store, revision, variantId) {
        store.planner.planningSource(store.adjusted()).byId(variantId)
    } ?: return
    val status = remember(revision, date, variantId) {
        repository.participation().firstOrNull {
            it.date == day && it.variantId == variantId
        }
    }
    if (status?.status == ParticipationStatus.COMPLETED)
        MonarchVolumeEntry(ui, repository, day, variantId, metadata.category)
    val recommendation = remember(revision, date, variantId) {
        runCatching { repository.volumeRecommendation(day, variantId) }.getOrNull()
    } ?: return
    val bounds = remember(revision, variantId) { repository.find(variantId) }
    if (bounds == null && status?.status != ParticipationStatus.COMPLETED) return
    HText(recommendation.explanation, size = 12, color = LocalHunter.current.muted)
    val accepted = remember(revision, variantId) { repository.acceptedReference(variantId) }
    val unit = bounds?.unit ?: return
    val label = if (unit == VolumeUnit.SECONDS) "segundos" else "repeticiones"
    if (recommendation.reference != null && recommendation.sets != null)
        HText("Referencia orientativa · ${recommendation.sets} series de ${recommendation.reference} $label", size = 12)
    if (accepted != null)
        HText("Referencia guardada · ${accepted.sets} series de ${accepted.value} $label", size = 12)
    if (recommendation.optional && recommendation.reference != null && recommendation.sets != null) {
        HButton("ACEPTAR REFERENCIA OPCIONAL", Modifier.fillMaxWidth()) {
            ui.attempt { repository.acceptVolumeReference(day, variantId) }
        }
    }
    if (accepted != null || recommendation.reference != null) {
        var editing by remember(day, variantId) { mutableStateOf(false) }
        HButton(if (editing) "CERRAR EDICIÓN" else "EDITAR REFERENCIA", Modifier.fillMaxWidth()) {
            editing = !editing
        }
        if (editing) {
            var sets by remember(accepted) { mutableStateOf((accepted?.sets ?: recommendation.sets)?.toString().orEmpty()) }
            var amount by remember(accepted) { mutableStateOf((accepted?.value ?: recommendation.reference)?.toString().orEmpty()) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HInput(sets, { sets = it }, "Series (1–3)", Modifier.weight(1f), number = true)
                HInput(amount, { amount = it }, label, Modifier.weight(1f), number = true)
            }
            HButton("GUARDAR MI REFERENCIA", Modifier.fillMaxWidth()) {
                ui.attempt {
                    val value = amount.toIntOrNull() ?: error("Indica una cantidad")
                    val setCount = sets.toIntOrNull() ?: error("Indica la cantidad de series")
                    require(value in bounds.minimum..bounds.maximum && setCount in 1..3) {
                        "Usa ${bounds.minimum}–${bounds.maximum} $label y 1–3 series"
                    }
                    repository.saveVolumeReference(day, variantId, VolumeReference(value, setCount, unit))
                    editing = false
                }
            }
        }
    }

}

@Composable
fun MonarchTechnicalSection(ui: NativeUi, date: String) {
    if (date != today()) return
    val store = ui.store
    val revision = store.revision.intValue
    val today = LocalDate.parse(date)
    val profile = MonarchProfileAdapter.from(store) ?: return
    val category = profile.category(today) ?: return
    val repository = remember(store) { MonarchProgressRepository(store) }
    val phases = remember(revision) { repository.phases() }
    val checkpoint = remember(revision, date) {
        TechnicalCheckpointPolicy.review(profile, category, today,
            repository.efforts(), phases[category] ?: TechnicalPhase(), repository.interruptions())
    }
    Eyebrow("REVISIÓN TÉCNICA · MISMA SESIÓN")
    HText("Fase actual: " + (phases[category]?.phase ?: 0) +
        "/2 · día de experiencia " + checkpoint.day +
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

/** Reconoce la participación declarada en una rutina. */
@Composable
fun MonarchRoutineParticipation(ui: NativeUi) {
    val s = ui.store
    val revision = s.revision.intValue
    val repo = remember(s) { MonarchProgressRepository(s) }
    val points = remember(revision) { repo.routinePoints() }
    HPanel(padding = 14) {
        Eyebrow("PARTICIPACIÓN DE RUTINAS")
        HText("Puntos registrados: " + points +
            " · máximo 10 por fecha tras completar una rutina.", size = 13)
        val dates = remember(revision) { repo.routineDates() }
        if (dates.isNotEmpty()) HText("Fechas recientes · " + dates.take(5).joinToString(), size = 12)
        HButton("REGISTRAR RUTINA TERMINADA", Modifier.fillMaxWidth(),
            enabled = repo.canDeclareRoutine(LocalDate.now())) {
            ui.attempt {
                val added = repo.awardRoutine(LocalDate.now(), true)
                ui.message(if (added > 0) "Participación registrada."
                    else "La fecha ya estaba registrada.")
            }
        }
    }
}
