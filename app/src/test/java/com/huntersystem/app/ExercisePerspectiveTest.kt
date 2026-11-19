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
}
