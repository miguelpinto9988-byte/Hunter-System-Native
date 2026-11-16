package com.huntersystem.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Synthetic metadata ONLY. No real exercise catalog is reproduced by these tests.
 */
class HunterProgressRulesTest {
    private val monday = LocalDate.parse("2026-11-02")
    private val family = HunterFamily("synthetic-family", HunterCategory.UPPER)
    private val type = HunterType("synthetic-type", family.id)
    private fun variant(id: String, level: Int = 1, gear: Set<String> = emptySet(),
        tags: Set<String> = emptySet(), intensity: HunterIntensity = HunterIntensity.BAJA,
        eligible: Boolean = true, supervised: Boolean = false
    ) = HunterVariant(id, type.id, HunterCategory.UPPER, level, intensity, gear, tags,
        supervisionRequired = supervised, automaticEligible = eligible)
    private fun catalog(vararg rows: HunterVariant): HunterCatalogPort =
        ValidatedHunterCatalog(listOf(family), listOf(type), rows.toList(), "synthetic-only")
    private fun settings(limit: Int = 3, experience: HunterExperience = HunterExperience.INTERMEDIO,
        goal: String = "", gear: Set<String> = setOf("body")) =
        HunterSettings(experience, monday.minusDays(21),
            listOf(HunterCategory.UPPER, HunterCategory.REST, HunterCategory.UPPER,
                HunterCategory.REST, HunterCategory.UPPER, HunterCategory.REST, HunterCategory.REST),
            gear, "general", goal, limit, HunterIntensity.MODERADA)

    @Test fun convertsHistoricalSundayFirstCalendarWithoutChangingStoredValues() {
        val p = JSONObject().put("difficulty","basic")
            .put("weekPlan",org.json.JSONArray(listOf("rest","train","cardio","mobility","rest","train","rest")))
            .put("weekFocus",org.json.JSONArray(listOf("upper","core","cardio","mobility","upper","lower","upper")))
        val before = p.toString()
        val cfg = HunterProgressBridge.settings(p,monday)
        assertEquals(HunterCategory.CORE,cfg.week[0])
        assertEquals(HunterCategory.CARDIO,cfg.week[1])
        assertEquals(HunterCategory.REST,cfg.week[6])
        assertEquals(before,p.toString())
    }

    @Test fun exactlyFiveCategoriesAndRestNotPartOfCatalog() {
        assertEquals(5, HunterCategory.activity.size)
        assertEquals(HunterCategory.CORE, HunterCategory.parse("integrado"))
        assertEquals(HunterCategory.CORE, HunterCategory.parse("tronco"))
        assertEquals(HunterCategory.LOWER, HunterCategory.parse("piernas"))
        assertEquals(HunterCategory.MOBILITY, HunterCategory.parse("coordinacion"))
        assertFalse(HunterCategory.REST in HunterCategory.activity)
        assertTrue(catalog(variant("test-a")).categories().size == 5)
    }

