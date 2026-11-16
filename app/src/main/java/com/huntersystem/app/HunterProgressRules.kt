package com.huntersystem.app

import java.security.MessageDigest
import java.text.Normalizer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Local, deterministic progression core. This file contains NO exercise records.
 * A catalog is supplied by the host and is never mutated by reading or planning.
 */
enum class HunterCategory(val code: String, val title: String) {
    UPPER("tren_superior", "Tren superior"),
    CORE("tren_central", "Tren central"),
    LOWER("tren_inferior", "Tren inferior"),
    MOBILITY("movilidad", "Movilidad"),
    CARDIO("cardio", "Cardio"),
    REST("descanso", "Descanso");

    companion object {
        val activity = entries.filter { it != REST }
        fun parse(value: String): HunterCategory? = when (value.trim().lowercase(Locale.ROOT)) {
            "tren_superior", "upper" -> UPPER
            "tren_central", "core", "tronco", "integrado" -> CORE
            "tren_inferior", "lower", "piernas" -> LOWER
            "movilidad", "mobility", "coordinacion", "recovery" -> MOBILITY
            "cardio" -> CARDIO
            "descanso", "rest" -> REST
            else -> null
        }
    }
}

enum class HunterExperience { PRINCIPIANTE, INTERMEDIO, AVANZADO }
enum class HunterIntensity { BAJA, MODERADA, ALTA }
enum class HunterStatus { PENDING, COMPLETED, SKIPPED }
enum class HunterUnit { REPETITIONS, SECONDS }
enum class HunterReview { ESPERAR, INSUFICIENTE, MANTENER, VARIAR, RECUPERAR, REVISAR_SENCILLA, AVANZAR_FASE }

/** Stable category > family > type > variant identity. */
data class HunterFamily(val id: String, val category: HunterCategory)
data class HunterType(val id: String, val familyId: String)
data class HunterVariant(
    val id: String,
    val typeId: String,
    val category: HunterCategory,
    val technicalLevel: Int,
    val intensity: HunterIntensity,
    val equipment: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val modality: String = "",
    val supervisionRequired: Boolean = false,
    val automaticEligible: Boolean = false,
    val unit: HunterUnit = HunterUnit.REPETITIONS
)

/** The implementation is injected; it does not create or import any catalog inventory. */
interface HunterCatalogPort {
    fun categories(): List<HunterCategory>
    fun families(category: HunterCategory): List<HunterFamily>
    fun types(familyId: String): List<HunterType>
    fun variants(typeId: String): List<HunterVariant>
    fun variant(id: String): HunterVariant?
    fun find(category: HunterCategory, tags: Set<String> = emptySet()): List<HunterVariant>
    fun revision(): String
}

class ValidatedHunterCatalog(
    families: List<HunterFamily>,
    types: List<HunterType>,
    variants: List<HunterVariant>,
    private val externalRevision: String = ""
) : HunterCatalogPort {
    private val familyById = families.associateBy { it.id }
    private val typeById = types.associateBy { it.id }
    private val variantById = variants.associateBy { it.id }
    private val sorted = variants.sortedBy { it.id }

    init {
        require(families.size == familyById.size && families.all { it.id.isNotBlank() && it.category != HunterCategory.REST })
        require(types.size == typeById.size && types.all { it.id.isNotBlank() && familyById.containsKey(it.familyId) })
        require(variants.size == variantById.size && variants.all {
            it.id.isNotBlank() && it.technicalLevel in 0..5 &&
            typeById[it.typeId]?.let { t -> familyById[t.familyId]?.category == it.category } == true &&
            it.category != HunterCategory.REST && it.equipment.none(String::isBlank)
        }) { "Identificador duplicado o referencia de catálogo inválida" }
    }
    override fun categories() = HunterCategory.activity
    override fun families(category: HunterCategory) = familyById.values.filter { it.category == category }.sortedBy { it.id }
    override fun types(familyId: String) = typeById.values.filter { it.familyId == familyId }.sortedBy { it.id }
    override fun variants(typeId: String) = sorted.filter { it.typeId == typeId }
    override fun variant(id: String) = variantById[id]
    override fun find(category: HunterCategory, tags: Set<String>) =
        sorted.filter { it.category == category && (tags.isEmpty() || it.tags.any(tags::contains)) }
    override fun revision(): String = HunterProgressRules.digest(externalRevision + "|" + sorted.joinToString("|") {
        listOf(it.id, it.typeId, it.category.code, it.technicalLevel, it.intensity, it.equipment.sorted(),
            it.tags.sorted(), it.modality, it.supervisionRequired, it.automaticEligible).joinToString(":")
    })
}

