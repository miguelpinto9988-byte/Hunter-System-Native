package com.huntersystem.app.planning

import java.security.MessageDigest
import java.text.Normalizer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.min
import kotlin.math.roundToInt

/** Núcleo local: ninguna regla crea contenido de catálogo ni modifica registros por consultar. */
enum class Category(val code: String) {
    UPPER("tren_superior"), CENTRAL("tren_central"), LOWER("tren_inferior"),
    MOBILITY("movilidad"), CARDIO("cardio")
}
object CategoryMapper {
    val codes = Category.values().map { it.code }
    fun normalize(value: String): Category? {
        val code = when (value.lowercase().trim()) {
            "upper", "tren_superior" -> "tren_superior"
            "core", "tronco", "integrado", "tren_central" -> "tren_central"
            "lower", "piernas", "tren_inferior" -> "tren_inferior"
            "mobility", "coordinacion", "movilidad" -> "movilidad"
            "cardio" -> "cardio"
            else -> value.lowercase().trim()
        }
        return Category.values().firstOrNull { it.code == code }
    }
    fun day(code: Int): Category? {
        require(code in 0..5) { "Índice de calendario inválido" }
        return Category.values().getOrNull(code) // 5 = descanso, sin categoría
    }
}
enum class Experience { BEGINNER, INTERMEDIATE, ADVANCED }
enum class Intensity { LOW, MODERATE, HIGH, UNKNOWN }
enum class VolumeUnit { REPETITIONS, SECONDS }
enum class ParticipationStatus { PENDING, COMPLETED, SKIPPED }
enum class Adaptation { MAINTAIN, EQUIVALENT_VARIATION, REDUCE_TWO, RECOVER_ONE }
enum class TechnicalDecision { WAIT, MAINTAIN, SIMPLIFY, ADVANCE_PHASE, ROTATE_EQUIVALENT }
enum class VolumeDecision {
    NO_HISTORY, MAINTAIN, OPTIONAL_INCREASE, REDUCE, REVIEW_EXTRA_SET,
    GUIDED_REVIEW, RECOVERY_PRIORITY, PAUSED, OPTIONAL_MINIMUM_ADJUSTMENT
}
enum class RoutineLevel(val maxTechnical: Int) {
    BASIC(1), INTERMEDIATE(2), ADVANCED(3)
}

/** IDs de familia y tipo son externos; no se asignan movimientos concretos aquí. */
data class ActivityMetadata(
    val id: String, val category: Category, val familyId: String, val typeId: String,
    val technicalLevel: Int, val intensity: Intensity, val equipment: Set<String>,
    val modality: String, val requiresSupervision: Boolean, val automaticEligible: Boolean,
    val tags: Set<String> = emptySet(), val restrictionTags: Set<String> = emptySet(),
    val sport: String = ""
) {
    init {
        require(id.isNotBlank() && familyId.isNotBlank() && typeId.isNotBlank())
        require(technicalLevel in 0..5)
        require(equipment.none { it.isBlank() })
    }
}
interface ActivityRepositoryInterface {
    /** La revisión cambia cuando cambia cualquier metadato de elegibilidad o relación. */
    val revision: String
    fun all(): List<ActivityMetadata>
    fun verified(): List<ActivityMetadata> = all().also(ActivityCatalogValidator::verify)
    fun signature(): String = ProfileSignature.source(all()) + ":" + revision
    fun byId(id: String): ActivityMetadata? = all().firstOrNull { it.id == id }
    fun categories(): List<Category> = Category.values().toList()
    fun families(category: Category): List<String> =
        all().filter { it.category == category }.map { it.familyId }.distinct().sorted()
    fun types(category: Category, familyId: String): List<String> =
        all().filter { it.category == category && it.familyId == familyId }
            .map { it.typeId }.distinct().sorted()
    fun variants(category: Category, familyId: String? = null, typeId: String? = null): List<ActivityMetadata> =
        all().filter { it.category == category &&
            (familyId == null || it.familyId == familyId) &&
            (typeId == null || it.typeId == typeId) }
}
object ActivityCatalogValidator {
    fun verify(rows: List<ActivityMetadata>) {
        require(rows.map { it.id }.distinct().size == rows.size) { "IDs de variante duplicados" }
        require(rows.all { it.familyId.isNotBlank() && it.typeId.isNotBlank() })
        require(rows.groupBy { it.typeId }.values.all { items ->
            items.map { it.category to it.familyId }.distinct().size == 1
        }) { "Tipo vinculado a categorías o familias incompatibles" }
        require(rows.groupBy { it.familyId }.values.all { items ->
            items.map { it.category }.distinct().size == 1
        }) { "Familia vinculada a más de una categoría" }
    }
}

