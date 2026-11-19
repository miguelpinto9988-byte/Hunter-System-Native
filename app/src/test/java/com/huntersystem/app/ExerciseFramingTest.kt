package com.huntersystem.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseFramingTest {
    @Test fun effectiveScaleFitsWideAndTallPanelsThroughoutTheMotion() {
        val first = List(11) { Offset(it * 8f, it * 15f) }
        val last = first.toMutableList().also { it[4] = Offset(-190f, 52f) }
        val equipment = ExerciseProp("step", Rect(80f, 60f, 180f, 200f))
        val motion = ExerciseMotion(listOf(first, last), 4f, listOf(0f, .5f),
            "original", "right", props = listOf(equipment))
        val frame = ExerciseFraming.from(motion)

        for (stage in listOf(Size(160f, 360f), Size(520f, 170f))) {
            val scale = frame.scaleFor(stage)
            assertTrue(scale > 0f)
            assertTrue(frame.bounds.width * scale <= stage.width)
            assertTrue(frame.bounds.height * scale <= stage.height)
        }
        assertTrue(frame.bounds.left < last[4].x)
        assertTrue(frame.bounds.right > equipment.bounds.right)
        assertEquals(equipment.bounds.bottom, frame.floor, .001f)
    }

    @Test fun compactPoseDoesNotZoomToFillThePanel() {
        val pose = List(11) { Offset(100f, if (it == 2) 265f else 100f) }
        val motion = ExerciseMotion(listOf(pose, pose), 4f, listOf(0f, .5f), "original", "right")
        val frame = ExerciseFraming.from(motion)
        assertEquals(90f, frame.scaleFor(Size(300f, 300f)) * 165f * frame.unit, .001f)
        assertEquals(0f, frame.scaleFor(Size.Zero), .001f)
    }
}
