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

internal fun canRunCardioIntervals(
    exercise: Exercise, rules: Rules, profile: JSONObject, level: Int, adaptation: Adaptation
): Boolean = exercise.id in intervalExerciseIds && exercise.planningLevel <= level &&
    !rules.reduced(profile) && adaptation != Adaptation.RECOVER_ONE &&
    adaptation != Adaptation.REDUCE_TWO && rules.compatible(exercise, profile)

internal fun cardioIntervalRoutines(
    planner: Planner,
    mode: String,
    level: Int,
    duration: Int,
    profile: JSONObject,
    logs: JSONObject,
    date: LocalDate,
    avoidImpact: Boolean,
    onlyBody: Boolean
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
    return intervalExerciseIds.mapNotNull(catalog::originalExercise)
        .filter { exercise ->
            exercise.gym == (mode == "gym") &&
                (!avoidImpact || !exercise.impact) &&
                (!onlyBody || (exercise.equipmentKeys - "space").isEmpty()) &&
                canRunCardioIntervals(exercise, rules, profile, level, adaptation)
        }.mapNotNull { exercise ->
            val rope = "jumprope" in exercise.equipmentKeys
            val work = if (rope) listOf(15, 20, 25)[level] else listOf(20, 30, 45)[level]
            val recovery = if (level == 2) 75 else 60
            val rounds = minOf((duration * 60 - warmup - cooldown + recovery) / (work + recovery),
                listOf(6, 8, 10)[level])
            if (rounds < 2) return@mapNotNull null
            val id = exercise.id
            val warmupLabel = when {
                exercise.gym -> "Comienza en la máquina a ritmo suave"
                rope -> "Marcha y practica el giro de la soga"
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
            val routine = obj(
                "id" to "monarca-routine-" + ProfileSignature.stable("intervalos|$date|$duration|$level", id),
                "name" to "Intervalos · ${exercise.name}",
                "description" to "$rounds tramos de $work s con $recovery s de recuperación. " +
                    "Empieza a un ritmo que puedas repetir y baja la intensidad si pierdes el control.",
                "category" to "Cardio", "focus3845" to "cardio", "level" to level,
                "mode3845" to mode, "gymOnly23" to exercise.gym, "gymRequirements23" to arr(exercise.gymRequirements),
                "impact" to if (exercise.impact) "Con saltos" else "Sin saltos",
                "availableMinutes" to duration, "unifiedPlanning" to true, "cardioIntervals" to true, "items" to arr(listOf(item)),
                "sourceSequence" to obj("steps" to arr(steps))
            )
            routine.put("minutes", planner.minutes(routine, profile))
        }
}
