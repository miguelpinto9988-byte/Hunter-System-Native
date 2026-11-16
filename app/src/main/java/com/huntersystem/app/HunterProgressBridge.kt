package com.huntersystem.app

import java.time.LocalDate
import org.json.JSONObject
import org.json.JSONArray

/**
 * Non-destructive adapter for the EXISTING catalog and persisted JSON schema.
 * No asset, exercise entry, or historical identifier is created or rewritten.
 */
object HunterProgressBridge {
    fun settings(p: JSONObject, date: LocalDate): HunterSettings {
        val chosen = p.a("weekCategories").strings().takeIf { it.size == 7 }
        val week = (0..6).map { i ->
            val legacy = p.a("weekPlan").optString(i, "rest")
            val focus = p.a("weekFocus").optString(i, "upper")
            if (chosen != null) HunterCategory.parse(chosen[i]) ?: HunterCategory.REST
            else when (legacy) {
                "train" -> HunterCategory.parse(focus) ?: HunterCategory.UPPER
                "cardio" -> HunterCategory.CARDIO
                "mobility", "recovery" -> HunterCategory.MOBILITY
                else -> HunterCategory.REST
            }
        }
        val experience = when (p.s("difficulty")) {
            "advanced" -> HunterExperience.AVANZADO
            "intermediate" -> HunterExperience.INTERMEDIO
            else -> HunterExperience.PRINCIPIANTE
        }
        val started = runCatching { LocalDate.parse(p.s("experienceStartedAt")) }.getOrElse { date }
        val available = mutableSetOf("body")
        available += p.a("materials46").strings()
        available += p.a("gymEquipment23").strings()
        if (p.s("equipment") in listOf("db", "gym")) available += "db"
        if (p.s("equipment") == "gym") available += "gym"
        if (p.b("bench")) available += "bench"
        if (p.b("step")) available += "step"
        return HunterSettings(
            experience = experience, startedAt = started, week = week, equipment = available,
            goal = p.s("goal", "general"),
            customGoal = p.s("custom_goal", p.s("finalGoal")).take(500),
            dailyLimit = p.i("dailyMissions", 3).coerceIn(1, 7),
            intensity = if (p.s("intensityPreference") == "moderada") HunterIntensity.MODERADA else HunterIntensity.BAJA,
            minutes = p.i("minutes", 20).coerceIn(1, 240),
            restSeconds = p.i("restSeconds", 60).coerceIn(0, 600),
            organization = p.s("blockMode22", "series"),
            mode = if (p.s("trainingMode") == "routines") "rutinas" else "misiones",
            blockedIds = p.a("blockedActivityIds").strings().toSet(),
            blockedTags = p.a("blockedActivityTags").strings().toSet()
        )
    }

    fun catalog(c: Catalog, r: Rules, p: JSONObject): HunterCatalogPort {
        val exercises = c.exercises.mapNotNull { e ->
            val category = HunterCategory.parse(e.type)?.takeIf { it != HunterCategory.REST }
                ?: return@mapNotNull null
            val familyId = category.code + "/" + e.family
            val typeId = familyId + "/" + e.oldFamily
            val explicit = when {
                e.raw.has("technicalLevel") -> e.raw.i("technicalLevel", 0)
                e.raw.has("technicalLevel46") -> e.raw.i("technicalLevel46", 0)
                e.meta.has("level") && e.planningLevel in 0..2 -> e.planningLevel + 1
                else -> 0 // Metadata missing: never guess technical eligibility.
            }
            val level = explicit.takeIf { it in 0..5 } ?: 0
            val intensity = when (e.raw.s("intensity").lowercase()) {
                "low", "baja" -> HunterIntensity.BAJA
                "moderate", "moderada", "medium" -> HunterIntensity.MODERADA
                else -> HunterIntensity.MODERADA // Conservative when descriptive intensity is missing.
            }
            val required = buildSet {
                addAll(e.raw.a("materials46").strings())
                addAll(e.raw.a("materialRequirements").strings())
                if (e.equipment == "db") add("db")
                if (e.equipment == "gym" || e.gym) add("gym")
            }
            val tags = buildSet {
                addAll(e.raw.a("sportTags").strings())
                addAll(e.raw.a("tags").strings())
                if (e.pattern.isNotBlank()) add(e.pattern)
            }
            val eligible = r.compatible(e, p) && !e.raw.b("pendingTechnicalReview") &&
                (!e.raw.b("sourceExpansion2k") || e.raw.b("approvedForAutoplan"))
            Triple(HunterFamily(familyId, category), HunterType(typeId, familyId),
                HunterVariant(
                    id = e.id, typeId = typeId, category = category, technicalLevel = level,
                    intensity = intensity, equipment = required, tags = tags,
                    modality = e.raw.s("modality"),
                    supervisionRequired = e.raw.b("requiresSupervision") ||
                        e.raw.b("supervisionRequired") || e.raw.b("highSkill46"),
                    automaticEligible = eligible,
                    unit = if (e.unit == "sec") HunterUnit.SECONDS else HunterUnit.REPETITIONS
                ))
        }
        return ValidatedHunterCatalog(
            exercises.map { it.first }.distinctBy { it.id },
            exercises.map { it.second }.distinctBy { it.id },
            exercises.map { it.third },
            "catalog-native-v43"
        )
    }

