package com.huntersystem.app.planning

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/** Datos sintéticos solo para pruebas: no forman parte del catálogo de producción. */
class MonarchPlanningRulesTest {
    private val date = LocalDate.of(2026, 11, 16)
    private fun profile(week: List<Int> = List(7) { 0 }, experience: Experience = Experience.INTERMEDIATE,
                        equipment: Set<String> = emptySet(), goal: String = "") =
        TrainingProfile(experience, date.minusDays(21), week, equipment = equipment,
            customGoal = goal, dailyLimit = 4)
    private fun activity(id: String, category: Category = Category.UPPER, level: Int = 1,
                         family: String = "family-" + id, type: String = "type-" + id,
                         gear: Set<String> = emptySet(), intensity: Intensity = Intensity.LOW,
                         eligible: Boolean = true, supervisor: Boolean = false,
                         tags: Set<String> = emptySet()) =
        ActivityMetadata(id, category, family, type, level, intensity, gear, "synthetic-test",
            supervisor, eligible, tags)
    private class FakeRepo(private val rows: List<ActivityMetadata>) : ActivityRepositoryInterface {
        override val revision: String = "synthetic-tests-v1"
        override fun all(): List<ActivityMetadata> = rows
    }
    private fun effort(day: Int, category: Category, rpe: Int, completed: Boolean = true,
                       interruption: Boolean = false) =
        EffortObservation(date.minusDays(day.toLong()), category, rpe, completed, interruption)
    private fun volume(id: String, daysAgo: Long, rpe: Int, sets: Int = 2,
                       unit: VolumeUnit = VolumeUnit.REPETITIONS, block: String = "one",
                       interrupted: Boolean = false) =
        VolumeObservation(id + daysAgo, date.minusDays(daysAgo), Category.UPPER, "a",
            "type-a", 1, unit, sets, 10, rpe, true, interrupted, block)
    private val meta get() = activity("a", type = "type-a")
    private val reference get() =
        VolumeProfile("a", VolumeUnit.REPETITIONS, 2, 10, 5, 20, 2, true)

