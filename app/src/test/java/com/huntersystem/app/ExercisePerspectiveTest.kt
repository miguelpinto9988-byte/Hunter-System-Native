package com.huntersystem.app

import android.content.Context
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExercisePerspectiveTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun frontExerciseAndFloorExercisesUseTheirOwnProjection() {
        val jumpingJack = checkNotNull(ExerciseMotions.load(context, "jumpjack"))
        assertEquals(ExerciseView.FRONT, jumpingJack.view)
        assertFalse(jumpingJack.handSupport)

        for (id in listOf("home-pushwide30", "home-push-one23")) {
            val motion = checkNotNull(ExerciseMotions.load(context, id))
            assertEquals(ExerciseView.OVERHEAD, motion.view)
            assertTrue(motion.prone)
            assertTrue(motion.handSupport)
        }
        for (id in listOf("pushup", "kneepush", "home-scappush30")) {
            val motion = checkNotNull(ExerciseMotions.load(context, id))
            assertEquals(ExerciseView.SIDE, motion.view)
            assertTrue(motion.prone)
            assertTrue(motion.handSupport)
        }
    }
    @Test fun sportsExpansionLoadsFromTheCatalogWithAllThreeDifficultyLevels() {
        val catalog = Catalog(context)
        val expansion = catalog.exercises.filter { it.id.startsWith("sport-ampl-") }
        val disciplines = setOf("Calistenia", "Fútbol", "Atletismo", "Movilidad", "Fuerza")
        assertEquals(disciplines, expansion.map { it.sport }.toSet())
        for ((_, group) in expansion.groupBy { it.sport }) {
            assertEquals(setOf(0, 1, 2), group.map { it.planningLevel }.toSet())
        }
        for (exercise in expansion) {
            assertFalse(exercise.validationBlocked)
            val motion = checkNotNull(ExerciseMotions.load(context, exercise.id)) { exercise.id }
            assertTrue(motion.frames.distinct().size > 1)
            val framing = ExerciseFraming.from(motion)
            assertTrue(framing.scaleFor(Size(200f, 320f)) > 0f)
        }
    }

    @Test fun everyStandaloneExerciseLoadsThroughTheApprovedRenderer() {
        val catalog = Catalog(context)
        val standalone = catalog.exercises.filter { it.raw.a("sequenceParts").length() == 0 }
        assertEquals(2_031, standalone.size)
        for (exercise in standalone) {
            val motion = checkNotNull(ExerciseMotions.load(context, exercise.id)) { exercise.id }
            assertTrue(exercise.id, ExerciseFraming.from(motion).scaleFor(Size(320f, 220f)) > 0f)
        }
    }

    @Test fun everyNewSequenceLoadsWithItsOwnPerspectiveAndACommonScale() {
        val catalog = Catalog(context)
        assertEquals(5_000, catalog.exercises.size)
        assertEquals("new46-tke", catalog.byId.getValue("hx_lower_0019").id)
        assertTrue(catalog.exercises.none { it.id == "hx_lower_0019" })
        val sequences = catalog.exercises.filter { it.id.startsWith("sport-seq-") }
        assertEquals(2_969, sequences.size)
        val pairs = mutableSetOf<List<String>>()
        val replacements = mapOf("Fuerza" to listOf(0), "Calistenia" to listOf(1, 2),
            "Atletismo" to listOf(1))
        for ((discipline, exercises) in sequences.groupBy { it.sport }) {
            val levels = listOf(208, 223, 162).mapIndexed { index, count ->
                index to (count + if (index in replacements[discipline].orEmpty()) 1 else 0)
            }.toMap()
            assertEquals(levels, exercises.groupingBy { it.planningLevel }.eachCount())
        }
        assertEquals("sport-ampl-pushup-staggered-hands", catalog.byId.getValue("home-staggered23").id)
        assertEquals("sport-ampl-track-a-skip", catalog.byId.getValue("new46-askip").id)
        assertEquals("new46-scapularhang", catalog.byId.getValue("sport-calisthenics-scapula").id)
        for (exercise in sequences) {
            assertFalse(exercise.validationBlocked)
            val pair = exercise.raw.a("sequenceParts").strings().map {
                val component = catalog.byId.getValue(it)
                component.raw.s("baseId", component.id)
            }.sorted()
            assertTrue(exercise.id, pairs.add(pair))
            val components = checkNotNull(ExerciseMotions.loadSequence(context, exercise)) { exercise.id }
            assertEquals(2, components.size)
            val stage = Size(208f, 320f)
            val frames = components.map(ExerciseFraming::from)
            val unitScale = frames.minOf { it.scaleFor(stage) * it.unit }
            assertTrue(exercise.id, unitScale.isFinite() && unitScale > 0f)
            for (frame in frames) {
                assertTrue(frame.bounds.width * unitScale / frame.unit < stage.width)
                assertTrue(frame.bounds.height * unitScale / frame.unit < stage.height)
            }
        }
    }
}
