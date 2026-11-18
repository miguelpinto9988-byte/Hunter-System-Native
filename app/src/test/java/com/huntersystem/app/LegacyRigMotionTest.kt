package com.huntersystem.app

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Asegura que el movimiento estructural antiguo vive en JSON, no en SVG. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class LegacyRigMotionTest {
    @Test fun muestraPosesArticuladasOriginales() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val rig=LegacyRigRepository.load(context,"media2d/squat.svg")
        assertNotNull("Debe conservarse el rig histórico de sentadillas",rig)
        val first=rig!!.sample(0f)
        val middle=rig.sample(.5f)
        assertEquals(11,first.size)
        assertEquals(11,middle.size)
        assertTrue("La pose debe variar a lo largo del movimiento",
            first.zip(middle).any{(a,b)->kotlin.math.abs(a.x-b.x)>1f||kotlin.math.abs(a.y-b.y)>1f})
    }

    @Test fun interpolaEntreFotogramasSinModificarLaEstetica() {
        val a=List(11){Offset(100f,200f)}
        val b=List(11){Offset(200f,400f)}
        val motion=LegacyRigMotion(listOf(a,b))
        val atQuarter=motion.sample(.25f)[0]
        assertEquals(150f,atQuarter.x,.001f)
        assertEquals(300f,atQuarter.y,.001f)
        assertEquals(a[0],motion.sample(1f)[0])
    }

    @Test fun noReintroduceSvgAnimadosNiRutasArbitrarias() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        assertNull(LegacyRigRepository.load(context,"demos30/squat.svg"))
        assertNull(LegacyRigRepository.load(context,"media2d/../../squat.svg"))
        assertNull(LegacyRigRepository.load(context,""))
    }
}
