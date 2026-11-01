package com.huntersystem.app

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Prueba de visualización real: comprueba que todos los PNG renderizan píxeles visibles. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HunterMinimalIconResolutionTest {
    @Test fun all49NativePngsActuallyPaintVisiblePixels() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        assertEquals(49,hunterPixelPngResources.size)
        val missing=mutableListOf<String>()
        for ((name,id) in hunterPixelPngResources) {
            val output=requireNotNull(BitmapFactory.decodeResource(context.resources,id)) {
                "No se pudo decodificar PNG RGBA del ícono $name"
            }
            val pixels=IntArray(output.width*output.height)
            output.getPixels(pixels,0,output.width,0,0,output.width,output.height)
            if (pixels.none { Color.alpha(it)>0 } ||
                pixels.all { Color.alpha(it)==255 } ||
                Color.alpha(output.getPixel(0,0))!=0) missing+=name
            output.recycle()
        }
        assertTrue("PNG sin alfa correcto o sin píxeles visibles: ${missing.joinToString()}", missing.isEmpty())
    }

    @Test fun allSelectedNavigationVariantsAreDirectPngResources() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val ids=listOf(
            R.drawable.hunter_nav_home, R.drawable.hunter_nav_quest,
            R.drawable.hunter_nav_calendar, R.drawable.hunter_nav_dumbbell,
            R.drawable.hunter_nav_chart, R.drawable.hunter_nav_profile
        )
        for (id in ids) {
            val value=android.util.TypedValue()
            context.resources.getValue(id,value,true)
            assertTrue("Debe ser un archivo PNG, no XML vectorial",value.string.toString().endsWith(".png"))
            val bitmap=requireNotNull(BitmapFactory.decodeResource(context.resources,id))
            assertEquals(256,bitmap.width)
            assertEquals(256,bitmap.height)
            assertEquals("La esquina debe tener transparencia",0,Color.alpha(bitmap.getPixel(0,0)))
            assertTrue("El icono no debe ser invisible", (0 until bitmap.width).any {
                x -> (0 until bitmap.height).any { y -> Color.alpha(bitmap.getPixel(x,y))>0 }
            })
            bitmap.recycle()
        }
        assertEquals(R.drawable.hunter_nav_home,hunterNavigationDrawableId("panel",true))
        assertEquals(R.drawable.hunter_pixel_home,hunterNavigationDrawableId("panel",false))
    }

    @Test fun everySemanticIconPointsToANativePng() {
        assertEquals(R.drawable.hunter_pixel_home,hunterIconDrawableId("panel"))
        assertEquals(R.drawable.hunter_pixel_quest,hunterIconDrawableId("missions"))
        assertEquals(R.drawable.hunter_pixel_calendar,hunterIconDrawableId("routines"))
        assertEquals(R.drawable.hunter_pixel_dumbbell,hunterIconDrawableId("exercises"))
        assertEquals(R.drawable.hunter_pixel_week_core,hunterIconDrawableId("week_core"))
        assertEquals(R.drawable.hunter_pixel_week_lower,hunterIconDrawableId("week_lower"))
        assertEquals(R.drawable.hunter_pixel_lightning,hunterIconDrawableId("unknown_key"))
    }
}