    @Test fun fiveCategoriesAndRestAreStable() {
        assertEquals(listOf("tren_superior", "tren_central", "tren_inferior",
            "movilidad", "cardio"), CategoryMapper.codes)
        assertNull(CategoryMapper.day(5))
        assertEquals(Category.CENTRAL, CategoryMapper.day(1))
        assertTrue(DailyMissionPlanner.plan(date, profile(List(7) { 5 }),
            FakeRepo(listOf(meta)), emptyList()).ids.isEmpty())
    }
    @Test fun historicalAliasesNeverRewriteIds() {
        assertEquals(Category.CENTRAL, CategoryMapper.normalize("tronco"))
        assertEquals(Category.CENTRAL, CategoryMapper.normalize("integrado"))
        assertEquals(Category.LOWER, CategoryMapper.normalize("piernas"))
        assertEquals(Category.MOBILITY, CategoryMapper.normalize("coordinacion"))
        assertEquals("a", meta.id)
    }
    @Test fun duplicatesAndBrokenRelationshipsAreRejected() {
        try { ActivityCatalogValidator.verify(listOf(meta, meta)); fail("duplicate") }
        catch (_: IllegalArgumentException) {}
        try {
            ActivityCatalogValidator.verify(listOf(activity("a", type = "common"),
                activity("b", category = Category.CARDIO, type = "common")))
            fail("cross-category type")
        } catch (_: IllegalArgumentException) {}
    }
    @Test fun equipmentSupervisionAndUnsupportedLevelsFailClosed() {
        val rows = listOf(activity("mat", gear = setOf("mat")), activity("blocked", eligible = false),
            activity("supervisor", supervisor = true), activity("zero", level = 0),
            activity("four", level = 4), activity("five", level = 5),
            activity("unknown", intensity = Intensity.UNKNOWN), activity("safe"))
        val result = DailyMissionPlanner.plan(date, profile(), FakeRepo(rows), emptyList())
        assertEquals(listOf("safe"), result.ids)
    }
    @Test fun unknownGoalAndNegationHaveNoPositiveAffinity() {
        assertFalse(PersonalGoalInterpreter.interpret("Me gusta el ajedrez").recognized)
        assertFalse(PersonalGoalInterpreter.interpret("No quiero correr").recognized)
        assertEquals(setOf("caminar"), PersonalGoalInterpreter.interpret(
            "No deseo correr, pero quiero caminar").tags)
        assertEquals(setOf("movilidad"), PersonalGoalInterpreter.interpret("MOVILIDÁD").tags)
    }
    @Test fun writtenGoalChangesOnlyCompatibleOrder() {
        val rows = listOf(activity("z", tags = setOf("caminar")),
            activity("a", tags = setOf("bicicleta")), activity("blocked",
                gear = setOf("gym"), tags = setOf("caminar")))
        val plan = DailyMissionPlanner.plan(date,
            profile(goal = "Quiero caminar"), FakeRepo(rows), emptyList())
        assertEquals("z", plan.ids.first())
        assertFalse("blocked" in plan.ids)
    }
    @Test fun dailyPinIsStableAndProfileChangeInvalidates() {
        val rows = listOf(activity("a"), activity("b"))
        val repository = FakeRepo(rows)
        val p = profile()
        val a = DailyMissionPlanner.plan(date, p, repository, emptyList())
        val b = DailyMissionPlanner.plan(date, p, repository,
            listOf(effort(1, Category.UPPER, 10)), pinned = a)
        assertEquals(a, b)
        val changed = p.copy(equipment = setOf("mat"))
        val c = DailyMissionPlanner.plan(date, changed, repository, emptyList(), pinned = a)
        assertNotEquals(a.profileHash, c.profileHash)
    }
    @Test fun missingCompatibleActivitiesMeansEmptyMission() {
        val result = DailyMissionPlanner.plan(date, profile(),
            FakeRepo(listOf(activity("no", level = 5))), emptyList())
        assertTrue(result.ids.isEmpty())
        assertTrue(result.explanation.contains("No existen"))
    }
    @Test fun categoriesNeverShareEffort() {
        val records = listOf(effort(1, Category.CARDIO, 10))
        assertEquals(Adaptation.MAINTAIN,
            CategoryEffortPolicy.decide(records, date, Category.UPPER))
        assertEquals(Adaptation.RECOVER_ONE,
            CategoryEffortPolicy.decide(records, date, Category.CARDIO))
        assertEquals(Adaptation.MAINTAIN,
            CategoryEffortPolicy.decide(listOf(EffortObservation(
                date.minusDays(1), null, 10, true)), date, Category.CARDIO))
    }
    @Test fun eightReducesTwoNineReducesOneAndSimplifies() {
        val rows = (1..5).map { activity(it.toString(), level = if (it == 1) 1 else 2) }
        val repo = FakeRepo(rows)
        val p = profile()
        assertEquals(2, DailyMissionPlanner.plan(date, p, repo,
            listOf(effort(1, Category.UPPER, 8))).ids.size)
        val recovery = DailyMissionPlanner.plan(date, p, repo,
            listOf(effort(1, Category.UPPER, 9)))
        assertEquals(listOf("1"), recovery.ids)
    }
    @Test fun recentHighSignalOverridesLaterLowReadingWithinSameCategory() {
        val readings = listOf(effort(5, Category.UPPER, 9),
            effort(1, Category.UPPER, 2),
            effort(1, Category.CARDIO, 2))
        assertEquals(Adaptation.RECOVER_ONE,
            CategoryEffortPolicy.decide(readings, date, Category.UPPER))
        assertEquals(Adaptation.EQUIVALENT_VARIATION,
            CategoryEffortPolicy.decide(readings, date, Category.CARDIO))
    }
    @Test fun staleEffortIsIgnoredAfterTwentyOneDays() {
        assertEquals(Adaptation.MAINTAIN, CategoryEffortPolicy.decide(
            listOf(effort(22, Category.UPPER, 10)), date, Category.UPPER))
    }
    @Test fun weeklyReviewRequiresTwoCompletedDifferentDays() {
        val recent = listOf(effort(6, Category.UPPER, 2),
            effort(2, Category.UPPER, 2, completed = false))
        assertNotEquals(WeeklyDecision.EQUIVALENT_VARIATION,
            WeeklyCategoryChallengePolicy.review(recent, date, Category.UPPER))
        val complete = recent + effort(1, Category.UPPER, 3)
        assertEquals(WeeklyDecision.EQUIVALENT_VARIATION,
            WeeklyCategoryChallengePolicy.review(complete + effort(10, Category.CARDIO, 5),
                date, Category.UPPER))
        assertEquals(WeeklyDecision.RECOVERY,
            WeeklyCategoryChallengePolicy.review(complete + effort(1, Category.UPPER, 9),
                date, Category.UPPER))
    }
    @Test fun checkpointDaysDoNotAdvanceByTimeAlone() {
        val p = profile().copy(experienceStart = date.minusDays(7))
        val result = TechnicalCheckpointPolicy.review(p, Category.UPPER, date,
            emptyList(), TechnicalPhase())
        assertEquals(TechnicalDecision.MAINTAIN, result.decision)
        assertEquals(0, result.phase.phase)
        assertEquals(TechnicalDecision.WAIT,
            TechnicalCheckpointPolicy.review(p.copy(experienceStart = date.minusDays(8)),
                Category.UPPER, date, emptyList(), TechnicalPhase()).decision)
    }
    @Test fun checkpointsSevenFourteenTwentyOneAndPhaseCeiling() {
        val evidence = listOf(effort(6, Category.UPPER, 2), effort(1, Category.UPPER, 2),
            effort(15, Category.CARDIO, 4))
        for (days in listOf(7L, 14L, 21L)) {
            val p = profile().copy(experienceStart = date.minusDays(days))
            val result = TechnicalCheckpointPolicy.review(p, Category.UPPER, date,
                evidence, TechnicalPhase())
            assertEquals(days, result.day)
            assertEquals(TechnicalDecision.ADVANCE_PHASE, result.decision)
            val atMax = TechnicalCheckpointPolicy.review(p, Category.UPPER, date,
                evidence, TechnicalPhase(2))
            assertEquals(TechnicalDecision.ROTATE_EQUIVALENT, atMax.decision)
            assertEquals(2, atMax.phase.phase)
        }
        assertEquals(0, MissionDifficultyMixPolicy.levelThreeTarget(Experience.BEGINNER, 2))
        assertEquals(60, MissionDifficultyMixPolicy.levelThreeTarget(Experience.INTERMEDIATE, 2))
        assertEquals(75, MissionDifficultyMixPolicy.levelThreeTarget(Experience.ADVANCED, 2))
    }
    @Test fun progressiveRotationKeepsOneStretchAndNeverRepeatsAFamily() {
        val rows = listOf(activity("base-a", family = "a"),
            activity("base-a-variant", family = "a"), activity("base-b"),
            activity("hard-a", level = 2), activity("hard-b", level = 2),
            activity("too-hard", level = 3))
        val beginner = profile(experience = Experience.BEGINNER)
        val repo = FakeRepo(rows)
        val baseline = DailyMissionPlanner.plan(date, beginner, repo, emptyList())
        assertEquals(2, baseline.ids.size)
        assertTrue(baseline.ids.all { it.startsWith("base-") })
        val priority = mapOf("hard-a" to 100, "hard-b" to 100)
        val progressed = DailyMissionPlanner.plan(date, beginner, repo, emptyList(),
            historicalPriority = priority, progressionReady = true)
        assertEquals(1, progressed.ids.count { it.startsWith("hard-") })
        assertTrue(progressed.ids.first().startsWith("base-"))
        val recovery = DailyMissionPlanner.plan(date, beginner, repo,
            listOf(effort(1, Category.UPPER, 10)), historicalPriority = priority,
            progressionReady = true)
        assertEquals(1, recovery.ids.size)
        assertTrue(recovery.ids.single().startsWith("base-"))
    }

