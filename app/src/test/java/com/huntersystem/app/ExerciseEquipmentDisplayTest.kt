package com.huntersystem.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w393dp-h851dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExerciseEquipmentDisplayTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test(timeout=120000) fun theCatalogStageShowsEquipmentInItsVectorScene() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = NativeStore(context)
        val catalog = store.catalog
        val id = mutableStateOf("row")
        compose.setContent {
            HunterTheme(store) {
                MonarchExerciseStage(catalog.byId.getValue(id.value),catalog,mirror=false,play=false,height=300)
            }
        }
        compose.onNodeWithText("Equipo · Mancuerna · Mesa de apoyo").assertIsDisplayed()
        val row = captureScene("Equipo: Mancuerna, Mesa de apoyo")
        assertTrue(row.width >= 320 && row.height >= 280)
        save("remo-mancuerna",row)

        compose.runOnIdle { id.value = "towelrow" }
        compose.onNodeWithText("Equipo · Toalla").assertIsDisplayed()
        val towel = captureScene("Equipo: Toalla")
        val pixels = IntArray(towel.width*towel.height)
        towel.getPixels(pixels,0,towel.width,0,0,towel.width,towel.height)
        // The cloth has a distinct base fill that appears in neither the athlete nor the gym SVG.
        assertTrue(pixels.count { it and 0xFFFFFF == 0xEEA86C } > 70)
        save("remo-toalla",towel)
    }

    private fun captureScene(description: String): Bitmap {
        val bounds = compose.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot
        return compose.runOnIdle {
            val content = compose.activity.findViewById<View>(android.R.id.content)
            val left = bounds.left.toInt().coerceAtLeast(0)
            val top = bounds.top.toInt().coerceAtLeast(0)
            val width = bounds.right.toInt().coerceAtMost(content.width) - left
            val height = bounds.bottom.toInt().coerceAtMost(content.height) - top
            Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888).also { bitmap ->
                val canvas = Canvas(bitmap)
                canvas.translate(-left.toFloat(),-top.toFloat())
                content.draw(canvas)
            }
        }
    }

    private fun save(name: String, bitmap: Bitmap) {
        val file = File("../qa/equipo-$name.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
}
