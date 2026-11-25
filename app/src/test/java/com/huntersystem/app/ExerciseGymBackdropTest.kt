package com.huntersystem.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h851dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExerciseGymBackdropTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val examples = mapOf(
        "chair" to ExerciseGymBackdrop.SIDE,
        "jumpjack" to ExerciseGymBackdrop.FRONT,
        "home-pushwide30" to ExerciseGymBackdrop.OVERHEAD
    )

    @Test fun approvedMotionsSelectThreeDistinctStaticBackdropsInBothThemes() {
        for ((id, backdrop) in examples) {
            val motion = checkNotNull(ExerciseMotions.load(context, id))
            assertEquals(id, backdrop, ExerciseGymBackdrop.forView(motion.view))
        }
        assertEquals(ExerciseGymBackdrop.FRONT, ExerciseGymBackdrop.forView(ExerciseView.REAR))

        val dark = ExerciseGymBackdrop.entries.map { render(it, light = false) }
        val light = ExerciseGymBackdrop.entries.map { render(it, light = true) }
        for (index in dark.indices) {
            assertFalse(dark[index].sameAs(light[index]))
            for (other in 0 until index) {
                assertFalse(dark[index].sameAs(dark[other]))
                assertFalse(light[index].sameAs(light[other]))
            }
        }
    }

    @Test(timeout = 120000) fun captureTheThreeViewsInTheApp() {
        val profile = JSONArray(javaClass.classLoader!!.getResource("groups-golden.json")!!.readText())
            .objects()[7].o("p").copyJson().put("name", "Cazador").put("animate", false)
            .put("weeklyCategories", arr(List(7) { 2 }))
        val store = NativeStore(context)
        store.saveProfile(profile.copyJson().put("theme", "dark"))
        val ui = NativeUi(store)
        compose.setContent { HunterTheme(store) { HunterRoot(ui) } }

        val output = File("../qa").apply { mkdirs() }
        for (theme in listOf("dark", "light")) {
            compose.runOnIdle {
                ui.stack.clear()
                store.saveProfile(profile.copyJson().put("theme", theme))
            }
            for ((id, backdrop) in examples) {
                compose.runOnIdle {
                    ui.stack.clear()
                    ui.open("exercise", obj("id" to id))
                }
                compose.onAllNodesWithText(store.catalog.byId.getValue(id).name).onFirst().assertIsDisplayed()
                compose.runOnIdle {
                    val view = checkNotNull(ShadowDialog.getLatestDialog()?.window?.decorView)
                    val screen = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(screen))
                    val file = File(output, "gimnasio-${backdrop.name.lowercase()}-$theme.png")
                    file.outputStream().use { assertTrue(screen.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                }
            }
        }
    }

    private fun render(backdrop: ExerciseGymBackdrop, light: Boolean): Bitmap {
        val picture = checkNotNull(backdrop.load(context, light)) { backdrop.asset }
        assertEquals(650, picture.width)
        assertEquals(416, picture.height)
        val bitmap = Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawPicture(picture)
        assertEquals(255, Color.alpha(bitmap.getPixel(325, 208)))
        assertTrue(bitmap.getPixel(325, 20) != bitmap.getPixel(325, 390))
        return bitmap
    }
}