data class HunterSettings(
    val experience: HunterExperience,
    val startedAt: LocalDate,
    val week: List<HunterCategory>,
    val equipment: Set<String>,
    val goal: String,
    val customGoal: String = "",
    val dailyLimit: Int = 3,
    val intensity: HunterIntensity = HunterIntensity.BAJA,
    val minutes: Int = 20,
    val restSeconds: Int = 60,
    val organization: String = "series",
    val mode: String = "misiones",
    val blockedIds: Set<String> = emptySet(),
    val blockedTags: Set<String> = emptySet(),
    val permittedModalities: Set<String> = emptySet()
) {
    init {
        require(week.size == 7 && dailyLimit in 1..7 && minutes in 1..240 && restSeconds in 0..600)
        require(mode in setOf("misiones", "rutinas"))
    }
    fun category(date: LocalDate) = week[date.dayOfWeek.value - 1]
    fun signature(): String = HunterProgressRules.digest(listOf(
        experience, startedAt, week.map { it.code }, equipment.sorted(), goal, customGoal,
        dailyLimit, intensity, minutes, restSeconds, organization, mode, blockedIds.sorted(),
        blockedTags.sorted(), permittedModalities.sorted()
    ).joinToString("\u001e"))
}

data class HunterGoalIntent(val original: String, val recognized: Boolean, val tags: Set<String>) {
    val explanation: String get() = if (recognized) "Objetivo interpretado mediante reglas locales."
    else if (original.isBlank()) "Sin objetivo personal escrito."
    else "Objetivo guardado sin afinidad reconocida."
}

data class HunterActivityRecord(
    val date: LocalDate, val variantId: String, val category: HunterCategory,
    val status: HunterStatus, val note: String = ""
)
data class HunterEffort(
    val date: LocalDate, val category: HunterCategory?, val rpe: Int,
    val hasCompletedActivity: Boolean, val interrupted: Boolean = false
) {
    init { require(rpe in 1..10 && category != HunterCategory.REST) }
}
/** Historical global RPE uses category=null and never silently becomes category-specific. */
data class HunterInterruption(val date: LocalDate, val category: HunterCategory) {
    init { require(category != HunterCategory.REST) }
}
data class HunterPhaseChange(val category: HunterCategory, val phase: Int, val date: LocalDate) {
    init { require(category != HunterCategory.REST && phase in 0..2) }
}
data class HunterVolume(
    val date: LocalDate, val category: HunterCategory, val variantId: String, val typeId: String,
    val technicalLevel: Int, val sets: Int, val amountPerSet: Double, val unit: HunterUnit,
    val effort: Int?, val completed: Boolean, val interrupted: Boolean
) {
    init {
        require(category != HunterCategory.REST && technicalLevel in 0..5 && sets in 1..30)
        require(amountPerSet.isFinite() && amountPerSet > 0 && amountPerSet <= 3600)
        require(effort == null || effort in 1..10)
    }
}

data class HunterHistory(
    val activities: List<HunterActivityRecord> = emptyList(),
    val efforts: List<HunterEffort> = emptyList(),
    val phaseChanges: List<HunterPhaseChange> = emptyList(),
    val volumes: List<HunterVolume> = emptyList(),
    val interruptions: List<HunterInterruption> = emptyList()
) {
    /** Idempotent upsert; status is changed only by an explicit user action. */
    fun record(item: HunterActivityRecord): HunterHistory {
        require(item.category != HunterCategory.REST && item.variantId.isNotBlank())
        return copy(activities = activities.filterNot { it.date == item.date && it.variantId == item.variantId } + item)
    }
    fun recordEffort(item: HunterEffort): HunterHistory = copy(efforts =
        efforts.filterNot { it.date == item.date && it.category == item.category } + item)
    fun recordVolume(item: HunterVolume): HunterHistory = copy(volumes =
        volumes.filterNot { it.date == item.date && it.variantId == item.variantId && it.unit == item.unit } + item)
    fun phase(category: HunterCategory) = phaseChanges.withIndex().filter { it.value.category == category }
        .maxWithOrNull(compareBy<IndexedValue<HunterPhaseChange>> { it.value.date }.thenBy { it.index })
        ?.value?.phase ?: 0
    fun total(variantId: String, unit: HunterUnit): Double? {
        val rows = volumes.filter { it.variantId == variantId && it.unit == unit && it.completed && !it.interrupted }
        return rows.takeIf { it.isNotEmpty() }?.sumOf { it.sets * it.amountPerSet }
    }
}

