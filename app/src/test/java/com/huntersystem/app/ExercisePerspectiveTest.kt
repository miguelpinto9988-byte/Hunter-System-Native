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
        assertEquals(3_000, catalog.exercises.size)
        val sequences = catalog.exercises.filter { it.id.startsWith("sport-seq-") }
        assertEquals(965, sequences.size)
        for ((_, exercises) in sequences.groupBy { it.sport }) {
            assertEquals(mapOf(0 to 68, 1 to 73, 2 to 52),
                exercises.groupingBy { it.planningLevel }.eachCount())
        }
        for (exercise in sequences) {
            assertFalse(exercise.validationBlocked)
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
