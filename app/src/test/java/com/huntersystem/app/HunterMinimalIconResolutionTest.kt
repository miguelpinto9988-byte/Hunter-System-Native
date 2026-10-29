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

/** Prueba de visualización real: no basta con que los SVG estén empaquetados. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HunterMinimalIconResolutionTest {
    @Test fun all49NativeVectorsActuallyPaintVisiblePixels() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        assertEquals(49,hunterNativeVectorResources.size)
        val missing=mutableListOf<String>()
        for ((name,id) in hunterNativeVectorResources) {
            val drawable=requireNotNull(context.getDrawable(id)) {
                "No existe el vector Android del ícono $name"
            }
            val output=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888)
            drawable.setBounds(0,0,96,96)
            drawable.draw(Canvas(output))
            val pixels=IntArray(96*96)
            output.getPixels(pixels,0,96,0,0,96,96)
            if (pixels.none { Color.alpha(it)>0 }) missing+=name
            output.recycle()
        }
        assertTrue("Vectores que no dibujan píxeles: ${missing.joinToString()}", missing.isEmpty())
    }

    @Test fun everySemanticIconPointsToANativeVector() {
        assertEquals(R.drawable.hunter_icon_home,hunterIconDrawableId("panel"))
        assertEquals(R.drawable.hunter_icon_quest,hunterIconDrawableId("missions"))
        assertEquals(R.drawable.hunter_icon_calendar,hunterIconDrawableId("routines"))
        assertEquals(R.drawable.hunter_icon_dumbbell,hunterIconDrawableId("exercises"))
        assertEquals(R.drawable.hunter_icon_week_core,hunterIconDrawableId("week_core"))
        assertEquals(R.drawable.hunter_icon_week_lower,hunterIconDrawableId("week_lower"))
        assertEquals(R.drawable.hunter_icon_lightning,hunterIconDrawableId("unknown_key"))
    }
}
