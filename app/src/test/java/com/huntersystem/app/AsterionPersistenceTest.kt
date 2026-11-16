package com.huntersystem.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.huntersystem.app.asterion.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Comprueba que Asterion usa el AtomicFile original sin migrar valoraciones globales. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AsterionPersistenceTest {
    private lateinit var context: Context
    private val date = LocalDate.of(2026, 11, 15)
    @Before fun clean() {
        context = ApplicationProvider.getApplicationContext()
        context.filesDir.listFiles()?.filter { it.name.startsWith("hunter-system-native") }
            ?.forEach { it.delete() }
    }
    @Test fun participationIsIdempotentAndSurvivesReload() {
        val store = NativeStore(context)
        val repo = AsterionProgressRepository(store)
        val mission = DailyMission(date, Category.UPPER, "profile", "source",
            listOf("synthetic-id"), "test synthetic only")
        repo.pin(mission)
        repo.setParticipation(date, "synthetic-id", ParticipationStatus.COMPLETED)
        repo.setParticipation(date, "synthetic-id", ParticipationStatus.COMPLETED)
        assertEquals(1, repo.participation().size)
        val reopened = AsterionProgressRepository(NativeStore(context))
        assertEquals(1, reopened.participation().size)
        assertEquals(ParticipationStatus.COMPLETED, reopened.participation().single().status)
        assertEquals(mission, reopened.pinned(date))
    }
    @Test fun categoryRpeAndGlobalRpeRemainSeparate() {
        val store = NativeStore(context)
        val r = AsterionProgressRepository(store)
        r.addEffort(date, null, 5, completed = true)
        r.addEffort(date, Category.CARDIO, 8, completed = false)
        r.addEffort(date, Category.CARDIO, 9, completed = true)
        val reopened = AsterionProgressRepository(NativeStore(context))
        assertEquals(2, reopened.efforts().size)
        assertEquals(5, reopened.efforts().single { it.category == null }.rpe)
        assertEquals(9, reopened.efforts().single { it.category == Category.CARDIO }.rpe)
        val restored = NativeStore(context)
        assertTrue(restored.state.o("asterionV1").a("efforts").length() == 2)
    }
    @Test fun volumesUseTheirOriginalUnitsAndRetainPreviousBlocks() {
        val store = NativeStore(context)
        val r = AsterionProgressRepository(store)
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
        val restored = AsterionProgressRepository(NativeStore(context))
        assertEquals(2, restored.volume().size)
        assertEquals(setOf(VolumeUnit.SECONDS, VolumeUnit.REPETITIONS),
            restored.volume().map { it.unit }.toSet())
        assertTrue(restored.blocks().containsKey("synthetic-id"))
    }
    @Test fun routinePointsOnlyCountOnceAndRestoredBackupKeepsLegacy() {
        val store = NativeStore(context)
        store.state.put("legacyMarker", "kept")
        store.save()
        val r = AsterionProgressRepository(store)
        assertEquals(10, r.awardRoutine(date, true))
        assertEquals(0, r.awardRoutine(date, true))
        assertEquals(10, AsterionProgressRepository(NativeStore(context)).routinePoints())
        val restored = NativeStore(context)
        val exported = restored.export()
        restored.restore(org.json.JSONObject(exported))
        assertEquals("kept", restored.state.s("legacyMarker"))
        assertEquals(10, AsterionProgressRepository(restored).routinePoints())
    }
    @Test fun monarchConsentIsExplicitAndCycleScoped() {
        val store = NativeStore(context)
        val r = AsterionProgressRepository(store)
        assertFalse(r.monarchConsented("cycle-1"))
        r.setMonarchConsent("cycle-1", true)
        assertTrue(r.monarchConsented("cycle-1"))
        assertFalse(r.monarchConsented("cycle-2"))
        assertTrue(AsterionProgressRepository(NativeStore(context)).monarchConsented("cycle-1"))
    }
}
