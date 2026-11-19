package com.huntersystem.app

import android.content.Context
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
            assertTrue(framing.scaleFor(androidx.compose.ui.geometry.Size(200f, 320f)) > 0f)
        }
    }
}