data class TrainingProfile(
    val experience: Experience,
    val experienceStart: LocalDate,
    val weekly: List<Int>, // lunes a domingo, 5 = descanso
    val equipment: Set<String> = emptySet(),
    val restrictions: Set<String> = emptySet(),
    val lowIntensityOnly: Boolean = false,
    val dailyLimit: Int = 3,
    val minutes: Int = 20,
    val customGoal: String = "",
    val primaryGoal: String = "",
    val mode: String = "misiones",
    val restSeconds: Int = 60,
    val rounds: Boolean = false
) {
    init {
        require(weekly.size == 7 && weekly.all { it in 0..5 })
        require(dailyLimit in 1..7)
        require(minutes > 0)
        require(equipment.none { it.isBlank() })
    }
    fun category(date: LocalDate): Category? = CategoryMapper.day(weekly[date.dayOfWeek.value - 1])
}
object EligibilityPolicy {
    fun reason(item: ActivityMetadata, profile: TrainingProfile, maxLevel: Int = 3): String? = when {
        !item.automaticEligible -> "No elegible automáticamente"
        item.requiresSupervision -> "Requiere supervisión"
        item.technicalLevel == 0 || item.technicalLevel > min(3, maxLevel) ->
            "Complejidad técnica no permitida"
        item.intensity == Intensity.HIGH || item.intensity == Intensity.UNKNOWN ->
            "Intensidad fuera del filtro conservador"
        profile.lowIntensityOnly && item.intensity != Intensity.LOW -> "Preferencia de intensidad baja"
        !profile.equipment.containsAll(item.equipment) -> "Equipamiento no declarado"
        item.restrictionTags.any { it in profile.restrictions } -> "Restricción del perfil"
        else -> null
    }
    fun compatible(item: ActivityMetadata, profile: TrainingProfile, maxLevel: Int = 3): Boolean =
        reason(item, profile, maxLevel) == null
}
data class GoalInterpretation(val text: String, val tags: Set<String>, val recognized: Boolean)
object PersonalGoalInterpreter {
    private val dictionary = linkedMapOf(
        "caminar" to listOf("caminar", "caminata", "andar"),
        "bicicleta" to listOf("bicicleta", "ciclismo", "pedalear"),
        "carrera" to listOf("correr", "carrera", "trotar"),
        "danza" to listOf("danza", "bailar", "baile"),
        "equilibrio" to listOf("equilibrio", "balance"),
        "movilidad" to listOf("movilidad", "movilizar"),
        "flexibilidad" to listOf("flexibilidad", "estirar"),
        "postura" to listOf("postura", "postural"),
        "fuerza_funcional" to listOf("fuerza funcional", "fortalecer", "fuerza"),
        "coordinacion" to listOf("coordinacion", "coordinar"),
        "resistencia" to listOf("resistencia", "aguante")
    )
    private fun simplify(value: String): String =
        Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
    fun interpret(text: String): GoalInterpretation {
        val normalized = simplify(text)
        val segments = normalized.split(Regex("[.;,]|\\bpero\\b|\\baunque\\b"))
        val positive = segments.map { part ->
            // Una negación cubre la intención del segmento, no produce una afinidad.
            val denied = Regex("\\b(no quiero|no deseo|evitar|evito|sin interes en|no puedo|no busco)\\b")
            if (denied.containsMatchIn(part)) "" else part
        }.joinToString(" ")
        val tags = dictionary.filterValues { words ->
            words.any { word -> Regex("\\b" + Regex.escape(word) + "\\b").containsMatchIn(positive) }
        }.keys
        return GoalInterpretation(text, tags.toSet(), tags.isNotEmpty())
    }
}
object ProfileSignature {
    private fun hash(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    fun of(profile: TrainingProfile): String = hash(listOf(
        profile.experience.name, profile.experienceStart.toString(),
        profile.weekly.joinToString(","), profile.equipment.sorted().joinToString(","),
        profile.restrictions.sorted().joinToString(","), profile.lowIntensityOnly.toString(),
        profile.dailyLimit.toString(), profile.minutes.toString(), profile.customGoal,
        profile.primaryGoal, profile.mode, profile.restSeconds.toString(), profile.rounds.toString()
    ).joinToString("\u001f"))
    fun source(rows: List<ActivityMetadata>): String = hash(rows.sortedBy { it.id }.joinToString("\u001f") {
        listOf(it.id, it.category.code, it.familyId, it.typeId, it.technicalLevel,
            it.intensity, it.equipment.sorted(), it.modality, it.requiresSupervision,
            it.automaticEligible, it.tags.sorted(), it.restrictionTags.sorted(), it.sport).joinToString("|")
    })
    fun stable(seed: String, id: String): String = hash(seed + "|" + id)
}
data class Participation(
    val date: LocalDate, val variantId: String, val category: Category,
    val status: ParticipationStatus, val notes: String = "", val interrupted: Boolean = false
)
data class EffortObservation(
    val date: LocalDate, val category: Category?, val rpe: Int, val completed: Boolean,
    val interrupted: Boolean = false
) {
    init { require(rpe in 1..10) }
}
data class ProgressSummary(val planned: Int, val completed: Int, val skipped: Int, val pending: Int) {
    val evaluated: Int get() = completed + skipped
    val percent: Int? get() = if (evaluated == 0) null
        else (100.0 * completed / evaluated).roundToInt()
    val label: String get() = percent?.toString()?.plus("%") ?: "Sin datos suficientes"
    companion object {
        fun calculate(records: List<Participation>): ProgressSummary =
            ProgressSummary(records.size, records.count { it.status == ParticipationStatus.COMPLETED },
                records.count { it.status == ParticipationStatus.SKIPPED },
                records.count { it.status == ParticipationStatus.PENDING })
    }
}
object CategoryEffortPolicy {
    fun latest(history: List<EffortObservation>, date: LocalDate, category: Category): EffortObservation? =
        history.filter { it.category == category && it.date < date &&
            !it.date.isBefore(date.minusDays(21)) && (it.completed || it.interrupted || it.rpe >= 8) }
            .maxWithOrNull(compareBy<EffortObservation> { it.date }.thenBy { it.rpe })
    fun decide(history: List<EffortObservation>, date: LocalDate, category: Category): Adaptation {
        val last = latest(history, date, category) ?: return Adaptation.MAINTAIN
        val recent = history.filter { it.category == category && it.date < date &&
            !it.date.isBefore(date.minusDays(7)) }
        // Señales de recuperación recientes prevalecen sobre una valoración
        // baja posterior; las valoraciones de otras categorías no intervienen.
        return when {
            recent.any { it.interrupted || it.rpe >= 9 } -> Adaptation.RECOVER_ONE
            recent.any { it.rpe == 8 } -> Adaptation.REDUCE_TWO
            last.interrupted || last.rpe >= 9 -> Adaptation.RECOVER_ONE
            last.rpe == 8 -> Adaptation.REDUCE_TWO
            last.completed && last.rpe in 1..3 -> Adaptation.EQUIVALENT_VARIATION
            else -> Adaptation.MAINTAIN
        }
    }
}
enum class WeeklyDecision { WAIT, INSUFFICIENT, MAINTAIN, EQUIVALENT_VARIATION, RECOVERY }
object WeeklyCategoryChallengePolicy {
    fun review(history: List<EffortObservation>, date: LocalDate, category: Category): WeeklyDecision {
        val recent = history.filter { it.category == category && it.date < date &&
            !it.date.isBefore(date.minusDays(7)) }
        // La recuperación tiene prioridad incluso antes del primer hito semanal.
        if (recent.any { it.rpe >= 8 || it.interrupted }) return WeeklyDecision.RECOVERY
        val first = history.filter { it.category != null }.minOfOrNull { it.date }
            ?: return WeeklyDecision.WAIT
        if (ChronoUnit.DAYS.between(first, date) < 7) return WeeklyDecision.WAIT
        val completedLowDays = recent.filter { it.completed && !it.interrupted && it.rpe in 1..3 }
            .map { it.date }.distinct()
        if (completedLowDays.size >= 2) return WeeklyDecision.EQUIVALENT_VARIATION
        if (recent.count { it.completed && !it.interrupted } < 2) return WeeklyDecision.INSUFFICIENT
        return WeeklyDecision.MAINTAIN
    }
}
data class TechnicalPhase(val phase: Int = 0, val lastChange: LocalDate? = null) {
    init { require(phase in 0..2) }
}
data class TechnicalCheckpoint(val decision: TechnicalDecision, val phase: TechnicalPhase, val day: Long)
object TechnicalCheckpointPolicy {
    fun review(profile: TrainingProfile, category: Category, date: LocalDate,
               history: List<EffortObservation>, state: TechnicalPhase): TechnicalCheckpoint {
        val day = ChronoUnit.DAYS.between(profile.experienceStart, date)
        if (day < 7 || (day != 7L && day != 14L && day != 21L))
            return TechnicalCheckpoint(TechnicalDecision.WAIT, state, day)
        val weekly = WeeklyCategoryChallengePolicy.review(history, date, category)
        if (weekly == WeeklyDecision.RECOVERY) return TechnicalCheckpoint(TechnicalDecision.SIMPLIFY, state, day)
        if (weekly != WeeklyDecision.EQUIVALENT_VARIATION)
            return TechnicalCheckpoint(TechnicalDecision.MAINTAIN, state, day)
        if (state.phase >= 2 || profile.experience == Experience.BEGINNER)
            return TechnicalCheckpoint(TechnicalDecision.ROTATE_EQUIVALENT, state, day)
        if (state.lastChange != null && ChronoUnit.DAYS.between(state.lastChange, date) < 7)
            return TechnicalCheckpoint(TechnicalDecision.MAINTAIN, state, day)
        return TechnicalCheckpoint(TechnicalDecision.ADVANCE_PHASE,
            TechnicalPhase(state.phase + 1, date), day)
    }
}
object MissionDifficultyMixPolicy {
    fun levelThreeTarget(experience: Experience, phase: Int): Int = when (experience) {
        Experience.BEGINNER -> 0
        Experience.INTERMEDIATE -> listOf(25, 40, 60)[phase.coerceIn(0, 2)]
        Experience.ADVANCED -> listOf(33, 55, 75)[phase.coerceIn(0, 2)]
    }
}
data class DailyMission(
    val date: LocalDate, val category: Category?, val profileHash: String,
    val catalogHash: String, val ids: List<String>, val explanation: String
)
object DeterministicSelection {
    fun rankScore(item: ActivityMetadata, selected: List<ActivityMetadata>,
                  seed: String, baseLevel: Int, priority: Double): Double {
        val rotation = (seed + "|" + selected.size + "|" + item.id).hashCode()
        // Mix the string hash before using it so adjacent catalog IDs get different weights.
        val mixed = (rotation xor (rotation ushr 16)) * 0x45d9f3b
        val variety = Math.floorMod(mixed xor (mixed ushr 16), 25)
        val sportPenalty = if (item.sport.isBlank()) 0 else selected.count { it.sport == item.sport } * 14
        val patternPenalty = selected.count { it.typeId.substringAfterLast('/') ==
            item.typeId.substringAfterLast('/') } * 18
        return priority + variety - sportPenalty - patternPenalty -
            (baseLevel - item.technicalLevel).coerceAtLeast(0) * 12
    }

