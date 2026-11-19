package com.huntersystem.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

/** The original media2d look must remain independent from its articulated rig. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class OriginalVectorRigTest {
    @Test fun calfRigUsesOriginalArtAndMovement(){
        val context=ApplicationProvider.getApplicationContext<Context>()
        val model=OriginalVectorRigAssets.load(context,"media2d/calf.svg")
        assertNotNull("Elevación de talones must retain original anchors",model)
        val rig=model!!
        assertEquals(11,rig.sample(0f).size)
        assertEquals(48,rig.frames.size)
        assertTrue("Expected semantic joint anchors from original SVG",rig.calibratedAnchors>=6)
        assertTrue(rig.viewWidth>0f&&rig.viewHeight>0f)
        val initial=rig.sample(0f)
        val half=rig.sample(.5f)
        assertTrue("Motion data must vary across frames",
            initial.zip(half).any{(a,b)->abs(a.y-b.y)>4f||abs(a.x-b.x)>4f})
        assertTrue(rig.footVectors.containsKey(8))
        val art=context.assets.open("media2d/calf.svg").bufferedReader().use{it.readText()}
        assertTrue(art.contains("Elevación de talones"))
        assertFalse(art.contains("<animate"))
    }
    @Test fun otherOriginalsPreserveTheirOwnCoordinateSystems(){
        val context=ApplicationProvider.getApplicationContext<Context>()
        for(name in listOf("squat","curl")){
            val rig=OriginalVectorRigAssets.load(context,"media2d/$name.svg")
            assertNotNull("Cannot load vector rig for $name",rig)
            assertTrue(rig!!.calibratedAnchors>=5)
            assertEquals(11,rig.sample(.3f).size)
        }
    }
    @Test fun invalidAndRemovedDemonstrationsAreRejected(){
        val context=ApplicationProvider.getApplicationContext<Context>()
        assertNull(OriginalVectorRigAssets.load(context,"demos30/calf.svg"))
        assertNull(OriginalVectorRigAssets.load(context,"media2d/../../calf.svg"))
        assertNull(OriginalVectorRigAssets.load(context,""))
    }
}
