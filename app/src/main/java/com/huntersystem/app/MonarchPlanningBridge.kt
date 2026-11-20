package com.huntersystem.app

import com.huntersystem.app.planning.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

enum class MonarchEncounter(val label: String, val priorities: List<Category>) {
    GUARDIA("Guardia del Umbral", listOf(Category.CARDIO, Category.MOBILITY)),
    FORTALEZA("Asalto a la Fortaleza", listOf(Category.UPPER, Category.LOWER)),
    TRONO("Juicio del Trono Monarca", listOf(Category.CENTRAL, Category.UPPER))
}

/** Puente de solo lectura al catálogo ACTUAL: no incorpora fichas ni inventarios nuevos. */
class MonarchActivitySource(
    private val catalog: Catalog, private val rules: Rules, private val adjusted: JSONObject
) : ActivityRepositoryInterface {
    constructor(store: NativeStore) : this(store.catalog, store.rules, store.adjusted())
    override val revision: String = "native-" + catalog.exercises.size
    private val rows by lazy {
        catalog.exercises.mapNotNull { e ->
            val category = CategoryMapper.normalize(e.type) ?: return@mapNotNull null
            val declaredLevel = when {
                e.raw.has("technicalLevel") -> e.raw.i("technicalLevel")
                e.raw.has("technicalLevel46") -> e.raw.i("technicalLevel46")
                e.meta.has("level") -> e.planningLevel + 1
                else -> 0 // sin metadatos: nivel 0 bloqueado
            }.takeIf { it in 0..5 } ?: 0
            val levelValue = if (declaredLevel == 0) 0 else maxOf(
                declaredLevel, e.planningLevel + 1, e.raw.i("progressionTierNative") + 1,
                if (e.raw.b("pack46")) e.raw.i("level") + 1 else 1
            ).coerceAtMost(5)
            val declaredMaterials = e.raw.optJSONArray("materialRequirements")
            val allowedGear = buildSet {
                addAll(e.raw.a("materials46").strings())
                addAll(e.raw.a("materialRequirements").strings())
                if (e.equipment == "db") add("db")
                if (e.equipment == "gym" || e.gym) add("gym")
            }.filterNot { it == "body" }.toSet()
            val intensity = when (e.raw.s("intensity", e.meta.s("intensity")).lowercase()) {
                "low", "baja", "suave" -> Intensity.LOW
                "moderate", "moderada", "media", "medium" -> Intensity.MODERATE
                "high", "alta" -> Intensity.HIGH
                else -> when {
                    // Solo descriptores que ya constan en el catálogo; no inferir técnica de edad.
                    !e.power && !e.impact &&
                        e.raw.s("impact") in listOf("Bajo", "Sin saltos") &&
                        e.meta.has("conditioning") && e.meta.i("conditioning") <= 1 ->
                        Intensity.LOW
                    (e.raw.s("impact") in listOf("Moderado", "Medio") ||
                        e.hunter2000 && !e.power && !e.impact && e.raw.s("impact") == "Sin saltos") &&
                        e.meta.has("conditioning") && e.meta.i("conditioning") <= 2 ->
                        Intensity.MODERATE
                    else -> Intensity.UNKNOWN
                }
            }
            val familyId = category.code + "/" + e.family
            val typeId = familyId + "/" + e.pattern.ifBlank { e.oldFamily }
            val eligibility = (declaredMaterials != null || e.raw.optJSONArray("materials46") != null) &&
                rules.compatible(e, adjusted, maxPlanningLevel = 2)
            ActivityMetadata(e.id, category, familyId, typeId, levelValue, intensity, allowedGear,
                e.raw.s("modality", "no_especificada"),
                e.raw.b("requiresSupervision") || e.raw.b("supervisionRequired") ||
                    e.raw.b("highSkill46"),
                eligibility, buildSet {
                    addAll(e.raw.a("sportTags").strings())
                    addAll(e.raw.a("goalTags").strings())
                    addAll(e.raw.a("tags").strings())
                    if (e.pattern.isNotBlank()) add(e.pattern)
                    addAll(PersonalGoalInterpreter.affinity(
                        (listOf(e.name, e.sport, e.pattern) + this.toList()).joinToString(" ")
                    ))
                }, e.raw.a("restrictionTags").strings().toSet(), e.sport, e.name,
                when (e.unit) { "sec" -> VolumeUnit.SECONDS; "reps" -> VolumeUnit.REPETITIONS; else -> null })
        }
    }
    private val validatedRows by lazy { rows.also(ActivityCatalogValidator::verify) }
    private val cachedHash by lazy { ProfileSignature.source(validatedRows) + ":" + revision }
    private val variantsById by lazy { validatedRows.associateBy(ActivityMetadata::id) }
    override fun all(): List<ActivityMetadata> = validatedRows
    override fun byId(id: String): ActivityMetadata? = variantsById[id]
    override fun verified(): List<ActivityMetadata> = validatedRows
    override fun signature(): String = cachedHash
}