    @Test fun rotationChangesAcrossVariationsAndKeepsAPinnedMission() {
        val rows = (1..12).map { activity("item-$it") }
        val repo = FakeRepo(rows)
        val p = profile()
        val choices = (0..15).map {
            DailyMissionPlanner.plan(date, p, repo, emptyList(), variation = "$it")
        }
        assertTrue(choices.map { it.ids.toSet() }.distinct().size > 1)
        assertEquals(choices.first(), DailyMissionPlanner.plan(date, p, repo, emptyList(),
            pinned = choices.first(), variation = "another"))
    }

    @Test fun routinesHaveValidDurationsStepsAndDeterministicVariants() {
        val pool = (1..12).map { activity("synthetic-" + it) }
        val repo = FakeRepo(pool)
        val req = RoutineRequest(Category.UPPER, 20, RoutineLevel.BASIC, date, "variation")
        val a = RoutineGenerator.generate(req, profile(), repo)
        assertEquals(a, RoutineGenerator.generate(req, profile(), repo))
        assertTrue(a.size in 1..6)
        assertTrue(a.all { it.ids.size == 4 })
        assertEquals(3, RoutineGenerator.generate(req.copy(duration = 10),
            profile(), repo).first().ids.size)
        assertEquals(6, RoutineGenerator.generate(req.copy(duration = 60),
            profile(), repo).first().ids.size)
        assertEquals(a.size, a.map { it.ids }.distinct().size)
    }
    @Test fun routineFiltersNeverUseHigherLevelsOrMissingGear() {
        val repo = FakeRepo(listOf(activity("one"), activity("three", level = 3),
            activity("gym", gear = setOf("gym"))))
        val proposals = RoutineGenerator.generate(
            RoutineRequest(Category.UPPER, 15, RoutineLevel.BASIC, date, "x"), profile(), repo)
        assertTrue(proposals.all { it.ids == listOf("one") })
    }
    @Test fun participationAndSummaryAreExplicitAndIdempotent() {
        val pending = Participation(date, "one", Category.UPPER, ParticipationStatus.PENDING)
        assertNull(ProgressSummary.calculate(listOf(pending)).percent)
        assertEquals("Sin datos suficientes", ProgressSummary.calculate(listOf(pending)).label)
        val done = pending.copy(status = ParticipationStatus.COMPLETED)
        val history = ProgressTrackingService.record(
            ProgressTrackingService.record(listOf(pending), done), done)
        assertEquals(1, history.size)
        assertEquals(100, ProgressSummary.calculate(history).percent)
        val skipped = pending.copy(variantId = "other", status = ParticipationStatus.SKIPPED)
        assertEquals(50, ProgressSummary.calculate(history + skipped + pending).percent)
    }
    @Test fun volumeRejectsMixedUnitsAndVariantInheritance() {
        val history = listOf(volume("a", 2, 2), volume("a", 9, 2))
        val other = history.map { it.copy(unit = VolumeUnit.SECONDS) }
        assertEquals(VolumeDecision.NO_HISTORY,
            VolumeProgressionEngine.review(date, meta, reference, other, "one").decision)
        assertEquals(VolumeDecision.NO_HISTORY,
            VolumeProgressionEngine.review(date, meta, reference, history, "new-block").decision)
    }
    @Test fun volumeUsesValidatedInitialReferenceOrNoData() {
        val empty = VolumeProgressionEngine.review(date, meta, reference, emptyList(), "one")
        assertEquals(VolumeDecision.NO_HISTORY, empty.decision)
        assertEquals(10, empty.reference)
        val unknown = VolumeProgressionEngine.review(date, meta, null, emptyList(), "one")
        assertNull(unknown.reference)
        assertEquals(VolumeDecision.GUIDED_REVIEW, unknown.decision)
    }
    @Test fun volumeOptionalIncreaseNeedsEvidenceAndConsent() {
        val history = listOf(volume("a", 8, 2), volume("a", 1, 2))
        val rec = VolumeProgressionEngine.review(date, meta, reference, history, "one")
        assertEquals(VolumeDecision.OPTIONAL_INCREASE, rec.decision)
        assertTrue(rec.optional)
        assertEquals(12, rec.reference)
        assertEquals(2, rec.sets)
        assertEquals(VolumeDecision.MAINTAIN, VolumeProgressionEngine.review(
            date, meta, reference, listOf(volume("a", 1, 1)), "one").decision)
    }
    @Test fun highEffortOrInterruptionStopsIncrease() {
        val low = listOf(volume("a", 9, 1), volume("a", 2, 2))
        val high = VolumeProgressionEngine.review(date, meta, reference, low, "one",
            listOf(effort(1, Category.UPPER, 9)))
        assertEquals(VolumeDecision.RECOVERY_PRIORITY, high.decision)
        assertEquals(VolumeDecision.PAUSED, VolumeProgressionEngine.review(date, meta,
            reference, low + volume("a", 1, 2, interrupted = true), "one").decision)
        assertEquals(VolumeDecision.REDUCE, VolumeProgressionEngine.review(date,
            meta, reference, low + volume("a", 1, 8), "one").decision)
    }
    @Test fun extraSetNeedsThreeDistinctLowEffortDaysSeparatedBySevenDays() {
        val history = listOf(volume("a", 12, 2), volume("a", 8, 3), volume("a", 2, 2))
        val rec = VolumeProgressionEngine.review(date, meta, reference, history, "one")
        assertEquals(VolumeDecision.REVIEW_EXTRA_SET, rec.decision)
        assertEquals(3, rec.sets)
        assertEquals(10, rec.reference)
        assertEquals(VolumeDecision.OPTIONAL_INCREASE, VolumeProgressionEngine.review(
            date, meta, reference, history.take(2), "one").decision)
    }
    @Test fun volumeRespectsBoundsAndManualReview() {
        val capped = reference.copy(maximum = 10, initialValue = 10)
        assertEquals(VolumeDecision.MAINTAIN, VolumeProgressionEngine.review(date, meta,
            capped, listOf(volume("a", 7, 2), volume("a", 1, 2)), "one").decision)
        val manual = reference.copy(automaticReviewEligible = false)
        assertEquals(VolumeDecision.GUIDED_REVIEW,
            VolumeProgressionEngine.review(date, meta, manual,
                listOf(volume("a", 1, 2)), "one").decision)
    }
    @Test fun referenceResetPreservesOldRecords() {
        val history = listOf(volume("a", 1, 2))
        val newBlock = VolumeResetPolicy.block(Category.UPPER, "a",
            Experience.INTERMEDIATE, date)
        assertNotEquals("one", newBlock)
        assertEquals(1, history.size)
        assertEquals(VolumeDecision.NO_HISTORY,
            VolumeProgressionEngine.review(date, meta, reference, history, newBlock).decision)
    }
    @Test fun routinePointsAreOncePerLocalDate() {
        val first = RoutineParticipation.award(emptySet(), date, true)
        assertEquals(10, first.second)
        assertEquals(0, RoutineParticipation.award(first.first, date, true).second)
        assertEquals(0, RoutineParticipation.award(first.first, date.plusDays(1), false).second)
    }
    @Test fun monarchPreferenceOnlyChangesSafeOrdering() {
        val p = profile()
        val rows = listOf(activity("a"), activity("b"), activity("c"), activity("d"))
        val repo = FakeRepo(rows)
        val base = MonarchChallengePlanner.propose(date, true, p, repo, emptyList())
        val focused = MonarchChallengePlanner.propose(
            date, true, p, repo, emptyList(), listOf(Category.UPPER),
            previousUsed = setOf("a"))
        assertEquals(base.days.map { it.category }, focused.days.map { it.category })
        assertEquals(base.days.map { it.ids.toSet() }, focused.days.map { it.ids.toSet() })
        assertTrue(focused.days.all {
            "a" !in it.ids || it.ids.last() == "a"
        })
        assertEquals(focused, MonarchChallengePlanner.propose(
            date, true, p, repo, emptyList(), listOf(Category.UPPER),
            previousUsed = setOf("a")))
    }
    @Test fun monarchNeedsConsentAndPreservesRest() {
        val p = profile(listOf(5, 0, 5, 0, 5, 0, 5))
        val repo = FakeRepo(listOf(meta))
        assertTrue(MonarchChallengePlanner.propose(date, false, p, repo,
            emptyList()).days.isEmpty())
        val plan = MonarchChallengePlanner.propose(date, true, p, repo, emptyList())
        assertEquals(5, plan.days.size)
        assertTrue(plan.days.first().ids.isEmpty())
        assertTrue(MonarchChallengePlanner.propose(date, true,
            profile(List(7) { 5 }), repo, emptyList()).days.isEmpty())
    }
    @Test fun sourceAndProfileHashesAreDeterministic() {
        val p = profile(goal = "Resistencia")
        assertEquals(ProfileSignature.of(p), ProfileSignature.of(p))
        assertNotEquals(ProfileSignature.of(p), ProfileSignature.of(p.copy(customGoal = "Caminar")))
        assertEquals(ProfileSignature.source(listOf(meta, activity("b"))),
            ProfileSignature.source(listOf(activity("b"), meta)))
    }
    @Test fun globalReviewDoesNotInventTrend() {
        val empty = EffortReviewEngine.review(date.minusDays(21), date, emptyList())
        assertNull(empty.average)
        assertEquals(1, empty.cycleDay)
        val withRating = EffortReviewEngine.review(date.minusDays(21), date,
            listOf(EffortObservation(date, Category.UPPER, 5, true)))
        assertEquals(5.0, withRating.average!!, 0.00001)
    }
}
