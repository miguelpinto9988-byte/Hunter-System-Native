package com.huntersystem.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.huntersystem.app.planning.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Comprueba que Monarca usa el AtomicFile original sin migrar valoraciones globales. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MonarchPlanningPersistenceTest {
    private lateinit var context: Context
    private val date = LocalDate.now().minusDays(1)
    @Before fun clean() {
        context = ApplicationProvider.getApplicationContext()
        context.filesDir.listFiles()?.filter { it.name.startsWith("hunter-system-native") }
            ?.forEach { it.delete() }
    }
    @Test fun participationIsIdempotentAndSurvivesReload() {
        val store = NativeStore(context)
        val repo = MonarchProgressRepository(store)
        val mission = DailyMission(date, Category.UPPER, "profile", "source",
            listOf("synthetic-id"), "test synthetic only")
        repo.pin(mission)
        repo.setParticipation(date, "synthetic-id", ParticipationStatus.COMPLETED)
        repo.setParticipation(date, "synthetic-id", ParticipationStatus.COMPLETED)
        assertEquals(1, repo.participation().size)
        val reopened = MonarchProgressRepository(NativeStore(context))
        assertEquals(1, reopened.participation().size)
        assertEquals(ParticipationStatus.COMPLETED, reopened.participation().single().status)
        assertEquals(mission, reopened.pinned(date))
    }
    @Test fun categoryRpeAndGlobalRpeRemainSeparate() {
        val store = NativeStore(context)
        val r = MonarchProgressRepository(store)
        r.addEffort(date, null, 5, completed = true)
        r.addEffort(date, Category.CARDIO, 8, completed = false)
        r.addEffort(date, Category.CARDIO, 9, completed = true)
        val reopened = MonarchProgressRepository(NativeStore(context))
        assertEquals(2, reopened.efforts().size)
        assertEquals(5, reopened.efforts().single { it.category == null }.rpe)
        assertEquals(9, reopened.efforts().single { it.category == Category.CARDIO }.rpe)
        val restored = NativeStore(context)
        assertTrue(restored.state.o("monarcaPlanningV2").a("efforts").length() == 2)
    }
    @Test fun volumesUseTheirOriginalUnitsAndRetainPreviousBlocks() {
        val store = NativeStore(context)
        val reps = ActivityMetadata("synthetic-reps", Category.UPPER,
            "synthetic-family", "synthetic-type", 1, Intensity.LOW, emptySet(),
            "synthetic", false, true, workUnit = VolumeUnit.REPETITIONS)
        val seconds = reps.copy(id = "synthetic-seconds", workUnit = VolumeUnit.SECONDS)
        val r = MonarchProgressRepository(store) { id ->
            listOf(reps, seconds).firstOrNull { it.id == id }
        }
        r.pin(DailyMission(date, Category.UPPER, "synthetic", "synthetic",
            listOf(reps.id, seconds.id), "only test metadata"))
        r.setParticipation(date, reps.id, ParticipationStatus.COMPLETED)
        r.setParticipation(date, seconds.id, ParticipationStatus.COMPLETED, interrupted = true)
        val blockA = VolumeResetPolicy.block(Category.UPPER, reps.id, Experience.BEGINNER, date)
        val blockB = VolumeResetPolicy.block(Category.UPPER, seconds.id, Experience.BEGINNER, date)
        r.startVolumeBlock(reps, Experience.BEGINNER, date)
        r.startVolumeBlock(seconds, Experience.BEGINNER, date)
        val a = VolumeObservation("observation-a", date, Category.UPPER,
            reps.id, reps.typeId, 1, VolumeUnit.REPETITIONS,
            2, 8, 4, completed = true, interrupted = false, blockId = blockA)
        val b = a.copy(id = "observation-b", variantId = seconds.id,
            unit = VolumeUnit.SECONDS, blockId = blockB)
        assertTrue(runCatching { r.recordVolume(a.copy(unit = VolumeUnit.SECONDS)) }.isFailure)
        r.recordVolume(a)
        r.recordVolume(a.copy(id = "duplicate-submission"))
        r.recordVolume(b)
        r.startVolumeBlock(reps, Experience.BEGINNER, date.plusDays(1))
        val restored = MonarchProgressRepository(NativeStore(context))
        assertTrue(restored.volume().single { it.variantId == seconds.id }.interrupted)
        assertNotEquals(blockA, restored.blocks()[reps.id])
        assertEquals(blockA, restored.volume().single { it.variantId == reps.id }.blockId)
        assertEquals(2, restored.volume().size)
        assertEquals(setOf(VolumeUnit.SECONDS, VolumeUnit.REPETITIONS),
            restored.volume().map { it.unit }.toSet())
    }
    @Test fun optionalReferenceIsPersistedOnlyAfterExplicitConsentAndIsBlockScoped() {
        val store = NativeStore(context)
        val today = LocalDate.now()
        val fixture = org.json.JSONArray(javaClass.classLoader!!
            .getResource("groups-golden.json")!!.readText()).objects()[7].o("p")
        store.saveProfile(fixture.copyJson().put("name", "Perfil sintético de pruebas"))
        val activity = ActivityMetadata("synthetic-volume", Category.UPPER,
            "synthetic-family", "synthetic-type", 1, Intensity.LOW, emptySet(),
            "synthetic", false, true, workUnit = VolumeUnit.REPETITIONS)
        val lookup: (String) -> ActivityMetadata? = { activity.takeIf { row -> row.id == it } }
        val repository = MonarchProgressRepository(store, lookup)
        val profile = MonarchProfileAdapter.from(store)!!
        val block = repository.currentVolumeBlock(activity, profile)
        val r = store.state.o("monarcaPlanningV2")
        r.put("validatedVolumeProfiles", obj(activity.id to obj(
            "variantId" to activity.id, "validated" to true, "sourceId" to "synthetic-source",
            "sourceRevision" to "test", "unit" to "REPETITIONS", "initialSets" to 2,
            "initialValue" to 8, "minimum" to 3, "maximum" to 15,
            "permittedIncrement" to 2, "automaticReviewEligible" to true)))
        store.state.put("monarcaPlanningV2", r)
        store.save()
        listOf(today.minusDays(3), today.minusDays(1)).forEach { day ->
            repository.pin(DailyMission(day, Category.UPPER, "synthetic", "synthetic",
                listOf(activity.id), "test only"))
            repository.setParticipation(day, activity.id, ParticipationStatus.COMPLETED)
            repository.recordVolume(VolumeObservation("synthetic:$day", day, Category.UPPER,
                activity.id, activity.typeId, activity.technicalLevel, VolumeUnit.REPETITIONS,
                2, 8, 2, true, false, block))
        }
        assertTrue(repository.volumeRecommendation(today, activity.id).optional)
        assertNull(repository.acceptedReference(activity.id))
        assertTrue(repository.acceptVolumeReference(today, activity.id))
        assertFalse(repository.acceptVolumeReference(today, activity.id))
        val reopened = MonarchProgressRepository(NativeStore(context), lookup)
        assertEquals(VolumeReference(10, 2, VolumeUnit.REPETITIONS), reopened.acceptedReference(activity.id))
        assertTrue(runCatching { reopened.saveVolumeReference(today,
            activity.id, VolumeReference(99, 2, VolumeUnit.REPETITIONS)) }.isFailure)
        reopened.startVolumeBlock(activity, profile.experience, today.plusDays(1))
        assertNull(reopened.acceptedReference(activity.id))
        assertEquals(2, reopened.volume().size)
    }
    @Test fun routinePointsOnlyCountOnceAndRestoredBackupKeepsLegacy() {
        val store = NativeStore(context)
        store.state.put("legacyMarker", "kept")
        store.save()
        val r = MonarchProgressRepository(store)
        assertEquals(0, r.awardRoutine(date, true))
        store.mutableDay(date.toString()).put("routineId", "synthetic-routine")
        store.state.put("logs", obj(date.toString() to obj("complete" to true,
            "items" to arr(listOf(obj("id" to store.catalog.exercises.first().id, "sets" to 1, "target" to 1,
                "progressStatus" to "SKIPPED"))))))
        assertEquals(0, r.awardRoutine(date, true))
        store.logs.o(date.toString()).a("items").getJSONObject(0).put("progressStatus", "COMPLETED")
        assertEquals(0, r.awardRoutine(date, false))
        assertEquals(10, r.awardRoutine(date, true))
        assertEquals(0, r.awardRoutine(date, true))
        assertEquals(10, MonarchProgressRepository(NativeStore(context)).routinePoints())
        val restored = NativeStore(context)
        val exported = restored.export()
        restored.restore(org.json.JSONObject(exported))
        assertEquals("kept", restored.state.s("legacyMarker"))
        assertEquals(10, MonarchProgressRepository(restored).routinePoints())
    }
    @Test fun legacyPlanningStorageIsReadWithoutDeletingPreviousData() {
        val store = NativeStore(context)
        val legacy = store.state.o("asterionV1")
        legacy.put("routineDays", arr(listOf(date.toString())))
        legacy.put("unknownHistoricValue", "must-survive")
        store.state.put("asterionV1", legacy)
        store.save()
        val repo = MonarchProgressRepository(store)
        assertEquals(10, repo.routinePoints())
        repo.addEffort(date, null, 4, completed = false)
        assertEquals(10, MonarchProgressRepository(NativeStore(context)).routinePoints())
        val restored = NativeStore(context)
        assertEquals("must-survive", restored.state.o("asterionV1").s("unknownHistoricValue"))
        assertEquals(1, restored.state.o("monarcaPlanningV2").a("efforts").length())
    }
    @Test fun monarchConsentIsExplicitAndCycleScoped() {
        val store = NativeStore(context)
        val r = MonarchProgressRepository(store)
        assertFalse(r.monarchConsented("cycle-1"))
        r.setMonarchConsent("cycle-1", true)
        assertTrue(r.monarchConsented("cycle-1"))
        assertFalse(r.monarchConsented("cycle-2"))
        assertTrue(MonarchProgressRepository(NativeStore(context)).monarchConsented("cycle-1"))
    }
}
