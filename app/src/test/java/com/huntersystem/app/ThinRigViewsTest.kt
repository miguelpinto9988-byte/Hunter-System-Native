package com.huntersystem.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThinRigViewsTest {
    @Test fun slimSquatHasTwoNativeViews() {
        val v = thinRigViews("demos30/squat.svg")
        assertEquals(listOf("FRONTAL", "LATERAL"), v.map { it.first })
        assertEquals("media2d/rig-v2/squat_front.svg", v[0].second)
        assertEquals("media2d/rig-v2/squat_side.svg", v[1].second)
    }

    @Test fun pushupUsesTopView() {
        assertEquals("media2d/rig-v2/pushup_top.svg", thinRigViews("demos30/pushup.svg").single().second)
    }

    @Test fun otherCatalogExercisesAreNotChanged() {
        assertTrue(thinRigViews("demos30/squat_pause.svg").isEmpty())
        assertTrue(thinRigViews("demos30/kneepush.svg").isEmpty())
        assertTrue(thinRigViews("demos30/hx_upper_0001.svg").isEmpty())
    }
}