    @Test fun rejectsDuplicateAndBrokenRelations() {
        try {
            catalog(variant("dup"), variant("dup"))
            fail("Duplicate variant was accepted")
        } catch (_: IllegalArgumentException) {}
        try {
            ValidatedHunterCatalog(listOf(family), listOf(type),
                listOf(variant("invalid").copy(typeId = "missing")))
            fail("Unknown type was accepted")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun plannedRestDayNeverSuggestsAnActivity() {
        val plan = HunterProgressRules.propose(settings(), monday.plusDays(1),
            catalog(variant("test-a")), HunterHistory())
        assertTrue(plan.ids.isEmpty())
        assertEquals(HunterCategory.REST, plan.category)
    }

    @Test fun incompatibleEquipmentAndUnreviewedActivityYieldEmptyPlan() {
        val rows = catalog(variant("needs-mat", gear = setOf("mat")),
            variant("pending", eligible = false), variant("supervised", supervised = true),
            variant("level-four", level = 4), variant("level-zero", level = 0))
        val result = HunterProgressRules.propose(settings(), monday, rows, HunterHistory())
        assertTrue(result.ids.isEmpty())
        assertTrue(result.explanation.contains("compatibles"))
    }

    @Test fun onlyLevelsOneToThreeAndNoHighIntensityOrSupervision() {
        val rows = catalog(variant("one", level = 1),variant("two", level = 2),
            variant("three", level = 3),variant("four", level = 4),
            variant("high", intensity = HunterIntensity.ALTA))
        val beginner = HunterProgressRules.propose(settings(experience = HunterExperience.PRINCIPIANTE),
            monday, rows, HunterHistory())
        assertFalse("three" in beginner.ids)
        assertFalse("four" in beginner.ids)
        assertFalse("high" in beginner.ids)
        val advanced = HunterProgressRules.propose(settings(experience = HunterExperience.AVANZADO),
            monday, rows, HunterHistory())
        assertFalse("one" in advanced.ids)
        assertTrue(advanced.ids.all { it in listOf("two","three") })
    }

    @Test fun normalizedGoalsHandleNegationWithoutActivatingPermissions() {
        val positive = HunterProgressRules.interpret("Mejorar mi coordinación y andar en bicicleta")
        assertTrue(positive.recognized)
        assertTrue("coordinacion" in positive.tags)
        assertTrue("bicicleta" in positive.tags)
        val negative = HunterProgressRules.interpret("No quiero correr, prefiero movilidad")
        assertFalse("carrera" in negative.tags)
        assertTrue("movilidad" in negative.tags)
        val unknown = HunterProgressRules.interpret("Me gustaría una actividad diferente")
        assertFalse(unknown.recognized)
        assertEquals("Me gustaría una actividad diferente", unknown.original)
    }

    @Test fun sameInputsYieldSamePlanAndSameDayPinIgnoresNewEffort() {
        val rows = catalog(variant("a"),variant("b"),variant("c"),variant("d"))
        val first = HunterProgressRules.propose(settings(), monday, rows, HunterHistory())
        assertEquals(first, HunterProgressRules.propose(settings(), monday, rows, HunterHistory()))
        val laterEffort = HunterHistory(efforts = listOf(
            HunterEffort(monday, HunterCategory.UPPER, 9, true)))
        assertSame(first, HunterProgressRules.propose(settings(), monday, rows, laterEffort, first))
        val changed = HunterProgressRules.propose(settings(limit = 1), monday, rows, laterEffort, first)
        assertNotEquals(first.settingsHash, changed.settingsHash)
        assertEquals(1, changed.ids.size)
    }

    @Test fun highEffortControlsNextOccurrenceOnlyAndDoesNotCrossCategories() {
        val rows = catalog(variant("a"),variant("b"),variant("c"))
        val high = HunterHistory(efforts = listOf(
            HunterEffort(monday, HunterCategory.UPPER, 9, true),
            HunterEffort(monday.plusDays(1), HunterCategory.CARDIO, 2, true)))
        val nextUpper = HunterProgressRules.propose(settings(), monday.plusDays(2), rows, high)
        assertEquals(1, nextUpper.ids.size)
        assertEquals(9, HunterProgressRules.recentEffort(high,monday.plusDays(2),HunterCategory.UPPER)?.rpe)
        assertNull(HunterProgressRules.recentEffort(high,monday.plusDays(2),HunterCategory.CORE))
        val old = HunterProgressRules.recentEffort(high, monday.plusDays(23), HunterCategory.UPPER)
        assertNull(old)
    }

    @Test fun oneLowValueCannotPassWeeklyReviewButTwoRealDaysCan() {
        val first = HunterEffort(monday, HunterCategory.UPPER, 2, true)
        val one = HunterHistory(efforts = listOf(first))
        assertEquals(HunterReview.ESPERAR, HunterProgressRules.weekly(one,monday.plusDays(6),HunterCategory.UPPER).decision)
        assertEquals(HunterReview.INSUFICIENTE, HunterProgressRules.weekly(one,monday.plusDays(7),HunterCategory.UPPER).decision)
        val two = HunterHistory(efforts = listOf(first,
            HunterEffort(monday.plusDays(2), HunterCategory.UPPER, 3, true)))
        assertEquals(HunterReview.VARIAR,HunterProgressRules.weekly(two,monday.plusDays(7),HunterCategory.UPPER).decision)
        val notCompleted = HunterHistory(efforts = listOf(first,
            HunterEffort(monday.plusDays(2), HunterCategory.UPPER, 3, false)))
        assertEquals(HunterReview.INSUFICIENTE,
            HunterProgressRules.weekly(notCompleted,monday.plusDays(7),HunterCategory.UPPER).decision)
    }

    @Test fun recentHighEffortHasPriorityOverLowEffort() {
        val efforts = listOf(
            HunterEffort(monday, HunterCategory.UPPER, 2, true),
            HunterEffort(monday.plusDays(2), HunterCategory.UPPER, 2, true),
            HunterEffort(monday.plusDays(4), HunterCategory.UPPER, 8, true))
        assertEquals(HunterReview.RECUPERAR,
            HunterProgressRules.weekly(HunterHistory(efforts = efforts), monday.plusDays(7), HunterCategory.UPPER).decision)
    }

    @Test fun technicalMilestonesRequireEvidenceAndMinimumInterval() {
        val config = settings()
        val h = HunterHistory(efforts = listOf(
            HunterEffort(monday.minusDays(7), HunterCategory.UPPER, 2, true),
            HunterEffort(monday.minusDays(5), HunterCategory.UPPER, 3, true)))
        val first = HunterProgressRules.technical(h,config,monday,HunterCategory.UPPER)
        // Day 21 from chosen experience start; it is a review, not automatic advancement.
        assertEquals(HunterReview.AVANZAR_FASE,first.decision)
        assertEquals(1,first.nextPhase)
        val changed = h.copy(phaseChanges = listOf(HunterPhaseChange(HunterCategory.UPPER,1,monday)))
        assertEquals(HunterReview.VARIAR,
            HunterProgressRules.technical(changed,config,monday,HunterCategory.UPPER).decision)
        assertEquals(HunterReview.ESPERAR,
            HunterProgressRules.technical(h,config,monday.plusDays(1),HunterCategory.UPPER).decision)
    }

    @Test fun phaseRollbackWinsEvenWhenBothEventsAreOnSameDay() {
        val state = HunterHistory(phaseChanges = listOf(
            HunterPhaseChange(HunterCategory.UPPER,1,monday),
            HunterPhaseChange(HunterCategory.UPPER,0,monday)))
        assertEquals(0,state.phase(HunterCategory.UPPER))
    }

    @Test fun summaryUsesEvaluatedOnlyAndNoDataIsNotZero() {
        val a = HunterActivityRecord(monday,"a",HunterCategory.UPPER,HunterStatus.PENDING)
        val empty = HunterProgressRules.summary(listOf(a))
        assertNull(empty.percentage)
        assertEquals("Sin datos suficientes",empty.explanation)
        val two = HunterProgressRules.summary(listOf(a,
            a.copy(variantId="b",status=HunterStatus.COMPLETED),
            a.copy(variantId="c",status=HunterStatus.SKIPPED)))
        assertEquals(3,two.planned)
        assertEquals(50,two.percentage)
        val upsert = HunterHistory().record(a).record(a.copy(status=HunterStatus.COMPLETED))
        assertEquals(1, upsert.activities.size)
        assertEquals(HunterStatus.COMPLETED,upsert.activities.single().status)
    }

    @Test fun volumeNeverMixesUnitsAndGlobalLegacyStaysGlobal() {
        val first = HunterVolume(monday,HunterCategory.UPPER,"a","t",1,2,10.0,
            HunterUnit.REPETITIONS, 5,true,false)
        val second = first.copy(date=monday.plusDays(2),unit=HunterUnit.SECONDS,amountPerSet=30.0)
        val h = HunterHistory().recordVolume(first).recordVolume(second)
        assertEquals(20.0,h.total("a",HunterUnit.REPETITIONS)!!,0.01)
        assertEquals(60.0,h.total("a",HunterUnit.SECONDS)!!,0.01)
        val historic = JSONObject().put("2026-11-02",JSONObject().put("rpe",6)
            .put("complete",true).put("trainingGroup","upper"))
        val restored = HunterProgressBridge.history(historic)
        assertNull(restored.efforts.single().category)
    }

    @Test fun globalReviewIsDescriptiveAndNoDataHasNoAverage() {
        val empty=HunterProgressRules.global(HunterHistory(),monday,monday.plusDays(10))
        assertNull(empty.mean)
        assertEquals(14,empty.nextMilestone)
        assertEquals(11,empty.dayOfCycle)
    }
}
