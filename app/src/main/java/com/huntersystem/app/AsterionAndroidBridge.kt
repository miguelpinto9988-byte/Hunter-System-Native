package com.huntersystem.app

import com.huntersystem.app.asterion.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Puente de solo lectura al catálogo ACTUAL: no incorpora fichas ni inventarios nuevos. */
class AsterionActivitySource(private val store: NativeStore) : ActivityRepositoryInterface {
    override val revision: String = "native-" + store.catalog.exercises.size
    private val rows by lazy {
        val adjusted = store.adjusted()
        store.catalog.exercises.mapNotNull { e ->
            val category = CategoryMapper.normalize(e.type) ?: return@mapNotNull null
            val levelValue = when {
                e.raw.has("technicalLevel") -> e.raw.i("technicalLevel")
                e.raw.has("technicalLevel46") -> e.raw.i("technicalLevel46")
                e.meta.has("level") -> e.planningLevel + 1
                else -> 0 // sin metadatos: nivel 0 bloqueado
            }.takeIf { it in 0..5 } ?: 0
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
                    e.raw.s("impact") in listOf("Moderado", "Medio") &&
                        e.meta.has("conditioning") && e.meta.i("conditioning") <= 2 ->
                        Intensity.MODERATE
                    else -> Intensity.UNKNOWN
                }
            }
            val familyId = category.code + "/" + e.family
            val typeId = familyId + "/" + e.pattern.ifBlank { e.oldFamily }
            val eligibility = (declaredMaterials != null || e.raw.optJSONArray("materials46") != null) &&
                !e.raw.b("sourceOnly") && !e.raw.b("pendingTechnicalReview") &&
                !(e.raw.b("sourceExpansion2k") && !e.raw.b("approvedForAutoplan")) &&
                store.rules.compatible(e, adjusted)
            ActivityMetadata(e.id, category, familyId, typeId, levelValue, intensity, allowedGear,
                e.raw.s("modality", "no_especificada"),
                e.raw.b("requiresSupervision") || e.raw.b("supervisionRequired") ||
                    e.raw.b("highSkill46"),
                eligibility, buildSet {
                    addAll(e.raw.a("sportTags").strings())
                    addAll(e.raw.a("goalTags").strings())
                    addAll(e.raw.a("tags").strings())
                    if (e.pattern.isNotBlank()) add(e.pattern)
                }, e.raw.a("restrictionTags").strings().toSet())
        }
    }
    override fun all(): List<ActivityMetadata> = rows
}