    /**
     * Prior logs lacking an explicit categoryRpe remain global-only.
     * Planned items are not silently turned into completed items.
     */
    fun history(logs: JSONObject, phases: JSONArray = JSONArray()): HunterHistory {
        val activities = mutableListOf<HunterActivityRecord>()
        val efforts = mutableListOf<HunterEffort>()
        val volumes = mutableListOf<HunterVolume>()
        for ((key, raw) in logs.entries()) {
            val date = runCatching { LocalDate.parse(key) }.getOrNull() ?: continue
            val log = raw as? JSONObject ?: continue
            val group = HunterCategory.parse(log.s("trainingGroup"))?.takeIf { it != HunterCategory.REST }
            val details = log.a("items").objects()
            for (item in details) {
                val id = item.s("id")
                if (id.isBlank() || group == null) continue
                val done = item.b("missionState") || item.b("selfReported") ||
                    item.a("actual").objects().any { it.b("done") }
                val explicit = item.s("progressStatus")
                val state = when {
                    explicit == "SKIPPED" -> HunterStatus.SKIPPED
                    done || explicit == "COMPLETED" -> HunterStatus.COMPLETED
                    else -> HunterStatus.PENDING
                }
                activities += HunterActivityRecord(date, id, group, state, item.s("progressNote"))
                val completedSets = item.a("actual").objects().filter { it.b("done") }
                val value = completedSets.map { it.n("value") }.takeIf { it.isNotEmpty() }?.average()
                if (value != null && value > 0 && value.isFinite()) {
                    volumes += HunterVolume(date, group, id, item.s("typeId", id),
                        item.i("technicalLevel", 0).coerceIn(0, 5), completedSets.size,
                        value, if (item.s("unit") == "sec") HunterUnit.SECONDS else HunterUnit.REPETITIONS,
                        log.i("rpe").takeIf { it in 1..10 }, log.b("complete"), log.b("pain") || log.b("fatigue"))
                }
            }
            val rpe = log.i("rpe")
            if (rpe in 1..10) {
                val explicitCategory = HunterCategory.parse(log.s("categoryRpe"))
                    ?.takeIf { it != HunterCategory.REST }
                efforts += HunterEffort(date, explicitCategory, rpe,
                    log.b("complete") && details.any { it.b("missionState") ||
                        it.b("selfReported") || it.a("actual").objects().any { a -> a.b("done") } },
                    log.b("pain") || log.b("fatigue"))
            }
        }
        val changes = phases.objects().mapNotNull {
            val c = HunterCategory.parse(it.s("category"))?.takeIf { c -> c != HunterCategory.REST }
            val date = runCatching { LocalDate.parse(it.s("date")) }.getOrNull()
            if (c != null && date != null && it.i("phase") in 0..2)
                HunterPhaseChange(c, it.i("phase"), date) else null
        }
        return HunterHistory(activities, efforts, changes, volumes)
    }

    fun pinned(day: JSONObject): HunterDailyPlan? {
        val date = runCatching { LocalDate.parse(day.s("date")) }.getOrNull() ?: return null
        val category = HunterCategory.parse(day.s("progressCategory")) ?: return null
        val sig = day.s("progressSettingsHash")
        val cat = day.s("progressCatalogHash")
        if (sig.isBlank() || cat.isBlank()) return null
        return HunterDailyPlan(date, category, day.a("items").objects().map { it.s("id") }, sig, cat,
            day.s("planningNote"))
    }
}
