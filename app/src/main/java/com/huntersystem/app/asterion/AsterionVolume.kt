package com.huntersystem.app.asterion

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.min

/** Únicamente valores proporcionados por una fuente revisada; nunca hay dosis predeterminadas ficticias. */
data class VolumeProfile(
    val variantId: String, val unit: VolumeUnit, val initialSets: Int?,
    val initialValue: Int?, val minimum: Int, val maximum: Int,
    val permittedIncrement: Int, val automaticReviewEligible: Boolean
) {
    init {
        require(variantId.isNotBlank() && minimum > 0 && maximum >= minimum)
        require(permittedIncrement > 0)
        require(initialSets == null || initialSets in 1..3)
        require(initialValue == null || initialValue in minimum..maximum)
    }
}
interface VolumeProfileInterface {
    fun find(variantId: String): VolumeProfile?
}
data class VolumeObservation(
    val id: String, val date: LocalDate, val category: Category, val variantId: String,
    val typeId: String, val technicalLevel: Int, val unit: VolumeUnit,
    val sets: Int, val quantityPerSet: Int, val rpe: Int?,
    val completed: Boolean, val interrupted: Boolean, val blockId: String
) {
    init {
        require(id.isNotBlank() && variantId.isNotBlank() && typeId.isNotBlank() && blockId.isNotBlank())
        require(technicalLevel in 1..5 && sets in 1..30 && quantityPerSet > 0)
        require(rpe == null || rpe in 1..10)
    }
}
data class VolumeRecommendation(
    val decision: VolumeDecision, val reference: Int?, val sets: Int?,
    val unit: VolumeUnit?, val optional: Boolean, val explanation: String
)
object VolumeResetPolicy {
    fun block(category: Category, variantId: String, experience: Experience, since: LocalDate): String =
        ProfileSignature.stable("volume|" + category.code + "|" + experience.name +
            "|" + since, variantId)
}
object VolumeProgressionEngine {
    fun review(today: LocalDate, activity: ActivityMetadata, profile: VolumeProfile?,
               history: List<VolumeObservation>, blockId: String,
               categoryHistory: List<EffortObservation> = emptyList()): VolumeRecommendation {
        if (profile == null || profile.variantId != activity.id)
            return VolumeRecommendation(VolumeDecision.GUIDED_REVIEW, null, null, null,
                false, "La variante carece de parámetros numéricos validados.")
        val matches = history.filter {
            it.date < today && !it.date.isBefore(today.minusDays(21)) &&
                it.category == activity.category && it.variantId == activity.id &&
                it.typeId == activity.typeId && it.technicalLevel == activity.technicalLevel &&
                it.unit == profile.unit && it.blockId == blockId
        }.sortedWith(compareBy<VolumeObservation> { it.date }.thenBy { it.id })
        val latest = matches.lastOrNull()
        val valid = matches.filter { it.completed && !it.interrupted && it.rpe != null }
        val reference = valid.lastOrNull()?.quantityPerSet ?: profile.initialValue
        val sets = valid.lastOrNull()?.sets ?: profile.initialSets
        if (!profile.automaticReviewEligible)
            return VolumeRecommendation(VolumeDecision.GUIDED_REVIEW, reference, sets,
                profile.unit, false, "Revisión manual obligatoria; referencia sin cambios.")
        val highCategory = categoryHistory.any {
            it.category == activity.category && it.date < today &&
                !it.date.isBefore(today.minusDays(21)) && (it.rpe >= 8 || it.interrupted)
        }
        if (latest?.interrupted == true || (latest != null && !latest.completed))
            return VolumeRecommendation(VolumeDecision.PAUSED, reference, sets,
                profile.unit, false, "Sesión interrumpida o no completada: sin aumentos.")
        if (highCategory || latest?.rpe?.let { it >= 9 } == true)
            return VolumeRecommendation(VolumeDecision.RECOVERY_PRIORITY, reference, sets,
                profile.unit, false, "Señal reciente de esfuerzo elevado: prioridad de recuperación.")
        if (reference == null)
            return VolumeRecommendation(VolumeDecision.NO_HISTORY, null, sets,
                profile.unit, false, "Sin historial comparable ni referencia inicial validada.")
        if (valid.isEmpty())
            return VolumeRecommendation(VolumeDecision.NO_HISTORY, reference, sets,
                profile.unit, false, "Referencia inicial opcional; todavía no hay sesiones comparables.")
        val effort = latest?.rpe ?: valid.last().rpe!!
        val baseChange = when (effort) {
            1 -> 5
            2 -> 3
            3 -> 1
            8 -> -1
            9 -> -2
            10 -> -3
            else -> 0
        }
        if (effort == 8 || baseChange < 0) {
            val reduced = (reference - min(abs(baseChange), profile.permittedIncrement))
                .coerceIn(profile.minimum, profile.maximum)
            return VolumeRecommendation(VolumeDecision.REDUCE, reduced, sets, profile.unit,
                true, "Referencia inferior orientativa por esfuerzo declarado; no se aplica sola.")
        }
        if (baseChange == 0)
            return VolumeRecommendation(VolumeDecision.MAINTAIN, reference, sets, profile.unit,
                false, "Esfuerzo moderado: mantener referencia.")
        val lowDays = valid.filter { it.rpe in 1..3 }.distinctBy { it.date }
        val consistent = lowDays.size >= 3 && lowDays.map { it.sets }.distinct().size == 1 &&
            ChronoUnit.DAYS.between(lowDays.first().date, lowDays.last().date) >= 7
        if (consistent && sets != null && sets in 1..2 && !highCategory)
            return VolumeRecommendation(VolumeDecision.REVIEW_EXTRA_SET, reference, sets + 1,
                profile.unit, true, "Tres días de esfuerzo bajo: serie adicional opcional; valor por serie sin cambio.")
        if (lowDays.size < 2)
            return VolumeRecommendation(VolumeDecision.MAINTAIN, reference, sets, profile.unit,
                false, "Una valoración baja aislada no justifica una subida.")
        val increased = (reference + min(baseChange, profile.permittedIncrement))
            .coerceIn(profile.minimum, profile.maximum)
        if (increased == reference)
            return VolumeRecommendation(VolumeDecision.MAINTAIN, reference, sets, profile.unit,
                false, "Límite de referencia validado alcanzado.")
        return VolumeRecommendation(VolumeDecision.OPTIONAL_INCREASE, increased, sets,
            profile.unit, true, "Cálculo orientativo con historial comparable; requiere consentimiento explícito.")
    }
}
object ProgressTrackingService {
    /** Resultado explícito; una segunda acción con la misma clave reemplaza, no duplica. */
    fun record(current: List<Participation>, next: Participation): List<Participation> =
        current.filterNot { it.date == next.date && it.variantId == next.variantId } + next
    fun byWeek(history: List<Participation>, monday: LocalDate): List<Participation> =
        history.filter { !it.date.isBefore(monday) && it.date.isBefore(monday.plusDays(7)) }
    fun byVariant(history: List<Participation>, id: String): List<Participation> =
        history.filter { it.variantId == id }
}
object RoutineParticipation {
    fun award(currentDays: Set<LocalDate>, date: LocalDate, completedByUser: Boolean):
        Pair<Set<LocalDate>, Int> {
        if (!completedByUser || date in currentDays) return currentDays to 0
        return (currentDays + date) to 10
    }
}
data class CoordinatedResult(
    val mission: DailyMission, val adaptation: Adaptation?,
    val checkpoint: TechnicalCheckpoint?, val volume: Map<String, VolumeRecommendation>,
    val goal: GoalInterpretation
)
/** El coordinador consulta: no concede XP, no aplica referencias y no registra actividad. */
object CoordinatedTrainingEngine {
    fun evaluate(date: LocalDate, profile: TrainingProfile,
                 activities: ActivityRepositoryInterface,
                 efforts: List<EffortObservation>, phases: Map<Category, TechnicalPhase>,
                 volumeProfiles: VolumeProfileInterface,
                 volumeHistory: List<VolumeObservation>, blocks: Map<String, String>,
                 pinned: DailyMission? = null): CoordinatedResult {
        val category = profile.category(date)
        val mission = DailyMissionPlanner.plan(date, profile, activities, efforts, phases, pinned)
        val checkpoint = category?.let {
            TechnicalCheckpointPolicy.review(profile, it, date, efforts,
                phases[it] ?: TechnicalPhase())
        }
        val recommendations = mission.ids.mapNotNull { id ->
            val metadata = activities.byId(id) ?: return@mapNotNull null
            val block = blocks[id] ?: VolumeResetPolicy.block(
                metadata.category, id, profile.experience, profile.experienceStart)
            id to VolumeProgressionEngine.review(date, metadata, volumeProfiles.find(id),
                volumeHistory, block, efforts)
        }.toMap()
        return CoordinatedResult(mission,
            category?.let { CategoryEffortPolicy.decide(efforts, date, it) },
            checkpoint, recommendations, PersonalGoalInterpreter.interpret(profile.customGoal))
    }
}
