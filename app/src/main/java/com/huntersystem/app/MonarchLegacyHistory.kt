package com.huntersystem.app

import com.huntersystem.app.planning.*
import org.json.JSONObject
import java.time.LocalDate

/**
 * Vista de los registros históricos: NO migra, NO borra ni reescribe datos.
 * Un RPE global heredado permanece global salvo si tiene categoryRpe explícito.
 */
object MonarchLegacyHistory {
    private fun validDate(raw: String): LocalDate? =
        runCatching { LocalDate.parse(raw) }.getOrNull()

    private fun categoryOf(id: String, catalog: Catalog): Category? =
        CategoryMapper.normalize(catalog.byId[id]?.type ?: "")

    internal fun completed(item: JSONObject): Boolean =
        !item.b("archived") && item.s("progressStatus") != "SKIPPED" &&
            (item.s("progressStatus") == "COMPLETED" || item.b("missionState") ||
                item.b("selfReported") || (item.i("sets") > 0 &&
                    item.a("actual").objects().count { it.b("done") } >= item.i("sets")))

    private fun interrupted(log: JSONObject): Boolean =
        log.b("interrupted") || log.b("pain") || log.b("fatigue")

    fun interruptions(logs: JSONObject): List<SessionInterruption> = logs.entries().mapNotNull { (day, raw) ->
        val date = validDate(day) ?: return@mapNotNull null
        val log = raw as? JSONObject ?: return@mapNotNull null
        if (!interrupted(log)) return@mapNotNull null
        val category = CategoryMapper.normalize(log.s("progressCategory"))
            ?: CategoryMapper.normalize(log.s("categoryRpe")) ?: return@mapNotNull null
        SessionInterruption(date, category)
    }

    fun participation(logs: JSONObject, catalog: Catalog): List<Participation> {
        val records = mutableListOf<Participation>()
        for ((rawDate, rawValue) in logs.entries()) {
            val date = validDate(rawDate) ?: continue
            val log = rawValue as? JSONObject ?: continue
            for (item in log.a("items").objects()) {
                val id = item.s("id")
                val category = categoryOf(id, catalog) ?: continue
                val status = when {
                    item.s("progressStatus") == "SKIPPED" -> ParticipationStatus.SKIPPED
                    completed(item) -> ParticipationStatus.COMPLETED
                    else -> ParticipationStatus.PENDING
                }
                val sessionCategory = CategoryMapper.normalize(log.s("progressCategory"))
                    ?: CategoryMapper.normalize(log.s("categoryRpe"))
                records += Participation(date, id, category, status, item.s("progressNote"),
                    item.b("interrupted") || (category == sessionCategory && interrupted(log)))
            }
        }
        return records
    }

    fun efforts(logs: JSONObject, catalog: Catalog? = null): List<EffortObservation> {
        val result = mutableListOf<EffortObservation>()
        for ((rawDate, rawValue) in logs.entries()) {
            val date = validDate(rawDate) ?: continue
            val log = rawValue as? JSONObject ?: continue
            val category = CategoryMapper.normalize(log.s("categoryRpe"))
            val completedItems = log.a("items").objects().filter(::completed)
            val rpe = log.i("rpe")
            if (category != null && rpe in 1..10) {
                val participated = if (catalog == null) completedItems.isNotEmpty()
                    else completedItems.any { categoryOf(it.s("id"), catalog) == category }
                result += EffortObservation(date, category, rpe, participated, interrupted(log))
            }
            val globalRpe = if (log.has("globalRpe")) log.i("globalRpe") else if (category == null) rpe else 0
            if (globalRpe in 1..10) {
                result += EffortObservation(date, null, globalRpe, completedItems.isNotEmpty(), interrupted(log))
            }
        }
        return result
    }

    fun mergeParticipation(legacy: List<Participation>,
                           explicit: List<Participation>): List<Participation> =
        (legacy + explicit).associateBy { it.date to it.variantId }.values
            .sortedWith(compareBy<Participation> { it.date }.thenBy { it.variantId })

    fun mergeEffort(legacy: List<EffortObservation>,
                    explicit: List<EffortObservation>): List<EffortObservation> =
        (legacy + explicit).associateBy { it.date to it.category }.values
            .sortedWith(compareBy<EffortObservation> { it.date }
                .thenBy { it.category?.code ?: "" })
}
