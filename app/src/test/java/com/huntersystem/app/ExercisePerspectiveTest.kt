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

    @Test fun everyNewSequenceLoadsWithItsOwnPerspectiveAndACommonScale() {
        val catalog = Catalog(context)
        assertEquals(5_000, catalog.exercises.size)
        assertEquals("new46-tke", catalog.byId.getValue("hx_lower_0019").id)
        assertTrue(catalog.exercises.none { it.id == "hx_lower_0019" })
        val sequences = catalog.exercises.filter { it.id.startsWith("sport-seq-") }
        assertEquals(2_966, sequences.size)
        val pairs = mutableSetOf<List<String>>()
        for ((discipline, exercises) in sequences.groupBy { it.sport }) {
            val latest = exercises.filter { it.raw.i("catalogWave") == 3 }
            val replacement = if (discipline == "Fuerza") 1 else 0
            assertEquals(mapOf(0 to 70 + replacement, 1 to 75, 2 to 55),
                latest.groupingBy { it.planningLevel }.eachCount())
            assertEquals(mapOf(0 to 208 + replacement, 1 to 223, 2 to 162),
                exercises.groupingBy { it.planningLevel }.eachCount())
        }
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
