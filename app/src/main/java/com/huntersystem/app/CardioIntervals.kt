package com.huntersystem.app

import com.huntersystem.app.planning.Adaptation
import com.huntersystem.app.planning.Category
import com.huntersystem.app.planning.CategoryEffortPolicy
import com.huntersystem.app.planning.ProfileSignature
import org.json.JSONObject
import java.time.LocalDate

internal val continuousGymCardio = setOf("gym-row-erg23", "gym-bike30", "gym-airbike30")

private val intervalExerciseIds = listOf(
    "performance-outdoor-run", "new46-jumpropealt", "new46-jumpropeside"
) + continuousGymCardio

internal fun canRunCardioSession(
    exercise: Exercise, rules: Rules, profile: JSONObject, level: Int, adaptation: Adaptation,
    continuous: Boolean = false
): Boolean = (if (continuous) exercise.id == "performance-continuous-run" else exercise.id in intervalExerciseIds) &&
    exercise.planningLevel <= level &&
    !rules.reduced(profile) && adaptation != Adaptation.RECOVER_ONE &&
    adaptation != Adaptation.REDUCE_TWO && rules.compatible(exercise, profile)

internal fun guidedCardioRoutines(
    planner: Planner,
    mode: String,
    level: Int,
    duration: Int,
    profile: JSONObject,
    logs: JSONObject,
    date: LocalDate,
    avoidImpact: Boolean,
    onlyBody: Boolean,
    continuous: Boolean
): List<JSONObject> {
    val catalog = planner.catalog
    val rules = planner.rules
    if (mode !in setOf("offline", "gym") || duration !in 10..60 || rules.reduced(profile)) return emptyList()
    val adaptation = CategoryEffortPolicy.decide(
        MonarchLegacyHistory.efforts(logs, catalog), date, Category.CARDIO,
        MonarchLegacyHistory.interruptions(logs, catalog)
    )
    val warmup = if (duration >= 15) 300 else 240
    val cooldown = 180
    val candidates = if (continuous) listOf("performance-continuous-run") else intervalExerciseIds
    return candidates.mapNotNull(catalog::originalExercise)
        .filter { exercise ->
            exercise.gym == (mode == "gym") &&
                (!avoidImpact || !exercise.impact) &&
                (!onlyBody || (exercise.equipmentKeys - "space").isEmpty()) &&
                canRunCardioSession(exercise, rules, profile, level, adaptation, continuous)
        }.mapNotNull { exercise ->
            val rope = "jumprope" in exercise.equipmentKeys
            val availableWork = duration * 60 - warmup - cooldown
            val work = when {
                continuous -> minOf(availableWork, if (level == 2) 25 * 60 else 15 * 60)
                rope -> listOf(15, 20, 25)[level]
                else -> listOf(20, 30, 45)[level]
            }
            val recovery = when {
                continuous -> 0
                level == 2 -> 75
                else -> 60
            }
            val rounds = if (continuous) 1 else minOf((availableWork + recovery) / (work + recovery),
                listOf(6, 8, 10)[level])
            if (work <= 0 || !continuous && rounds < 2) return@mapNotNull null
            val id = exercise.id
            val warmupLabel = when {
                exercise.gym -> "Comienza en la máquina a ritmo suave"
                rope -> "Marcha y practica el giro de la soga"
                continuous -> "Camina y trota suave antes de mantener el ritmo"
                else -> "Camina y trota suave antes de acelerar"
            }
            val steps = listOf(
                obj("kind" to "preparation", "label" to warmupLabel, "seconds" to warmup),
                obj("kind" to "exercise", "exerciseId" to id, "sets" to rounds,
                    "target" to work, "unit" to "sec", "restSeconds" to recovery),
                obj("kind" to "cooldown", "label" to "Reduce el ritmo de forma gradual", "seconds" to cooldown)
            )
            val item = obj("id" to id, "sets" to rounds, "target" to work, "unit" to "sec",
                "rest" to recovery, "unifiedPlanning" to true, "noAutoLoad" to true)
            val format = if (continuous) "continuo" else "intervalos"
            val routine = obj(
                "id" to "monarca-routine-" + ProfileSignature.stable("$format|$date|$duration|$level", id),
                "name" to if (continuous) exercise.name else "Intervalos · ${exercise.name}",
                "description" to if (continuous)
                    "${work / 60} min de carrera a ritmo conversacional. Si pierdes el ritmo, alterna con caminata."
                else "$rounds tramos de $work s con $recovery s de recuperación. " +
                    "Empieza a un ritmo que puedas repetir y baja la intensidad si pierdes el control.",
                "category" to "Cardio", "focus3845" to "cardio", "level" to level,
                "mode3845" to mode, "gymOnly23" to exercise.gym, "gymRequirements23" to arr(exercise.gymRequirements),
                "impact" to if (exercise.impact) "Con saltos" else "Sin saltos",
                "availableMinutes" to duration, "unifiedPlanning" to true,
                "cardioIntervals" to !continuous, "cardioContinuous" to continuous, "items" to arr(listOf(item)),
                "sourceSequence" to obj("steps" to arr(steps))
            )
            routine.put("minutes", planner.minutes(routine, profile))
        }
}
