package com.huntersystem.app
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.json.JSONArray
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import org.robolectric.Shadows.shadowOf
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w393dp-h851dp-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComposeSmokeTest {
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 lateinit var store:NativeStore
 lateinit var ui:NativeUi
 @Before fun setup(){val c=ApplicationProvider.getApplicationContext<Context>();c.filesDir.listFiles()?.filter{it.name.startsWith("hunter-system-native")}?.forEach{it.delete()};store=NativeStore(c);val profile=JSONArray(javaClass.classLoader!!.getResource("groups-golden.json")!!.readText()).objects()[7].o("p").copyJson().put("name","Cazador").put("animate",false).put("goalGroup","build").put("finalGoal","Aumentar mi fuerza").put("finalTarget","strength").put("weekFocus",arr(listOf("upper","upper","upper","lower","upper","core","upper")));store.saveProfile(profile);ui=NativeUi(store);compose.mainClock.autoAdvance=true;compose.setContent{HunterTheme(store){HunterRoot(ui)}};compose.mainClock.advanceTimeByFrame();compose.waitForIdle()}
 fun capture(name:String){compose.runOnIdle{val view=compose.activity.window.decorView;val image=Bitmap.createBitmap(view.width.coerceAtLeast(393),view.height.coerceAtLeast(851),Bitmap.Config.ARGB_8888);view.draw(android.graphics.Canvas(image));val qa=File("../qa");qa.mkdirs();File(qa,"native-$name.png").outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)}}}
 @Test(timeout=120000) fun sixScreensAndExerciseDialogRender(){
  for(tab in listOf("home","plan","routines","book","stats","profile")){
   compose.runOnIdle{ui.go(tab)};compose.waitForIdle();if(tab=="profile")assertProfileLoaded() else {
    compose.onAllNodesWithText("HUNTER SYSTEM").onFirst().assertIsDisplayed()
    capture(tab)
   }
  }
  compose.runOnIdle{ui.open("exercise",obj("id" to "chair"))};compose.waitForIdle();compose.onAllNodesWithText("Sentadilla a silla").onFirst().assertExists();compose.onNodeWithText("Reproducir demostración").assertExists();compose.runOnIdle{ui.back()};compose.waitForIdle()
 }
 @Test(timeout=60000) fun sequencePreviewCanOpenBothGestures() {
  val exercise = store.catalog.exercises.first { it.id.startsWith("sport-seq-") }
  val labels = exercise.raw.a("sequenceLabels").strings()
  compose.runOnIdle { ui.open("exercise", obj("id" to exercise.id)) }
  compose.onNodeWithText("1 de 2 · ${labels[0]}").assertExists()
  compose.onNodeWithText("Gesto 2").performScrollTo().performClick()
  compose.onNodeWithText("2 de 2 · ${labels[1]}").assertExists()
  compose.onNodeWithText("Gesto 1").performScrollTo().performClick()
  compose.onNodeWithText("1 de 2 · ${labels[0]}").assertExists()
 }
 @Test(timeout=60000) fun firstLaunchProfileCanRender(){
  compose.runOnIdle{store.state.put("profile",JSONObject.NULL);store.save();ui.go("profile")};compose.waitForIdle()
  assertProfileLoaded()
  compose.onNodeWithContentDescription("Panel, seleccionada").assertDoesNotExist()
 }
 @Test(timeout=60000) fun avatarOpensIndependentEditorAndBackReturnsToPanel(){
  compose.onNodeWithContentDescription("Abrir perfil").performClick()
  compose.waitForIdle()
  assertProfileLoaded()
  compose.onNodeWithContentDescription("Panel, seleccionada").assertDoesNotExist()
  compose.runOnIdle { ui.closeProfile() }
  compose.waitForIdle()
  compose.onNodeWithText("Bienvenido, Cazador").assertExists()
 }
 private fun assertProfileLoaded() {
  fun findWebView(view:View):WebView?=when(view){
   is WebView->view
   is ViewGroup->(0 until view.childCount).firstNotNullOfOrNull { findWebView(view.getChildAt(it)) }
   else->null
  }
  compose.runOnIdle {
   val webView=checkNotNull(findWebView(compose.activity.window.decorView))
   Assert.assertEquals(profilePrototypeUrl,shadowOf(webView).lastLoadedUrl)
  }
 }
 @Test(timeout=120000) fun fourStepProfileActivatesAndConnectsThePlanner(){
  compose.runOnIdle {
   store.state.put("profile",JSONObject.NULL)
   store.save()
   ui.go("profile")
   val registration=ProfileRegistration(ui)
   val input=registration.initialData()
   input.o("fields").put("name","Alex").put("age","14").put("difficulty","intermediate")
    .put("companionCharacter","tusk").put("finalGoal","Caminar\nMejorar mi equilibrio")
   input.put("week",arr(listOf(0,5,5,5,5,5,5)))
   Assert.assertThrows(IllegalArgumentException::class.java){registration.submit(input)}
   Assert.assertNull(store.profile)
   registration.submit(input.put("confirmed",true))
   val saved=store.profile!!
   Assert.assertEquals("intermediate",saved.s("difficulty"))
   Assert.assertEquals("light",saved.s("theme"))
   Assert.assertEquals("tusk",companionKey(saved))
   Assert.assertEquals("Caminar\nMejorar mi equilibrio",saved.s("custom_goal"))
   Assert.assertTrue(saved.isNull("height"))
   Assert.assertTrue(store.rules.gate(saved).ok)
   Assert.assertEquals(listOf(0,5,5,5,5,5,5),MonarchProfileAdapter.from(store)!!.weekly)
   Assert.assertEquals("intermediate",store.state.o("monarcaPlanningV2").s("experience"))
   ui.profileSaved()
  }
  compose.onNodeWithText("Bienvenido, Alex").assertExists()
 }
 @Test(timeout=90000) fun generatedRoutineAssignmentPreservesDisplayedItems(){
  // El diseño Monarca 42 cambió la selección visual; ejercitamos el generador real y la asignación.
  lateinit var displayed:JSONObject
  compose.runOnIdle {
    ui.go("routines");ui.library="offline"
    val p=store.adjusted()
    val options=(0..1).mapNotNull{variant->
      store.planner.generateRoutine("offline","upper",level(store.profile!!),variant,
        "short",++ui.serial,p,store.logs.copyJson())
    }
    org.junit.Assert.assertTrue("Debe generarse al menos una rutina compatible",options.isNotEmpty())
    ui.generated["offline"]=options
    displayed=options.first()
  }
  compose.waitForIdle()
  compose.onNodeWithText("Biblioteca de rutinas").assertExists()
  compose.runOnIdle { store.assign(displayed,today()) }
  compose.waitForIdle()
  org.junit.Assert.assertEquals(displayed.a("items").toString(),store.day(today()).a("items").toString())
  org.junit.Assert.assertEquals(displayed.s("name"),store.day(today()).s("title"))
 }

}
