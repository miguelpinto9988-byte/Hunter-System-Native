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
    private val date = LocalDate.of(2026, 11, 15)
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
        val r = MonarchProgressRepository(store)
        r.pin(DailyMission(date, Category.UPPER, "synthetic", "synthetic",
            listOf("synthetic-id"), "only test metadata"))
        r.setParticipation(date, "synthetic-id", ParticipationStatus.COMPLETED)
        val a = VolumeObservation("observation-a", date, Category.UPPER,
            "synthetic-id", "synthetic-type", 1, VolumeUnit.REPETITIONS,
            2, 8, 4, completed = true, interrupted = false, blockId = "block1")
        val b = a.copy(id = "observation-b", unit = VolumeUnit.SECONDS, blockId = "block2")
        r.recordVolume(a)
        r.recordVolume(a)
        r.recordVolume(b)
        r.startVolumeBlock(ActivityMetadata("synthetic-id", Category.UPPER,
            "family", "synthetic-type", 1, Intensity.LOW, emptySet(),
            "synthetic", false, true), Experience.BEGINNER, date)
        val restored = MonarchProgressRepository(NativeStore(context))
        assertEquals(2, restored.volume().size)
        assertEquals(setOf(VolumeUnit.SECONDS, VolumeUnit.REPETITIONS),
            restored.volume().map { it.unit }.toSet())
        assertTrue(restored.blocks().containsKey("synthetic-id"))
    }
    @Test fun routinePointsOnlyCountOnceAndRestoredBackupKeepsLegacy() {
        val store = NativeStore(context)
        store.state.put("legacyMarker", "kept")
        store.save()
        val r = MonarchProgressRepository(store)
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
