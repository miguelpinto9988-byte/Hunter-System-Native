package com.huntersystem.app

import com.huntersystem.app.planning.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Pruebas con datos sintéticos: este archivo no añade ejercicios al catálogo.
 * Solo se consultan IDs ya disponibles para comprobar el adaptador de lectura.
 */
class MonarchLegacyHistoryTest {
    private val catalog = Catalog(loader = { File("src/main/assets", it).readText() })
    private val category = CategoryMapper.normalize(catalog.byId["chair"]!!.type)!!
    private val date = LocalDate.parse("2026-11-02")
    private fun originalLog(rpe: Int = 4, categoryRpe: String? = null): JSONObject =
        obj("date" to date.toString(), "kind" to "strength",
            "complete" to true, "rpe" to rpe,
            "items" to arr(listOf(
                obj("id" to "chair", "missionState" to true, "sets" to 2,
                    "actual" to arr(emptyList<JSONObject>())),
                obj("id" to "row", "sets" to 2,
                    "actual" to arr(emptyList<JSONObject>()))
            ))).apply {
                if (categoryRpe != null) put("categoryRpe", categoryRpe)
            }
    @Test fun oldGlobalRpeNeverBecomesCategoryEffort() {
        val logs = obj(date.toString() to originalLog())
        val before = logs.toString()
        val one = MonarchLegacyHistory.efforts(logs).single()
        assertNull(one.category)
        assertEquals(4, one.rpe)
        assertEquals(before, logs.toString())
    }
    @Test fun onlyExplicitCategoryRpeIsCategorySpecific() {
        val logs = obj(date.toString() to originalLog(categoryRpe = category.code))
        assertEquals(category, MonarchLegacyHistory.efforts(logs).single().category)
    }
    @Test fun plannedAndCompletedRemainSeparateWithoutFakeSets() {
        val logs = obj(date.toString() to originalLog())
        val rows = MonarchLegacyHistory.participation(logs, catalog)
        assertEquals(2, rows.size)
        assertEquals(1, rows.count { it.status == ParticipationStatus.COMPLETED })
        assertEquals(1, rows.count { it.status == ParticipationStatus.PENDING })
        assertEquals(100, ProgressSummary.calculate(rows).percent)
    }
    @Test fun explicitSkipTakesPrecedenceAndIsIdempotent() {
        val logs = obj(date.toString() to originalLog())
        val row = MonarchLegacyHistory.participation(logs, catalog).first()
        val skip = row.copy(status = ParticipationStatus.SKIPPED)
        val merged = MonarchLegacyHistory.mergeParticipation(listOf(row), listOf(skip, skip))
        assertEquals(1, merged.size)
        assertEquals(ParticipationStatus.SKIPPED, merged.single().status)
    }
    @Test fun interruptionIsNotAutomaticallyMarkedSkipped() {
        val log = originalLog()
        log.put("pain", true)
        val first = MonarchLegacyHistory.participation(obj(date.toString() to log), catalog).first()
        assertTrue(first.interrupted)
        assertEquals(ParticipationStatus.COMPLETED, first.status)
    }
    @Test fun categoryEffortFiltersLegacyGlobalEvenWithMatchingTrainingGroup() {
        val logs = obj(date.toString() to originalLog().put("trainingGroup", "upper"))
        val observations = MonarchLegacyHistory.efforts(logs)
        assertEquals(Adaptation.MAINTAIN,
            CategoryEffortPolicy.decide(observations, date.plusDays(1), category))
    }
}