/** Traduce los índices históricos domingo=0 a lunes=0, sin reescribir perfiles guardados. */
object AsterionProfileAdapter {
    fun from(store: NativeStore): TrainingProfile? {
        val p = store.profile ?: return null
        val experience = when (p.s("difficulty")) {
            "advanced" -> Experience.ADVANCED
            "intermediate" -> Experience.INTERMEDIATE
            else -> Experience.BEGINNER
        }
        // Los formatos `weekCategories` (texto) y `weeklyCategories` (0..5)
        // usan lunes=0; el calendario legacy `weekPlan` usa domingo=0.
        val numericWeek = p.optJSONArray("weeklyCategories")
        val textualWeek = p.optJSONArray("weekCategories")
        val explicitWeek: List<Int>? = when {
            numericWeek?.length() == 7 -> numericWeek.ints().takeIf { it.all { n -> n in 0..5 } }
            textualWeek?.length() == 7 -> textualWeek.strings().map {
                if (it in listOf("descanso", "rest")) 5
                else CategoryMapper.normalize(it)?.ordinal ?: 5
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
        val savedStart = store.state.o("asterionV1").s("experienceSince")
        val since = runCatching { LocalDate.parse(savedStart) }.getOrNull()
            ?: runCatching { LocalDate.parse(p.s("experienceStartedAt")) }.getOrNull()
            ?: LocalDate.now()
        val declared = buildSet {
            add("body")
            val gear = p.s("equipment", "body")
            if (gear == "db" || gear == "gym") add("db")
            if (gear == "gym") add("gym")
            addAll(p.a("materials46").strings())
            addAll(p.a("gymEquipment23").strings())
            if (p.b("bench")) add("bench")
            if (p.b("step")) add("step")
        }
        val restrictions = p.o("restrictions").entries().mapNotNull {
            if (it.second == true) it.first else null
        }.toSet()
        return runCatching {
            TrainingProfile(experience, since, week, declared, restrictions,
                p.s("intensityPreference") == "low",
                p.i("dailyMissions", 3).coerceIn(1, 7),
                p.i("minutes", 20).coerceAtLeast(1),
                p.s("custom_goal", p.s("finalGoal")), p.s("goal", "general"),
                if (p.s("trainingMode") == "routines") "rutinas" else "misiones",
                p.i("restSeconds", 60), p.s("blockMode22") == "rounds")
        }.getOrNull()
    }
}

/**
 * Datos aislados bajo asterionV1, dentro del AtomicFile existente: conservan el
 * backup histórico, y la lectura nunca convierte RPE global antiguo en RPE por categoría.
 */
class AsterionProgressRepository(private val store: NativeStore) : VolumeProfileInterface {
    /** Solo lee referencias ya validadas y entregadas por una integración externa. */
    override fun find(variantId: String): VolumeProfile? {
        val exercise = store.catalog.byId[variantId] ?: return null
        // Fuente primaria: metadatos de la misma variante en el catálogo externo.
        // Fuente importada: exige identidad y revisión del proveedor verificables.
        val catalogProfile = exercise.raw.optJSONObject("volumeProfile")
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
            require(unit == (if (exercise.unit == "sec") VolumeUnit.SECONDS
                             else VolumeUnit.REPETITIONS))
            VolumeProfile(variantId, unit,
                record.i("initialSets").takeIf { record.has("initialSets") },
                record.i("initialValue").takeIf { record.has("initialValue") },
                record.i("minimum"), record.i("maximum"),
                record.i("permittedIncrement"), record.b("automaticReviewEligible"))
        }.getOrNull()
    }
    private fun root(): JSONObject = store.state.o("asterionV1")
    private fun write(root: JSONObject) {
        store.state.put("asterionV1", root)
        store.save()
    }
    private fun date(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()
    private fun explicitEfforts(): List<EffortObservation> =
        root().a("efforts").objects().mapNotNull { row ->
            val day = date(row.s("date")) ?: return@mapNotNull null
            val category = CategoryMapper.normalize(row.s("category"))
            val rpe = row.i("rpe")
            if (rpe !in 1..10) return@mapNotNull null
            EffortObservation(day, category, rpe, row.b("completed"), row.b("interrupted"))
        }
    fun efforts(): List<EffortObservation> =
        AsterionLegacyMigration.mergeEffort(
            AsterionLegacyMigration.efforts(store.logs), explicitEfforts())
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
        AsterionLegacyMigration.mergeParticipation(
            AsterionLegacyMigration.participation(store.logs, store.catalog),
            explicitParticipation())
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
            row.s("profileHash"), row.s("catalogHash"), ids, row.s("explanation"))
    }
    fun pin(mission: DailyMission) {
        val r = root()
        val missions = r.a("missions").objects().filterNot { it.s("date") == mission.date.toString() }
        r.put("missions", arr((missions + obj("date" to mission.date.toString(),
            "category" to (mission.category?.code ?: "descanso"),
            "profileHash" to mission.profileHash, "catalogHash" to mission.catalogHash,
            "ids" to arr(mission.ids), "explanation" to mission.explanation)).takeLast(40)))
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
        val r = root()
        val entries = r.o("phases")
        entries.put(category.code, obj("phase" to checkpoint.phase.phase,
            "lastChange" to (checkpoint.phase.lastChange?.toString() ?: "")))
        r.put("phases", entries)
        write(r)
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
    fun recordVolume(observation: VolumeObservation) {
        require(!observation.date.isAfter(LocalDate.now()))
        require(observation.completed) { "Volumen reservado a actividad completada explícitamente" }
        val r = root()
        val all = volume().filterNot { it.id == observation.id } + observation
        r.put("volume", arr(all.map {
            obj("id" to it.id, "date" to it.date.toString(),
                "category" to it.category.code, "variantId" to it.variantId,
                "typeId" to it.typeId, "technicalLevel" to it.technicalLevel,
                "unit" to it.unit.name, "sets" to it.sets, "quantity" to it.quantityPerSet,
                "rpe" to it.rpe, "completed" to it.completed, "interrupted" to it.interrupted,
                "blockId" to it.blockId)
        }))
        write(r)
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
    fun awardRoutine(day: LocalDate, explicitCompletion: Boolean): Int {
        require(!day.isAfter(LocalDate.now()))
        val r = root()
        val existing = r.a("routineDays").strings().mapNotNull(::date).toSet()
        val (days, points) = RoutineParticipation.award(existing, day, explicitCompletion)
        if (points > 0) {
            r.put("routineDays", arr(days.map { it.toString() }.sorted()))
            write(r)
        }
        return points
    }
    fun routinePoints(): Int = root().a("routineDays").length() * 10
    fun setMonarchConsent(cycleId: String, accepted: Boolean) {
        require(cycleId.isNotBlank())
        val r = root()
        val current = r.o("monarchConsent")
        current.put(cycleId, accepted)
        r.put("monarchConsent", current)
        write(r)
    }
    fun monarchConsented(cycleId: String): Boolean = root().o("monarchConsent").b(cycleId)
    fun monarchCompleted(cycleId: String): List<String> =
        root().a("monarchCompletion").objects().filter { it.s("cycle") == cycleId }
            .map { it.s("date") + "|" + it.s("id") }
    fun markMonarchCompletion(cycleId: String, day: LocalDate, variantId: String): Boolean {
        require(day == LocalDate.now()) { "Solo puedes declarar la fecha local actual" }
        require(monarchConsented(cycleId)) { "Monarca requiere consentimiento explícito" }
        val start = LocalDate.parse(cycleId)
        val profile = AsterionProfileAdapter.from(store) ?: error("Perfil no configurado")
        val plan = MonarchChallengePlanner.propose(start, true, profile,
            AsterionActivitySource(store), efforts())
        require(plan.days.any { it.date == day && variantId in it.ids }) {
            "La actividad no pertenece a un desafío vigente y compatible"
        }
        val key = day.toString() + "|" + variantId
        if (key in monarchCompleted(cycleId)) return false
        val r = root()
        r.put("monarchCompletion", arr(r.a("monarchCompletion").objects() +
            obj("cycle" to cycleId, "date" to day.toString(), "id" to variantId)))
        write(r)
        return true
    }
}

/** Consulta y fijación explícita: reabrir la pantalla reutiliza la misma misión. */
class AsterionTrainingSessionCoordinator(private val store: NativeStore) {
    val progress = AsterionProgressRepository(store)
    fun current(date: LocalDate = LocalDate.now()): CoordinatedResult? {
        val profile = AsterionProfileAdapter.from(store) ?: return null
        val source = AsterionActivitySource(store)
        val result = CoordinatedTrainingEngine.evaluate(date, profile, source,
            progress.efforts(), progress.phases(), progress,
            progress.volume(), progress.blocks(), progress.pinned(date))
        if (progress.pinned(date) != result.mission && date == LocalDate.now())
            progress.pin(result.mission)
        return result
    }
}
