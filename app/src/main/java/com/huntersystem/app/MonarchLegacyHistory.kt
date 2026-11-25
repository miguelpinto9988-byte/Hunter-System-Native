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
                    item.s("progressStatus") == "COMPLETED" -> ParticipationStatus.COMPLETED
                    item.b("missionState") || item.b("selfReported") ||
                        (item.i("sets") > 0 && item.a("actual").length() >= item.i("sets") &&
                            item.a("actual").objects().count { it.b("done") } >= item.i("sets")) ->
                        ParticipationStatus.COMPLETED
                    else -> ParticipationStatus.PENDING
                }
                records += Participation(date, id, category, status,
                    item.s("progressNote"), log.b("pain") || log.b("fatigue"))
            }
        }
        return records
    }

    fun efforts(logs: JSONObject): List<EffortObservation> {
        val result = mutableListOf<EffortObservation>()
        for ((rawDate, rawValue) in logs.entries()) {
            val date = validDate(rawDate) ?: continue
            val log = rawValue as? JSONObject ?: continue
            val rpe = log.i("rpe")
            if (rpe !in 1..10) continue
            // Sin categoryRpe, el valor original es exclusivamente global.
            val category = CategoryMapper.normalize(log.s("categoryRpe"))
            val items = log.a("items").objects()
            val completed = log.b("complete") && items.any { item ->
                item.b("missionState") || item.b("selfReported") ||
                    (item.i("sets") > 0 && item.a("actual").objects().count { it.b("done") } >= item.i("sets"))
            }
            result += EffortObservation(date, category, rpe, completed,
                log.b("pain") || log.b("fatigue"))
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
