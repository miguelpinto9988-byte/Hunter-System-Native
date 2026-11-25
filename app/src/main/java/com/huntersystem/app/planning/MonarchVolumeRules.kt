package com.huntersystem.app.planning

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
data class VolumeReference(val value: Int, val sets: Int, val unit: VolumeUnit) {
    init { require(value > 0 && sets in 1..3) }
}
object VolumeResetPolicy {
    fun changedVariants(previous: Set<String>, selected: Set<String>): Set<String> =
        if (previous.isEmpty()) emptySet() else selected - previous

    fun block(category: Category, variantId: String, experience: Experience, since: LocalDate): String =
        ProfileSignature.stable("volume|" + category.code + "|" + experience.name +
            "|" + since, variantId)
}
object VolumeProgressionEngine {
    fun review(today: LocalDate, activity: ActivityMetadata, profile: VolumeProfile?,
               history: List<VolumeObservation>, blockId: String,
               categoryHistory: List<EffortObservation> = emptyList(),
               interruptions: List<SessionInterruption> = emptyList()): VolumeRecommendation {
        if (profile == null || profile.variantId != activity.id ||
            (activity.workUnit != null && profile.unit != activity.workUnit))
            return VolumeRecommendation(VolumeDecision.GUIDED_REVIEW, null, null, null,
                false, "La variante carece de parámetros numéricos compatibles y validados.")
        val start = today.minusDays(21)
        val matches = history.filter {
            it.date < today && !it.date.isBefore(start) &&
                it.category == activity.category && it.variantId == activity.id &&
                it.typeId == activity.typeId && it.technicalLevel == activity.technicalLevel &&
                it.unit == profile.unit && it.blockId == blockId
        }.sortedWith(compareBy<VolumeObservation> { it.date }.thenBy { it.id })
        val completed = matches.filter { it.completed && !it.interrupted }
        val latest = completed.lastOrNull()
        val reference = latest?.quantityPerSet?.coerceIn(profile.minimum, profile.maximum)
            ?: profile.initialValue
        val sets = latest?.sets?.coerceAtMost(3) ?: profile.initialSets
        fun result(decision: VolumeDecision, explanation: String, value: Int? = reference,
                   series: Int? = sets, optional: Boolean = false) =
            VolumeRecommendation(decision, value, series, profile.unit, optional, explanation)
        if (!profile.automaticReviewEligible)
            return result(VolumeDecision.GUIDED_REVIEW, "Revisión manual. La referencia queda disponible para consulta.")
        fun relevant(date: LocalDate, category: Category?) =
            category == activity.category && date < today && !date.isBefore(start)
        if (matches.any { it.interrupted } || interruptions.any { relevant(it.date, it.category) } ||
            categoryHistory.any { relevant(it.date, it.category) && it.interrupted })
            return result(VolumeDecision.PAUSED, "Interrupción declarada en el período de revisión. Se suspenden los aumentos.")
        val baseChange = when (latest?.rpe) {
            1 -> 5
            2 -> 3
            3 -> 1
            8 -> -1
            9 -> -2
            10 -> -3
            else -> 0
        }
        val highCategory = categoryHistory.any { relevant(it.date, it.category) && it.rpe >= 8 }
        val highVariant = completed.any { (it.rpe ?: 0) >= 8 }
        if (reference != null && baseChange < 0) {
            val lower = (reference - min(abs(baseChange), profile.permittedIncrement))
                .coerceIn(profile.minimum, profile.maximum)
            val severe = (latest?.rpe ?: 0) >= 9
            return result(if (severe) VolumeDecision.RECOVERY_PRIORITY else VolumeDecision.REDUCE,
                if (severe) "Esfuerzo 9–10. Prioridad de recuperación y referencia inferior opcional."
                else "Esfuerzo 8. Referencia inferior opcional.",
                value = lower, optional = lower < reference)
        }
        if (highCategory || highVariant)
            return result(VolumeDecision.RECOVERY_PRIORITY, "Esfuerzo alto reciente de la categoría o variante. Se conservan los límites de recuperación.")
        if (latest == null)
            return result(VolumeDecision.NO_HISTORY,
                if (reference == null) "Sin historial comparable ni referencia inicial validada."
                else "Referencia inicial opcional. Todavía no hay sesiones comparables.")
        if (reference == null || baseChange == 0)
            return result(VolumeDecision.MAINTAIN, "La observación más reciente carece de esfuerzo utilizable para un aumento o corresponde a mantenimiento.")
        val lowDays = completed.asReversed().distinctBy { it.date }.asReversed()
            .filter { it.rpe in 1..3 }
        val consistent = lowDays.size >= 3 && lowDays.all { it.sets == latest.sets } &&
            ChronoUnit.DAYS.between(lowDays.first().date, lowDays.last().date) >= 7
        if (consistent && sets != null && sets < 3)
            return result(VolumeDecision.REVIEW_EXTRA_SET,
                "Tres sesiones comparables en días distintos. Puedes revisar una serie adicional con la misma cantidad por serie.",
                series = sets + 1, optional = true)
        if (lowDays.size < 2)
            return result(VolumeDecision.MAINTAIN, "Se necesitan al menos dos días comparables con esfuerzo bajo para revisar un aumento.")
        val increased = (reference.toLong() + min(baseChange, profile.permittedIncrement))
            .coerceAtMost(profile.maximum.toLong()).toInt()
        if (increased == reference)
            return result(VolumeDecision.MAINTAIN, "La referencia ya está en el máximo validado.")
        val decision = if (increased - reference == 1) VolumeDecision.OPTIONAL_MINIMUM_ADJUSTMENT
            else VolumeDecision.OPTIONAL_INCREASE
        return result(decision,
            "Cálculo orientativo con historial comparable. Puedes mantener tu referencia o aceptar el ajuste.",
            value = increased, optional = true)
    }
}
object ProgressTrackingService {
    /** Resultado explícito; una segunda acción con la misma clave reemplaza, no duplica. */
    fun record(current: List<Participation>, next: Participation): List<Participation> =
        current.filterNot { it.date == next.date && it.variantId == next.variantId } + next
    fun byWeek(history: List<Participation>, monday: LocalDate): List<Participation> =
        history.filter { !it.date.isBefore(monday) && it.date.isBefore(monday.plusDays(7)) }
    fun byDate(history: List<Participation>, date: LocalDate): List<Participation> =
        history.filter { it.date == date }
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
