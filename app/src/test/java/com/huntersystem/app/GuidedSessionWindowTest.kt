package com.huntersystem.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w393dp-h851dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GuidedSessionWindowTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()

    @Test(timeout=120000) fun sessionKeepsNavigationVisibleAndDoesNotRecordSkippedSeries() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val fixture=JSONArray(javaClass.classLoader!!.getResource("groups-golden.json")!!.readText())
            .objects()[7].o("p").copyJson()
        val profile=fixture.put("name","Cazador").put("animate",false)
            .put("weeklyCategories",arr(List(7){2})).put("theme","dark")
        val store=NativeStore(context)
        store.saveProfile(profile)
        val date=today()
        val exercise=store.catalog.byId.getValue("chair")
        val item=store.planner.libraryDose(exercise,store.adjusted(),"strength")
            .put("unifiedPlanning",true).put("sets",2).put("target",8)
        store.customize(date,listOf(item),emptyList(),"Tren inferior")
        val ui=NativeUi(store)

        compose.setContent { HunterTheme(store) { HunterRoot(ui) } }
        compose.runOnIdle { ui.open("session",obj("date" to date)) }
        compose.onNodeWithText("Paso 1 de 3").assertIsDisplayed()
        compose.onNodeWithText(exercise.name).assertIsDisplayed()
        compose.onNodeWithText("‹ Anterior").assertIsNotEnabled()
        compose.onNodeWithText("Completar y registrar").assertIsDisplayed()
        compose.onNodeWithText("Detener por molestias").assertIsDisplayed()
        capture("oscuro")

        compose.onNodeWithText("Indicaciones").performScrollTo().performClick()
        compose.onNodeWithText("Siguiente ›").assertIsDisplayed().performClick()
        compose.onNodeWithText("La serie sigue pendiente.",substring=true).assertIsDisplayed()
        compose.onNodeWithText("Siguiente ›").performClick()
        compose.onNodeWithText("Paso 2 de 3").assertIsDisplayed()
        compose.onNodeWithText("Intervalo de recuperación").assertIsDisplayed()
        compose.onNodeWithText("Completar y registrar").assertDoesNotExist()
        compose.onNodeWithText("A CONTINUACIÓN").assertIsDisplayed()
        capture("descanso")
        compose.runOnIdle { assertEquals(0,store.stats().i("series")) }

        compose.runOnIdle { store.saveProfile(profile.copyJson().put("theme","light")) }
        compose.onNodeWithText("‹ Anterior").performClick()
        compose.onNodeWithText("Paso 1 de 3").assertIsDisplayed()
        capture("claro")
        compose.onNodeWithText("Completar y registrar").performClick()
        compose.onNodeWithText("Confirmar serie realizada").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1,store.stats().i("series")) }
    }

    private fun capture(theme:String) {
        val output=File("../qa")
        compose.runOnIdle {
            val view=checkNotNull(ShadowDialog.getLatestDialog()?.window?.decorView)
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            output.mkdirs()
            File(output,"sesion-paso-a-paso-$theme.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
            }
        }
    }
}