data class HunterSummary(val planned: Int, val completed: Int, val skipped: Int, val pending: Int) {
    val evaluated = completed + skipped
    val percentage: Int? = if (evaluated == 0) null else (100.0 * completed / evaluated).roundToInt()
    val explanation = if (percentage == null) "Sin datos suficientes" else "Participación en actividades evaluadas"
}
data class HunterWeeklyReview(val decision: HunterReview, val evidence: Int, val explanation: String)
data class HunterTechnicalReview(val decision: HunterReview, val currentPhase: Int, val nextPhase: Int, val explanation: String)
data class HunterGlobalReview(
    val dayOfCycle: Int, val previousMilestone: Int, val nextMilestone: Int,
    val observations: Int, val mean: Double?, val explanation: String
)
data class HunterDailyPlan(
    val date: LocalDate, val category: HunterCategory, val ids: List<String>,
    val settingsHash: String, val catalogHash: String, val explanation: String
)

object HunterProgressRules {
    fun digest(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun plain(text: String): String = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")

    private val intents = linkedMapOf(
        "caminar" to listOf("caminar", "caminata", "pasear"),
        "bicicleta" to listOf("bicicleta", "ciclismo", "pedalear"),
        "carrera" to listOf("correr", "carrera", "trotar"),
        "danza" to listOf("danza", "bailar", "baile"),
        "equilibrio" to listOf("equilibrio"),
        "movilidad" to listOf("movilidad", "moverme"),
        "flexibilidad" to listOf("flexibilidad", "estirar"),
        "postura" to listOf("postura"),
        "fuerza_funcional" to listOf("fuerza funcional", "fortalecer", "fuerza"),
        "coordinacion" to listOf("coordinacion"),
        "resistencia" to listOf("resistencia", "aguante")
    )
    fun interpret(goal: String): HunterGoalIntent {
        val normalized = plain(goal)
        val denied = Regex("\\b(?:no quiero|no deseo|no busco|evitar|evito|sin interes en|sin)\\s+(?:(?:hacer|practicar|mejorar|tener|mas|la|el|los|las)\\s+){0,3}")
        val found = linkedSetOf<String>()
        for ((tag, words) in intents) for (word in words) {
            val pattern = Regex("(?<![a-z])" + Regex.escape(word) + "(?![a-z])")
            for (match in pattern.findAll(normalized)) {
                val clauseStart = normalized.lastIndexOfAny(charArrayOf('.', ',', ';', '\n'), match.range.first - 1) + 1
                val before = normalized.substring(clauseStart, match.range.first)
                if (!denied.containsMatchIn(before.takeLast(80))) found += tag
            }
        }
        return HunterGoalIntent(goal, found.isNotEmpty(), found)
    }

    fun summary(items: List<HunterActivityRecord>): HunterSummary = HunterSummary(
        items.size, items.count { it.status == HunterStatus.COMPLETED },
        items.count { it.status == HunterStatus.SKIPPED }, items.count { it.status == HunterStatus.PENDING }
    )

    fun eligible(v: HunterVariant, p: HunterSettings): Boolean {
        if (!v.automaticEligible || v.supervisionRequired || v.technicalLevel !in 1..3) return false
        if (v.id in p.blockedIds || v.tags.any(p.blockedTags::contains)) return false
        if (!p.equipment.containsAll(v.equipment)) return false
        if (p.intensity == HunterIntensity.BAJA && v.intensity != HunterIntensity.BAJA) return false
        if (v.intensity == HunterIntensity.ALTA) return false
        if (p.permittedModalities.isNotEmpty() && v.modality !in p.permittedModalities) return false
        return when (p.experience) {
            HunterExperience.PRINCIPIANTE -> v.technicalLevel <= 2
            HunterExperience.INTERMEDIO -> v.technicalLevel in 1..3
            HunterExperience.AVANZADO -> v.technicalLevel in 2..3
        }
    }

    private fun categoryRows(h: HunterHistory, date: LocalDate, category: HunterCategory, days: Long) =
        h.efforts.filter { it.category == category && (it.hasCompletedActivity || it.interrupted) && it.date < date &&
            it.date >= date.minusDays(days) }.sortedBy { it.date }

    fun recentEffort(h: HunterHistory, date: LocalDate, category: HunterCategory): HunterEffort? =
        categoryRows(h, date, category, 21).lastOrNull()

    fun weekly(h: HunterHistory, date: LocalDate, category: HunterCategory): HunterWeeklyReview {
        val initial = h.efforts.filter { it.category != null }.minOfOrNull { it.date }
            ?: return HunterWeeklyReview(HunterReview.ESPERAR, 0, "Aún no existe una valoración.")
        if (date < initial.plusDays(7)) return HunterWeeklyReview(HunterReview.ESPERAR, 0,
            "Primera revisión disponible siete días después de la primera valoración.")
        val all = h.efforts.filter { it.category == category && it.date < date && it.date >= date.minusDays(7) }
        if (all.any { it.interrupted || it.rpe >= 8 } ||
            h.interruptions.any { it.category == category && it.date < date &&
                it.date >= date.minusDays(7) }) return HunterWeeklyReview(HunterReview.RECUPERAR, 0,
            "Se prioriza la recuperación por una señal reciente.")
        val lowDays = all.filter { it.hasCompletedActivity && it.rpe in 1..3 }
            .map { it.date }.distinct()
        return if (lowDays.size >= 2) HunterWeeklyReview(HunterReview.VARIAR, lowDays.size,
            "Dos días distintos con sesiones completadas y esfuerzo bajo: solo variedad equivalente.")
        else HunterWeeklyReview(HunterReview.INSUFICIENTE, lowDays.size,
            "Se mantiene el plan: hacen falta dos sesiones completadas y valoradas en días diferentes.")
    }

    fun technical(h: HunterHistory, p: HunterSettings, date: LocalDate, category: HunterCategory): HunterTechnicalReview {
        val current = h.phase(category)
        val elapsed = ChronoUnit.DAYS.between(p.startedAt, date)
        if (elapsed < 7 || (elapsed !in listOf(7L, 14L, 21L)))
            return HunterTechnicalReview(HunterReview.ESPERAR, current, current, "Fuera de un hito técnico.")
        val review = weekly(h, date, category)
        if (review.decision == HunterReview.RECUPERAR)
            return HunterTechnicalReview(HunterReview.REVISAR_SENCILLA, current, current, review.explanation)
        if (review.decision != HunterReview.VARIAR)
            return HunterTechnicalReview(HunterReview.MANTENER, current, current, review.explanation)
        val lastChange = h.phaseChanges.filter { it.category == category }.maxOfOrNull { it.date }
        val enoughInterval = lastChange == null || ChronoUnit.DAYS.between(lastChange, date) >= 7
        val interrupted = h.efforts.any { it.category == category && it.date < date &&
            it.date >= date.minusDays(7) && it.interrupted } ||
            h.interruptions.any { it.category == category && it.date < date &&
                it.date >= date.minusDays(7) }
        if (!enoughInterval || interrupted || current >= 2 || p.experience == HunterExperience.PRINCIPIANTE)
            return HunterTechnicalReview(HunterReview.VARIAR, current, current, "Solo variedad equivalente, sin subir nivel.")
        return HunterTechnicalReview(HunterReview.AVANZAR_FASE, current, current + 1,
            "Hito con evidencia de dos sesiones valoradas y completadas, sin señales prioritarias.")
    }

    fun global(h: HunterHistory, started: LocalDate, date: LocalDate): HunterGlobalReview {
        val elapsed = ChronoUnit.DAYS.between(started, date).coerceAtLeast(0)
        val day = (elapsed % 21).toInt() + 1
        val prev = when { day >= 21 -> 21; day >= 14 -> 14; day >= 7 -> 7; else -> 0 }
        val next = when { day < 7 -> 7; day < 14 -> 14; else -> 21 }
        val cycleStart = date.minusDays((day - 1).toLong())
        val rows = h.efforts.filter { it.date in cycleStart..date && it.date >= date.minusDays(20) }
        val avg = rows.takeIf { it.isNotEmpty() }?.map { it.rpe.toDouble() }?.average()
        return HunterGlobalReview(day, prev, next, rows.size, avg,
            if (avg == null) "No hay observaciones para determinar una tendencia."
            else "Resumen descriptivo del esfuerzo declarado; no altera las propuestas por categoría.")
    }

    fun propose(p: HunterSettings, date: LocalDate, catalog: HunterCatalogPort, history: HunterHistory,
                pinned: HunterDailyPlan? = null): HunterDailyPlan {
        val category = p.category(date)
        val signature = p.signature()
        val revision = catalog.revision()
        if (category == HunterCategory.REST)
            return HunterDailyPlan(date, category, emptyList(), signature, revision, "Descanso elegido: no se generan actividades.")
        val candidates = catalog.find(category).filter { eligible(it, p) }
        if (pinned != null && pinned.date == date && pinned.category == category &&
            pinned.settingsHash == signature && pinned.catalogHash == revision &&
            pinned.ids.size == pinned.ids.toSet().size &&
            pinned.ids.all { id -> candidates.any { it.id == id } })
            return pinned
        if (candidates.isEmpty()) return HunterDailyPlan(date, category, emptyList(), signature, revision,
            "No hay actividades compatibles con el calendario, equipo, técnica y restricciones.")
        val last = recentEffort(history, date, category)
        val recentStop = history.interruptions.any { it.category == category &&
            it.date < date && it.date >= date.minusDays(21) }
        val maxCount = when { recentStop -> 1; last?.interrupted == true -> 1; last?.rpe == 8 -> 2; last?.rpe in 9..10 -> 1; else -> p.dailyLimit }
            .coerceAtMost(p.dailyLimit)
        val review = weekly(history, date, category)
        val intent = interpret(p.customGoal)
        val technicalMix = when (p.experience) {
            HunterExperience.PRINCIPIANTE -> 0.0
            HunterExperience.INTERMEDIO -> listOf(.25, .40, .60)[history.phase(category)]
            HunterExperience.AVANZADO -> listOf(.33, .55, .75)[history.phase(category)]
        }
        val selected = mutableListOf<HunterVariant>()
        val equivalent = if (review.decision == HunterReview.VARIAR) {
            val lastCompleted = history.activities.filter { it.category == category &&
                it.status == HunterStatus.COMPLETED && it.date < date }.maxByOrNull { it.date }
            val old = lastCompleted?.let { catalog.variant(it.variantId) }
            if (old == null) null else candidates.firstOrNull { it.id != old.id &&
                it.typeId == old.typeId && it.technicalLevel == old.technicalLevel &&
                it.intensity == old.intensity && it.unit == old.unit }
        } else null
        if (equivalent != null) selected += equivalent
        val recent = history.activities.filter { it.category == category &&
            it.status == HunterStatus.COMPLETED && it.date < date && it.date >= date.minusDays(21) }
            .map { it.variantId }.toSet()
        val previous = history.activities.filter { it.category == category &&
            it.status == HunterStatus.COMPLETED && it.date < date }.maxByOrNull { it.date }
        val previousVariant = previous?.let { catalog.variant(it.variantId) }
        val simple = last?.rpe?.let { it >= 8 } == true || last?.interrupted == true || recentStop
        val desiredLevel3 = (maxCount * technicalMix).roundToInt()
        repeat((maxCount - selected.size).coerceAtLeast(0)) {
            val pool = candidates.filterNot { v -> selected.any { it.id == v.id } }
            if (pool.isEmpty()) return@repeat
            fun score(v: HunterVariant): Int {
                var n = intent.tags.count { it in v.tags } * 50
                if (review.decision == HunterReview.VARIAR && v.id in recent) n -= 18
                n -= selected.count { it.typeId == v.typeId } * 35
                if (selected.isNotEmpty() && selected.any { it.typeId != v.typeId }) n += 1
                if (simple) n -= v.technicalLevel * 22
                if (v.technicalLevel == 3) n +=
                    if (selected.count { it.technicalLevel == 3 } < desiredLevel3) 12 else -25
                if (review.decision == HunterReview.VARIAR && previousVariant != null &&
                    v.typeId == previousVariant.typeId && v.technicalLevel == previousVariant.technicalLevel &&
                    v.id != previousVariant.id) n += 8
                if (last?.rpe in 1..3 && review.decision != HunterReview.VARIAR &&
                    previousVariant?.id == v.id) n += 8 // No change solely because one RPE is low
                return n
            }
            selected += pool.sortedWith(compareByDescending<HunterVariant> { score(it) }
                .thenBy { it.id }).first()
        }
        val explanation = when {
            recentStop || last?.interrupted == true || last?.rpe in 9..10 ->
                "Recuperación prioritaria: máximo una actividad sencilla de esta categoría."
            last?.rpe == 8 -> "Esfuerzo alto previo: máximo dos actividades; se prefieren opciones sencillas."
            review.decision == HunterReview.VARIAR ->
                "Se permite variedad equivalente por registros de días distintos, sin aumentar carga."
            else -> "Propuesta compatible y estable para esta fecha; " + intent.explanation
        }
        return HunterDailyPlan(date, category, selected.map { it.id }, signature, revision, explanation)
    }
}
