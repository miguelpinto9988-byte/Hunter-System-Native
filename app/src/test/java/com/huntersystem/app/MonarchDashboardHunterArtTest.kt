package com.huntersystem.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Evita una ilustración vacía al cambiar el rango del cazador. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class MonarchDashboardHunterArtTest {
    @Test fun bothHunterGendersAndEveryRankHaveBundledArt() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        for (gender in listOf("male","female","unspecified")) {
            for (rank in listOf("E","D","C","B","A","S")) {
                val file=monarchHunterHeroArtwork(gender,rank)
                context.assets.open(file).use { input ->
                    assertTrue("Ilustración faltante: $file",input.read()!=-1)
                }
                assertFalse("Todavía se carga un compañero: $file",file.contains("companion"))
            }
        }
    }

    @Test fun invalidRankFallsBackToExistingBaseHunter() {
        assertEquals("ui34/hunter-male-e.webp",monarchHunterHeroArtwork("unspecified","?"))
        assertEquals("ui34/hunter-female-e.webp",monarchHunterHeroArtwork("female","E"))
        assertEquals("ui33/hunter-male-s.webp",monarchHunterHeroArtwork("male","S"))
    }
}
