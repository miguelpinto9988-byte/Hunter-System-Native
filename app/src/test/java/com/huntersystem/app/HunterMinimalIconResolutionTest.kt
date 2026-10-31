package com.huntersystem.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
            val drawable=requireNotNull(context.getDrawable(id)) {
                "No existe el PNG Android del ícono $name"
            }
            val output=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888)
            drawable.setBounds(0,0,96,96)
            drawable.draw(Canvas(output))
            val pixels=IntArray(96*96)
            output.getPixels(pixels,0,96,0,0,96,96)
            if (pixels.none { Color.alpha(it)>0 }) missing+=name
            output.recycle()
        }
        assertTrue("PNG que no dibujan píxeles: ${missing.joinToString()}", missing.isEmpty())
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