/** Traduce los índices históricos domingo=0 a lunes=0, sin reescribir perfiles guardados. */
object MonarchProfileAdapter {
    fun from(store: NativeStore): TrainingProfile? {
        val p = store.profile ?: return null
        val savedStart = (store.state.optJSONObject("monarcaPlanningV2")
            ?: store.state.o("asterionV1")).s("experienceSince")
        val since = runCatching { LocalDate.parse(savedStart) }.getOrNull()
            ?: runCatching { LocalDate.parse(p.s("experienceStartedAt")) }.getOrNull()
            ?: LocalDate.now()
        return from(p, since)
    }
    fun from(p: JSONObject, since: LocalDate = LocalDate.now()): TrainingProfile? {
        val experience = when (p.s("difficulty", p.s("experience"))) {
            "advanced", "avanzado" -> Experience.ADVANCED
            "intermediate", "intermedio" -> Experience.INTERMEDIATE
            else -> Experience.BEGINNER
        }
        // Los formatos `weekCategories` (texto) y `weeklyCategories` (0..5)
        // usan lunes=0; el calendario legacy `weekPlan` usa domingo=0.
        val numericWeek = p.optJSONArray("weeklyCategories")
        val textualWeek = p.optJSONArray("weekCategories")
        val explicitWeek: List<Int>? = when {
            numericWeek != null -> {
                if (numericWeek.length() != 7) return null
                (0..6).map { index ->
                    numericWeek.opt(index)?.toString()?.toIntOrNull()?.takeIf { it in 0..5 }
                        ?: return null
                }
            }
            textualWeek != null -> {
                if (textualWeek.length() != 7) return null
                (0..6).map { index ->
                    val value = textualWeek.optString(index)
                    if (value.trim().lowercase() in listOf("descanso", "rest")) 5
                    else CategoryMapper.normalize(value)?.ordinal ?: return null
                }
            }
            else -> null
        }
        val week = explicitWeek ?: (0..6).map { mondayIndex ->
            val oldIndex = (mondayIndex + 1) % 7
            when (p.a("weekPlan").optString(oldIndex, "rest")) {
                "train" -> when (p.a("weekFocus").optString(oldIndex, "upper")) {
                    "upper" -> 0
                    "core", "tronco", "integrado" -> 1
                    "lower", "piernas" -> 2
                    else -> 5
                }
                "mobility", "recovery" -> 3
                "cardio" -> 4
                else -> 5
            }
        }
        val declared = buildSet {
            if(p.b("bodyAvailable",true))add("body")
            val gear = p.s("equipment", "body")
            if (p.b("dumbbellsAvailable", gear in listOf("db", "gym"))) add("db")
            if (gear == "gym") add("gym")
            addAll(p.a("materials46").strings())
            val gym=p.a("gymEquipment23").strings()
            addAll(gym)
            if(gym.any { it in setOf("bike","rower","elliptical") })add("cardio")
            if (p.b("bench")) add("bench")
            if (p.b("step")) add("step")
        }
        val restrictions = p.o("restrictions").entries().mapNotNull {
            if (it.second == true) it.first else null
        }.toSet()
        return runCatching {
            TrainingProfile(experience, since, week, declared, restrictions,
                p.s("intensityPreference", "baja") in listOf("low", "baja"),
                p.i("dailyMissions", 3).coerceIn(1, 7),
                p.i("minutes", 20).coerceAtLeast(1),
                p.s("custom_goal", p.s("finalGoal")), p.s("goal", "general"),
                if (p.s("trainingMode") in listOf("routines", "rutinas")) "rutinas" else "misiones",
                p.i("restSeconds", 60), p.s("blockMode22") == "rounds")
        }.getOrNull()
    }
}

/**
 * Los datos nuevos se guardan en monarcaPlanningV2 dentro del AtomicFile. Los
 * datos previos de asterionV1 se leen como respaldo, sin perder el historial.
 */