    fun select(rows: List<ActivityMetadata>, count: Int, seed: String,
               goalTags: Set<String>, experience: Experience, phase: Int,
               historicalPriority: Map<String, Int> = emptyMap(),
               baseLevel: Int = experience.ordinal + 1,
               stretchSlots: Int = if (phase > 0) 1 else 0): List<ActivityMetadata> {
        val selected = mutableListOf<ActivityMetadata>()
        val usedFamilies = mutableSetOf<String>()
        var stretches = 0
        while (selected.size < count) {
            val next = rows.asSequence().filter {
                it.familyId !in usedFamilies &&
                    (it.technicalLevel <= baseLevel ||
                        it.technicalLevel == baseLevel + 1 && selected.isNotEmpty() && stretches < stretchSlots)
            }.maxWithOrNull(compareBy<ActivityMetadata> {
                val priority = (historicalPriority[it.id] ?: 0) +
                    (if (it.tags.any { tag -> tag in goalTags }) 55 else 0) +
                    (if (it.technicalLevel > baseLevel && selected.isNotEmpty()) 12 else 0)
                rankScore(it, selected, seed, baseLevel, priority.toDouble())
            }.thenBy { it.id }) ?: break
            selected += next
            usedFamilies += next.familyId
            if (next.technicalLevel > baseLevel) stretches++
        }
        return selected
    }
}
object DailyMissionPlanner {
    fun plan(date: LocalDate, profile: TrainingProfile, repository: ActivityRepositoryInterface,
             efforts: List<EffortObservation>, phases: Map<Category, TechnicalPhase> = emptyMap(),
             pinned: DailyMission? = null,
             historicalPriority: Map<String, Int> = emptyMap(),
             recentWeekIds: Set<String> = emptySet(),
             variation: String = "", progressionReady: Boolean = false): DailyMission {
        val category = profile.category(date)
        val profileHash = ProfileSignature.of(profile)
        val rows = repository.verified()
        val catalogHash = repository.signature()
        val previous = pinned?.takeIf {
            it.date == date && it.category == category && it.profileHash == profileHash &&
                it.ids.distinct().size == it.ids.size &&
                it.ids.all { id -> repository.byId(id)?.let { a ->
                    category == a.category && EligibilityPolicy.compatible(a, profile,
                        min(3, profile.experience.ordinal + 2))
                } == true }
        }
        if (previous != null) return previous.copy(catalogHash = catalogHash)
        if (category == null)
            return DailyMission(date, null, profileHash, catalogHash, emptyList(), "Descanso programado.")
        val adaptation = CategoryEffortPolicy.decide(efforts, date, category)
        val limit = when (adaptation) {
            Adaptation.REDUCE_TWO -> min(profile.dailyLimit, 2)
            Adaptation.RECOVER_ONE -> 1
            else -> profile.dailyLimit
        }
        val maxLevel = when {
            adaptation == Adaptation.RECOVER_ONE -> 1
            adaptation == Adaptation.REDUCE_TWO -> 2
            profile.experience == Experience.BEGINNER -> 2
            else -> 3
        }
        val candidates = rows.filter { it.category == category &&
            EligibilityPolicy.compatible(it, profile, maxLevel) &&
            (adaptation != Adaptation.RECOVER_ONE || it.technicalLevel == 1) }
        val goal = PersonalGoalInterpreter.interpret(profile.customGoal)
        val weekly = WeeklyCategoryChallengePolicy.review(efforts, date, category)
        val phase = phases[category]?.phase ?: 0
        val priority = candidates.associate {
            it.id to ((historicalPriority[it.id] ?: 0) - if (it.id in recentWeekIds) 250 else 0)
        }
        val selected = DeterministicSelection.select(candidates, limit,
            profileHash + catalogHash + date + category.code + variation + phase, goal.tags,
            profile.experience, phase, priority,
            baseLevel = min(maxLevel, profile.experience.ordinal + 1),
            stretchSlots = if (progressionReady && adaptation in setOf(Adaptation.MAINTAIN, Adaptation.EQUIVALENT_VARIATION)) 1 else 0)
        val explanation = when {
            selected.isEmpty() -> "No existen alternativas compatibles con el perfil y las restricciones."
            adaptation == Adaptation.RECOVER_ONE -> "Esfuerzo reciente 9–10: recuperación y máximo una alternativa sencilla; descansar sigue disponible."
            adaptation == Adaptation.REDUCE_TWO -> "Esfuerzo reciente 8: máximo dos alternativas de baja complejidad."
            selected.any { it.technicalLevel > profile.experience.ordinal + 1 } ->
                "La revisión del grupo permite una variante técnica con dosis inicial reducida."
            weekly == WeeklyDecision.EQUIVALENT_VARIATION ->
                "Evidencia de dos sesiones completadas y valoradas: variedad equivalente opcional."
            goal.recognized -> "Selección compatible priorizada por objetivo escrito reconocido."
            else -> "Selección conservadora; sin evidencia suficiente para aumentar exigencia."
        }
        return DailyMission(date, category, profileHash, catalogHash, selected.map { it.id }, explanation)
    }
}
data class RoutineRequest(val category: Category, val duration: Int, val level: RoutineLevel,
                          val date: LocalDate, val variation: String) {
    init { require(duration in setOf(10, 15, 20, 30, 45, 60)) }
}
data class RoutineProposal(val ids: List<String>, val duration: Int, val explanation: String)
object RoutineGenerator {
    fun generate(request: RoutineRequest, profile: TrainingProfile,
                 repository: ActivityRepositoryInterface, maxProposals: Int = 6): List<RoutineProposal> {
        require(maxProposals in 0..6)
        val count = when {
            request.duration <= 15 -> 3
            request.duration <= 20 -> 4
            request.duration <= 30 -> 5
            else -> 6
        }
        val pool = repository.variants(request.category).filter {
            EligibilityPolicy.compatible(it, profile, request.level.maxTechnical)
        }
        val goal = PersonalGoalInterpreter.interpret(profile.customGoal).tags
        val distinct = linkedMapOf<String, RoutineProposal>()
        for (index in 0 until maxProposals) {
            val picked = DeterministicSelection.select(pool, count,
                request.date.toString() + request.variation + index + ProfileSignature.of(profile),
                goal, profile.experience, 0, baseLevel = request.level.maxTechnical)
            if (picked.isEmpty()) break
            val ids = picked.map { it.id }
            distinct.putIfAbsent(ids.joinToString("|"),
                RoutineProposal(ids, request.duration, "Propuesta opcional con filtros técnicos y materiales."))
        }
        return distinct.values.toList()
    }
}
data class MonarchPlan(val start: LocalDate, val accepted: Boolean, val days: List<DailyMission>)
object MonarchChallengePlanner {
    fun propose(start: LocalDate, accepted: Boolean, profile: TrainingProfile,
                repository: ActivityRepositoryInterface, efforts: List<EffortObservation>,
                preferred: List<Category> = emptyList(),
                previousUsed: Set<String> = emptySet(),
                dailyMission: ((LocalDate) -> DailyMission)? = null): MonarchPlan {
        if (!accepted || profile.weekly.all { it == 5 }) return MonarchPlan(start, accepted, emptyList())
        val days = (0L..4L).map { start.plusDays(it) }.map { day ->
            val normal = dailyMission?.invoke(day)
                ?: DailyMissionPlanner.plan(day, profile, repository, efforts)
            if (normal.category == null || normal.ids.isEmpty()) normal else {
                // Un jefe solo puede ordenar categorías, jamás suprimir elegibilidad.
                // El enfoque y el historial solo modifican orden y diversidad,
                // jamás la categoría del día, el filtro técnico ni el equipamiento.
                val ids = normal.ids.sortedWith(compareBy<String> {
                    if (it in previousUsed) 1 else 0
                }.thenBy { ProfileSignature.stable(
                    if (normal.category in preferred) "monarca-enfoque|" + start
                    else "monarca-estable|" + start, it
                ) })
                normal.copy(ids = ids, explanation = normal.explanation +
                    if (normal.category in preferred) " Categoría de enfoque Monarca." else "")
            }
        }
        return MonarchPlan(start, true, days)
    }
}
object EffortReviewEngine {
    data class Review(val cycleDay: Int, val previous: Int, val next: Int,
                      val observations: Int, val average: Double?, val explanation: String)
    fun review(start: LocalDate, date: LocalDate, history: List<EffortObservation>): Review {
        val elapsed = ChronoUnit.DAYS.between(start, date).coerceAtLeast(0).toInt()
        val cycleDay = (elapsed % 21) + 1
        val previous = ((cycleDay - 1) / 7) * 7
        val next = min(21, previous + 7)
        val from = date.minusDays(20).coerceAtLeast(start.plusDays((elapsed / 21 * 21).toLong()))
        val samples = history.filter { !it.date.isBefore(from) && !it.date.isAfter(date) }
        val average = samples.map { it.rpe }.takeIf { it.isNotEmpty() }?.average()
        return Review(cycleDay, previous, next, samples.size, average,
            if (average == null) "Sin datos suficientes para determinar una tendencia."
            else "Media descriptiva de valoraciones reales; no aumenta automáticamente el entrenamiento.")
    }
}
