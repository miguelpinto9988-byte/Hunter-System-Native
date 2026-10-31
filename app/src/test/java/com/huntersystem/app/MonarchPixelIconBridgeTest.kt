package com.huntersystem.app

import org.junit.Assert.assertEquals
import org.junit.Test

/** La iconografía del Panel y sus accesos rápidos comparte los PNG transparentes de navegación. */
class MonarchPixelIconBridgeTest {
    @Test fun panelMetricsAndShortcutsUsePngIcons() {
        val routes=mapOf(
            "home" to R.drawable.hunter_pixel_home,
            "plan" to R.drawable.hunter_pixel_quest,
            "routines" to R.drawable.hunter_pixel_calendar,
            "book" to R.drawable.hunter_pixel_book,
            "profile" to R.drawable.hunter_pixel_profile
        )
        routes.forEach { (route, expected) ->
            assertEquals(expected, hunterIconDrawableId(monarchGlyphPixelKey(route)))
        }
    }

    @Test fun transparentIconNamesResolveToRegisteredPngFiles() {
        assertEquals(49, hunterPixelPngResources.size)
        assertEquals(R.drawable.hunter_pixel_lightning, hunterIconDrawableId("unknown_key"))
    }
}