class MonarchProgressRepository(
    private val store: NativeStore,
    private val activityLookup: ((String) -> ActivityMetadata?)? = null
) : VolumeProfileInterface {
    private fun activitySource() = store.planner.planningSource(store.adjusted())
    private fun activity(id: String): ActivityMetadata? =
        if (activityLookup != null) activityLookup.invoke(id) else activitySource().byId(id)

    fun history() = TrainingHistory(efforts(), participation(), interruptions(), phases())

    /** Solo lee referencias ya validadas y entregadas por una integración externa. */
    override fun find(variantId: String): VolumeProfile? {
        val workUnit = activity(variantId)?.workUnit ?: return null
        val exercise = store.catalog.byId[variantId]
        // Fuente primaria: metadatos de la misma variante en el catálogo externo.
        // Fuente importada: exige identidad y revisión del proveedor verificables.
        val catalogProfile = exercise?.raw?.optJSONObject("volumeProfile")
        val record = if (catalogProfile?.b("validated") == true) catalogProfile
            else root().o("validatedVolumeProfiles").optJSONObject(variantId) ?: return null
        return runCatching {
            require(record.b("validated") && record.s("variantId") == variantId)
            require(catalogProfile === record ||
                (record.s("sourceId").isNotBlank() &&
                 record.s("sourceRevision").isNotBlank()))
            require(record.has("minimum") && record.has("maximum") &&
                record.has("permittedIncrement"))
            val unit = VolumeUnit.valueOf(record.s("unit"))
            require(unit == workUnit)
            VolumeProfile(variantId, unit,
                record.i("initialSets").takeIf { record.has("initialSets") },
                record.i("initialValue").takeIf { record.has("initialValue") },
                record.i("minimum"), record.i("maximum"),
                record.i("permittedIncrement"), record.b("automaticReviewEligible"))
        }.getOrNull()
    }
    private fun root(): JSONObject =
        store.state.optJSONObject("monarcaPlanningV2") ?: store.state.o("asterionV1")
    private fun write(root: JSONObject) {
        store.state.put("monarcaPlanningV2", root)
        store.save()
    }
    private fun date(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()
    fun planningRevisionKey(): String {
        val state = root()
        return ProfileSignature.stable("progreso", listOf("phases", "efforts", "participation")
            .joinToString("|") { state.opt(it)?.toString().orEmpty() })
    }

    private fun explicitEfforts(): List<EffortObservation> =
        root().a("efforts").objects().mapNotNull { row ->
            val day = date(row.s("date")) ?: return@mapNotNull null
            val category = CategoryMapper.normalize(row.s("category"))
            val rpe = row.i("rpe")
            if (rpe !in 1..10) return@mapNotNull null
            EffortObservation(day, category, rpe, row.b("completed"), row.b("interrupted"))
        }
    fun efforts(): List<EffortObservation> =
        MonarchLegacyHistory.mergeEffort(
            MonarchLegacyHistory.efforts(store.logs, store.catalog), explicitEfforts())
    fun interruptions(): List<SessionInterruption> =
        (MonarchLegacyHistory.interruptions(store.logs, store.catalog) +
            explicitParticipation().filter { it.interrupted }.map { SessionInterruption(it.date, it.category) })
            .distinct()
    fun addEffort(day: LocalDate, category: Category?, rpe: Int, completed: Boolean,
                  interrupted: Boolean = false) {
        require(rpe in 1..10 && !day.isAfter(LocalDate.now()))
        val r = root()
        val previous = r.a("efforts").objects().filterNot {
            it.s("date") == day.toString() && it.s("category") == (category?.code ?: "global")
        }
        r.put("efforts", arr(previous + obj("date" to day.toString(),
            "category" to (category?.code ?: "global"), "rpe" to rpe,
            "completed" to completed, "interrupted" to interrupted)))
        write(r)
    }
    private fun explicitParticipation(): List<Participation> =
        root().a("participation").objects().mapNotNull { row ->
        val day = date(row.s("date")) ?: return@mapNotNull null
        val category = CategoryMapper.normalize(row.s("category")) ?: return@mapNotNull null
        val status = runCatching { ParticipationStatus.valueOf(row.s("status")) }.getOrNull()
            ?: return@mapNotNull null
        Participation(day, row.s("id"), category, status, row.s("notes"), row.b("interrupted"))
    }
    fun participation(): List<Participation> =
        MonarchLegacyHistory.mergeParticipation(
            explicitParticipation(),
            MonarchLegacyHistory.participation(store.logs, store.catalog))
    /**
     * Estados pendientes derivados exclusivamente de propuestas realmente guardadas.
     * No convierte días omitidos, descansos ni actividades futuras en fracasos.
     */
    fun plannedParticipation(until: LocalDate = LocalDate.now()): List<Participation> {
        val planned = mutableListOf<Participation>()
        for (day in root().a("missions").objects()) {
            val date = date(day.s("date")) ?: continue
            if (date.isAfter(until)) continue
            val category = CategoryMapper.normalize(day.s("category")) ?: continue
            for (id in day.a("ids").strings()) {
                val exercise = store.catalog.byId[id] ?: continue
                if (CategoryMapper.normalize(exercise.type) == category)
                    planned += Participation(date, id, category, ParticipationStatus.PENDING)
            }
        }
        for (day in store.allDays()) {
            val date = date(day.s("date")) ?: continue
            if (date.isAfter(until) || day.s("kind") == "rest") continue
            for (item in day.a("items").objects()) {
                val id = item.s("id")
                val category = CategoryMapper.normalize(store.catalog.byId[id]?.type ?: "")
                    ?: continue
                planned += Participation(date, id, category, ParticipationStatus.PENDING)
            }
        }
        return MonarchLegacyHistory.mergeParticipation(planned, participation())
    }
    fun setParticipation(day: LocalDate, variantId: String, status: ParticipationStatus,
                         notes: String = "", interrupted: Boolean = false) {
        require(!day.isAfter(LocalDate.now()))
        val mission = pinned(day) ?: error("No hay misión fijada para la fecha")
        require(mission.category != null && variantId in mission.ids) { "ID fuera de la misión" }
        val all = ProgressTrackingService.record(explicitParticipation(),
            Participation(day, variantId, mission.category, status, notes.take(1000), interrupted))
        val r = root()
        r.put("participation", arr(all.map {
            obj("date" to it.date.toString(), "id" to it.variantId,
                "category" to it.category.code, "status" to it.status.name,
                "notes" to it.notes, "interrupted" to it.interrupted)
        }))
        write(r)
    }
    fun pinned(day: LocalDate): DailyMission? {
        val row = root().a("missions").objects().firstOrNull { it.s("date") == day.toString() }
            ?: return null
        val ids = row.a("ids").strings()
        if (ids.any { it.isBlank() } || ids.distinct().size != ids.size) return null
        return DailyMission(day, CategoryMapper.normalize(row.s("category")),
            row.s("profileHash"), row.s("catalogHash"), ids, row.s("explanation"),
            row.o("equivalentFrom").entries().mapNotNull { (id, value) ->
                (value as? String)?.let { id to it }
            }.toMap())
    }
    fun pin(mission: DailyMission) {
        val r = root()
        val missions = r.a("missions").objects().filterNot { it.s("date") == mission.date.toString() }
        r.put("missions", arr((missions + obj("date" to mission.date.toString(),
            "category" to (mission.category?.code ?: "descanso"),
            "profileHash" to mission.profileHash, "catalogHash" to mission.catalogHash,
            "ids" to arr(mission.ids), "explanation" to mission.explanation,
            "equivalentFrom" to JSONObject(mission.equivalentFrom))).takeLast(40)))
        write(r)
    }
    fun phases(): Map<Category, TechnicalPhase> = root().o("phases").entries().mapNotNull { (k, v) ->
        val category = CategoryMapper.normalize(k) ?: return@mapNotNull null
        val x = v as? JSONObject ?: return@mapNotNull null
        val changed = date(x.s("lastChange"))
        category to TechnicalPhase(x.i("phase").coerceIn(0, 2), changed)
    }.toMap()
    fun acceptPhase(category: Category, checkpoint: TechnicalCheckpoint) {
        require(checkpoint.decision == TechnicalDecision.ADVANCE_PHASE)
        val today = LocalDate.now()
        val profile = MonarchProfileAdapter.from(store) ?: error("Perfil no disponible")
        require(profile.category(today) == category)
        val current = phases()[category] ?: TechnicalPhase()
        val verified = TechnicalCheckpointPolicy.review(profile, category, today, efforts(),
            current, interruptions())
        require(verified == checkpoint) { "La revisión técnica ya cambió" }
        val r = root()
        val entries = r.o("phases")
        entries.put(category.code, obj("phase" to checkpoint.phase.phase,
            "lastChange" to (checkpoint.phase.lastChange?.toString() ?: "")))
        r.put("phases", entries)
        val accepted = r.o("acceptedTechnicalChanges")
        accepted.put(category.code, today.toString())
        r.put("acceptedTechnicalChanges", accepted)
        write(r)
        store.refreshWeek()
    }
    /** Reversión explícita sin borrar hitos anteriores ni historial por categoría. */
    fun revertPhase(category: Category): Boolean {
        val current = phases()[category] ?: return false
        if (current.phase == 0) return false
        val r = root()
        val phases = r.o("phases")
        phases.put(category.code, obj("phase" to current.phase - 1,
            "lastChange" to LocalDate.now().toString()))
        r.put("phases", phases)
        val accepted = r.o("acceptedTechnicalChanges")
        accepted.remove(category.code)
        r.put("acceptedTechnicalChanges", accepted)
        write(r)
        store.refreshWeek()
        return true
    }
    fun volume(): List<VolumeObservation> = root().a("volume").objects().mapNotNull { row ->
        runCatching {
            VolumeObservation(row.s("id"), LocalDate.parse(row.s("date")),
                CategoryMapper.normalize(row.s("category")) ?: error("Categoría no válida"),
                row.s("variantId"), row.s("typeId"), row.i("technicalLevel"),
                VolumeUnit.valueOf(row.s("unit")), row.i("sets"), row.i("quantity"),
                row.i("rpe").takeIf { it in 1..10 }, row.b("completed"),
                row.b("interrupted"), row.s("blockId"))
        }.getOrNull()
    }
    fun recordVolume(observation: VolumeObservation) = synchronized(store) {
        require(!observation.date.isAfter(LocalDate.now()))
        require(observation.completed) { "Volumen reservado a actividad completada explícitamente" }
        val metadata = activity(observation.variantId) ?: error("Variante de volumen desconocida")
        require(metadata.category == observation.category && metadata.typeId == observation.typeId &&
            metadata.technicalLevel == observation.technicalLevel && metadata.workUnit == observation.unit) {
            "Los metadatos del registro no coinciden con la variante"
        }
        val declared = participation().firstOrNull {
            it.date == observation.date && it.variantId == observation.variantId &&
                it.category == observation.category && it.status == ParticipationStatus.COMPLETED
        } ?: error("La actividad debe estar confirmada antes de registrar volumen")
        val profile = MonarchProfileAdapter.from(store)
        val expectedBlock = profile?.let { currentVolumeBlock(metadata, it) } ?: blocks()[metadata.id]
        require(expectedBlock == observation.blockId) { "La referencia pertenece a otro bloque" }
        val actual = observation.copy(interrupted = declared.interrupted || observation.interrupted)
        val r = root()
        val rows = r.a("volume").objects().filterNot {
            it.s("id") == actual.id || (it.s("date") == actual.date.toString() &&
                it.s("variantId") == actual.variantId && it.s("blockId") == actual.blockId &&
                it.s("unit") == actual.unit.name)
        }
        val record = actual.let {
            obj("id" to it.id, "date" to it.date.toString(),
                "category" to it.category.code, "variantId" to it.variantId,
                "typeId" to it.typeId, "technicalLevel" to it.technicalLevel,
                "unit" to it.unit.name, "sets" to it.sets, "quantity" to it.quantityPerSet,
                "rpe" to it.rpe, "completed" to it.completed, "interrupted" to it.interrupted,
                "blockId" to it.blockId)
        }
        r.put("volume", arr(rows + record))
        write(r)
    }
    /**
     * Guarda únicamente consentimiento de una referencia futura. No modifica
     * misiones, series realizadas, pesos ni el plan del día.
     */
    fun currentVolumeBlock(activity: ActivityMetadata, profile: TrainingProfile): String =
        blocks()[activity.id] ?: VolumeResetPolicy.block(activity.category, activity.id,
            profile.experience, profile.experienceStart)

    fun volumeRecommendation(day: LocalDate, variantId: String): VolumeRecommendation {
        val profile = MonarchProfileAdapter.from(store) ?: error("Perfil no configurado")
        val metadata = activity(variantId) ?: error("Variante desconocida")
        return TrainingSessionCoordinator.reviewVolume(day, metadata, this, volume(),
            currentVolumeBlock(metadata, profile), history())
    }

    fun acceptedReference(variantId: String): VolumeReference? {
        val profile = MonarchProfileAdapter.from(store) ?: return null
        val activity = activity(variantId) ?: return null
        val bounds = find(variantId) ?: return null
        val block = currentVolumeBlock(activity, profile)
        val row = root().o("acceptedVolumeReferences").optJSONObject("$variantId|$block") ?: return null
        return runCatching {
            val unit = VolumeUnit.valueOf(row.s("unit"))
            require(unit == bounds.unit && row.i("value") in bounds.minimum..bounds.maximum)
            VolumeReference(row.i("value"), row.i("sets"), unit)
        }.getOrNull()
    }

    fun acceptVolumeReference(day: LocalDate, variantId: String): Boolean {
        require(day == LocalDate.now()) { "La referencia solo puede aceptarse hoy" }
        val suggestion = volumeRecommendation(day, variantId)
        require(suggestion.optional && suggestion.reference != null && suggestion.sets != null &&
            suggestion.unit != null) { "No existe un ajuste opcional verificable para aceptar" }
        return saveVolumeReference(day, variantId,
            VolumeReference(suggestion.reference, suggestion.sets, suggestion.unit))
    }

    fun saveVolumeReference(day: LocalDate, variantId: String, reference: VolumeReference): Boolean {
        require(day == LocalDate.now()) { "La referencia solo puede editarse hoy" }
        val profile = MonarchProfileAdapter.from(store) ?: error("Perfil no configurado")
        val metadata = activity(variantId) ?: error("Variante desconocida")
        val bounds = find(variantId) ?: error("Sin referencia validada")
        require(reference.unit == bounds.unit && reference.value in bounds.minimum..bounds.maximum)
        val block = currentVolumeBlock(metadata, profile)
        val key = "$variantId|$block"
        val r = root()
        val accepted = r.o("acceptedVolumeReferences")
        val next = obj("variantId" to variantId, "blockId" to block,
            "unit" to reference.unit.name, "value" to reference.value,
            "sets" to reference.sets, "acceptedOn" to day.toString())
        if (accepted.optJSONObject(key)?.toString() == next.toString()) return false
        accepted.put(key, next)
        r.put("acceptedVolumeReferences", accepted)
        write(r)
        return true
    }

    /** Only the actual current day can close a milestone and open independent blocks. */
    fun syncVolumeBlocks(day: LocalDate, planned: JSONObject) {
        if (day != LocalDate.now()) return
        val profile = MonarchProfileAdapter.from(store) ?: return
        val category = profile.category(day) ?: return
        if (!planned.b("unifiedPlanning") || planned.s("progressCategory") != category.code ||
            planned.b("customized")) return
        val r = root()
        val phase = phases()[category] ?: TechnicalPhase()
        val window = TechnicalCheckpointPolicy.review(profile, category, day, efforts(),
            phase, interruptions())
        val acceptedDay = date(r.o("acceptedTechnicalChanges").s(category.code))
            ?.takeIf { !it.isBefore(profile.experienceStart) && it.isBefore(day) && !it.isBefore(day.minusDays(7)) }
        // A phase acceptance can affect the next scheduled session while today's pinned
        // mission stays unchanged. A ceiling review can only reset an actual equivalent rotation.
        val milestone = acceptedDay ?: profile.experienceStart.plusDays((window.day / 7 * 7).toLong()).takeIf {
            window.decision == TechnicalDecision.ROTATE_EQUIVALENT &&
                planned.o("progressEquivalentFrom").length() > 0
        } ?: return
        val previous = store.allDays().asSequence().filter {
            it.s("date") < day.toString() && it.s("progressCategory") == category.code
        }.maxByOrNull { it.s("date") } ?: return
        val previousIds = previous.a("items").objects().map { it.s("id") }.toSet()
        val currentIds = planned.a("items").objects().map { it.s("id") }.toSet()
        if (previousIds.isEmpty() || currentIds.isEmpty()) return
        val marker = category.code + "|" + profile.experienceStart + "|" + milestone
        val handled = r.o("volumeMilestones")
        if (handled.b(marker)) return
        val added = VolumeResetPolicy.changedVariants(previousIds, currentIds)
        val reset = if (acceptedDay != null) added else
            added.filter { planned.o("progressEquivalentFrom").has(it) }
        if (reset.isEmpty()) return
        val blocks = r.o("blocks")
        for (id in reset) {
            blocks.put(id, VolumeResetPolicy.block(category, id, profile.experience, day))
        }
        handled.put(marker, true)
        r.put("blocks", blocks)
        r.put("volumeMilestones", handled)
        store.state.put("monarcaPlanningV2", r)
    }
    fun blocks(): Map<String, String> = root().o("blocks").entries().mapNotNull {
        (id, value) -> (value as? String)?.let { id to it }
    }.toMap()
    fun startVolumeBlock(activity: ActivityMetadata, experience: Experience, today: LocalDate) {
        val r = root()
        val blocks = r.o("blocks")
        blocks.put(activity.id, VolumeResetPolicy.block(activity.category,
            activity.id, experience, today))
        r.put("blocks", blocks)
        write(r) // historial de bloques anteriores intacto
    }
    fun canDeclareRoutine(day: LocalDate): Boolean {
        val date = day.toString()
        val log = store.logs.optJSONObject(date) ?: return false
        return store.day(date).s("routineId").isNotBlank() && log.b("complete") &&
            log.a("items").objects().any { !it.b("archived") && MonarchLegacyHistory.completed(it) }
    }

    fun awardRoutine(day: LocalDate, explicitCompletion: Boolean): Int {
        if (!explicitCompletion || !canDeclareRoutine(day)) return 0
        return saveRoutineParticipation(day, store.day(day.toString()).s("routineId"))
    }

    fun canRunOptionalRoutine(day: LocalDate, routine: JSONObject): Boolean {
        if (day != LocalDate.now() || !routine.s("id").startsWith("monarca-routine-")) return false
        val profile = MonarchProfileAdapter.from(store) ?: return false
        if (profile.category(day) == null) return false
        val category = CategoryMapper.normalize(routine.s("focus3845")) ?: return false
        val adaptation = CategoryEffortPolicy.decide(efforts(), day, category, interruptions())
        val selectedLevel = RoutineLevel.values().getOrNull(routine.i("level", -1)) ?: return false
        val limit = minOf(selectedLevel.maxTechnical,
            if (profile.experience == Experience.BEGINNER) 2 else 3,
            if (adaptation == Adaptation.RECOVER_ONE) 1 else if (adaptation == Adaptation.REDUCE_TWO) 2 else 3)
        val items = routine.a("items").objects()
        val adjusted = store.adjusted()
        if (!store.rules.gate(adjusted).ok || items.isEmpty()) return false
        if (adaptation == Adaptation.RECOVER_ONE && items.size > 1 ||
            adaptation == Adaptation.REDUCE_TWO && items.size > 2) return false
        return items.all { item ->
            val id = item.s("id")
            val metadata = activity(id)
            val exercise = store.catalog.byId[id]
            metadata != null && exercise != null && metadata.category == category &&
                EligibilityPolicy.compatible(metadata, profile, limit) &&
                (adaptation != Adaptation.RECOVER_ONE || metadata.intensity == Intensity.LOW) &&
                store.rules.compatibleWithMission(exercise, adjusted, item)
        }
    }

    fun declareOptionalRoutine(day: LocalDate, routine: JSONObject, explicitlyCompleted: Boolean): Int {
        if (!explicitlyCompleted) return 0
        require(canRunOptionalRoutine(day, routine)) { "La rutina ya no es compatible o la fecha es de descanso" }
        return saveRoutineParticipation(day, routine.s("id"))
    }

    private fun saveRoutineParticipation(day: LocalDate, routineId: String): Int = synchronized(store) {
        require(!day.isAfter(LocalDate.now()))
        val r = root()
        val existing = r.a("routineDays").strings().mapNotNull(::date).toSet()
        val (days, points) = RoutineParticipation.award(existing, day, true)
        if (points > 0) {
            r.put("routineDays", arr(days.map { it.toString() }.sorted()))
            r.put("routineCompletions", r.o("routineCompletions").put(day.toString(), routineId))
            write(r)
        }
        points
    }
    fun routineDates(): List<LocalDate> =
        root().a("routineDays").strings().mapNotNull(::date).distinct().sortedDescending()
    fun routinePoints(): Int = routineDates().size * 10
    fun setMonarchConsent(cycleId: String, accepted: Boolean,
                          preferred: Category? = null, encounter: MonarchEncounter? = null) {
        // Los identificadores históricos de ciclos permanecen legibles;
        // solo una fecha ISO puede generar un nuevo desafío de cinco días.
        val start = runCatching { LocalDate.parse(cycleId) }.getOrNull()
        val r = root()
        if (accepted && start != null && !r.o("monarchPlans").has(cycleId)) {
            val profile = MonarchProfileAdapter.from(store) ?: error("Perfil no configurado")
            val source = MonarchActivitySource(store)
            val previousIds = r.o("monarchPlans").entries()
                .filter { it.first != cycleId }.flatMap { (_, raw) ->
                    (raw as? org.json.JSONArray)?.objects()?.flatMap { it.a("ids").strings() }
                        ?: emptyList()
                }.toSet()
            val offer = MonarchChallengePlanner.propose(start, true, profile, source,
                efforts(), (listOfNotNull(preferred) + encounter?.priorities.orEmpty()).distinct(), previousIds,
                interruptions())
            require(offer.days.any { it.ids.isNotEmpty() }) {
                "El calendario no contiene alternativas compatibles para Monarca"
            }
            val plans = r.o("monarchPlans")
            plans.put(cycleId, arr(offer.days.map { day ->
                obj("date" to day.date.toString(),
                    "category" to (day.category?.code ?: "descanso"),
                    "ids" to arr(day.ids), "profileHash" to day.profileHash,
                    "catalogHash" to day.catalogHash,
                    "explanation" to day.explanation)
            }))
            r.put("monarchPlans", plans)
            if (encounter != null)
                r.put("monarchEncounters", r.o("monarchEncounters").put(cycleId, encounter.name))
        }
        val current = r.o("monarchConsent")
        current.put(cycleId, accepted)
        r.put("monarchConsent", current)
        write(r)
    }
    fun monarchConsented(cycleId: String): Boolean = root().o("monarchConsent").b(cycleId)
    fun monarchEncounter(cycleId: String): MonarchEncounter? =
        MonarchEncounter.values().firstOrNull {
            it.name == root().o("monarchEncounters").s(cycleId)
        }

    fun currentMonarchStart(day: LocalDate): LocalDate {
        val existing = root().o("monarchPlans").entries().mapNotNull { date(it.first) }
            .filter { day in it..it.plusDays(4) || (it > day && monarchConsented(it.toString())) }
            .maxOrNull()
        if (existing != null) return existing
        val profile = MonarchProfileAdapter.from(store) ?: return day
        return (0L..6L).map(day::plusDays).firstOrNull { profile.category(it) != null } ?: day
    }

    fun monarchPlan(start: LocalDate): MonarchPlan {
        val cycleId = start.toString()
        val accepted = monarchConsented(cycleId)
        if (!accepted) return MonarchPlan(start, false, emptyList())
        val profile = MonarchProfileAdapter.from(store) ?: return MonarchPlan(start, true, emptyList())
        val source = MonarchActivitySource(store)
        val effort = efforts()
        val recentStops = interruptions()
        val days = root().o("monarchPlans").optJSONArray(cycleId)?.objects()?.mapNotNull { row ->
            val date = date(row.s("date")) ?: return@mapNotNull null
            if (date < start || !date.isBefore(start.plusDays(5))) return@mapNotNull null
            val category = CategoryMapper.normalize(row.s("category"))
            val scheduled = profile.category(date)
            val adaptation = scheduled?.let { CategoryEffortPolicy.decide(effort,
                minOf(date, LocalDate.now()), it, recentStops) }
            val recovery = adaptation == Adaptation.RECOVER_ONE
            val level = if (recovery) 1 else MonarchChallengePlanner.MAX_TECHNICAL
            val maximum = if (recovery) 1 else MonarchChallengePlanner.MAX_ACTIVITIES
            val ids = if (category == null || category != scheduled) emptyList()
                else row.a("ids").strings().distinct().filter { id ->
                    source.byId(id)?.let { item ->
                        item.category == category && EligibilityPolicy.compatible(item, profile, level) &&
                            (!recovery || item.intensity == Intensity.LOW)
                    } == true
                }.take(minOf(profile.dailyLimit, maximum))
            DailyMission(date, scheduled, row.s("profileHash"),
                row.s("catalogHash"), ids,
                row.s("explanation") + if (ids.size < row.a("ids").length())
                    " Se excluyeron alternativas por compatibilidad o recuperación." else "")
        } ?: emptyList()
        return MonarchPlan(start, accepted, days)
    }
    fun monarchCompleted(cycleId: String): List<String> =
        root().a("monarchCompletion").objects().filter { it.s("cycle") == cycleId }
            .map { it.s("date") + "|" + it.s("id") }
    fun markMonarchCompletion(cycleId: String, day: LocalDate, variantId: String): Boolean = synchronized(store) {
        require(day == LocalDate.now()) { "Solo puedes declarar la fecha local actual" }
        require(monarchConsented(cycleId)) { "Monarca requiere consentimiento explícito" }
        val start = LocalDate.parse(cycleId)
        val plan = monarchPlan(start)
        require(plan.days.any { it.date == day && variantId in it.ids }) {
            "La actividad no pertenece a un desafío vigente y compatible"
        }
        val key = day.toString() + "|" + variantId
        if (key in monarchCompleted(cycleId)) return@synchronized false
        val r = root()
        r.put("monarchCompletion", arr(r.a("monarchCompletion").objects() +
            obj("cycle" to cycleId, "date" to day.toString(), "id" to variantId)))
        write(r)
        true
    }
}

