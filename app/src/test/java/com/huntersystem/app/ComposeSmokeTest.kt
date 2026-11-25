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
import org.robolectric.shadows.ShadowDialog
import android.content.Context
import android.graphics.Bitmap
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
  compose.runOnIdle{ui.open("exercise",obj("id" to "chair"))};compose.waitForIdle();compose.onAllNodesWithText("Sentadilla a silla").onFirst().assertExists();compose.onNodeWithText("Pausar demostración").assertExists();compose.runOnIdle{ui.back()};compose.waitForIdle()
 }
 @Test(timeout=60000) fun catalogSearchOpensMatchingExercise() {
  compose.runOnIdle { ui.go("book") }
  val catalog = compose.onNodeWithTag("exerciseCatalog")
  catalog.performScrollToNode(hasSetTextAction())
  compose.onNode(hasSetTextAction()).performTextReplacement("Sentadilla a silla")
  val card = hasContentDescription("Abrir ejercicio: Sentadilla a silla")
  catalog.performScrollToNode(card)
  compose.onNode(card).performClick()
  compose.onNodeWithText("DEMOSTRACIÓN").assertExists()
  compose.onNodeWithText("Pausar demostración").assertExists()
 }
 @Test(timeout=60000) fun techniqueGuidesRenderWithAutoplay() {
  val cases=mapOf("bridge" to TechniqueDiagram.BRIDGE,"home-diamond23" to TechniqueDiagram.DIAMOND,
      "home-vup23-tempo23" to TechniqueDiagram.V_UP,"new46-reverseclam" to TechniqueDiagram.REVERSE_CLAM,
      "kickback" to TechniqueDiagram.KICKBACK)
  for((id,diagram) in cases){
   compose.runOnIdle { ui.open("exercise",obj("id" to id)) }
   compose.onNodeWithText("Pausar demostración").assertExists()
   val illustration=compose.onNodeWithContentDescription(diagram.title+". "+diagram.cue)
   illustration.performScrollTo().assertIsDisplayed()
   compose.runOnIdle {
    val view=checkNotNull(ShadowDialog.getLatestDialog()?.window?.decorView)
    val picture=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
    view.draw(android.graphics.Canvas(picture))
    val folder=File("../qa").apply { mkdirs() }
    File(folder,"tecnica-$id.png").outputStream().use { picture.compress(Bitmap.CompressFormat.PNG,100,it) }
   }
   compose.runOnIdle { ui.back() }
  }
 }
 @Test(timeout=60000) fun sequencePreviewCanOpenBothGestures() {
  val exercise = store.catalog.historicalExercises.first { it.id.startsWith("sport-seq-") && store.catalog.classification(it.id) != null }
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
  compose.onNodeWithText("IDENTIDAD DEL CAZADOR").assertIsDisplayed()
  compose.onNodeWithText("PASO 1 DE 4").assertIsDisplayed()
 }
 @Test(timeout=120000) fun composeProfileNavigatesAndRequiresConsent() {
  compose.runOnIdle { store.state.put("profile", JSONObject.NULL); store.save(); ui.openProfile() }
  capture("profile-01-identidad-claro")
  compose.onNodeWithText("SIGUIENTE ›").performClick()
  compose.onNodeWithText("Escribe tu nombre de cazador").assertExists()
  compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("Auditoría Compose")
  compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("14")
  compose.onNodeWithText("SIGUIENTE ›").performClick()
  capture("profile-02-entrenamiento-claro")
  compose.onNodeWithContentDescription("Lunes: Descanso").performScrollTo().performClick()
  compose.onNodeWithContentDescription("Lunes: Tren superior").assertExists()
  compose.onNodeWithText("Rutinas").performScrollTo().performClick()
  compose.onNodeWithText("Tiempo disponible", substring = true).assertExists()
  compose.onNodeWithText("Cantidad de misiones", substring = true).assertDoesNotExist()
  compose.onNodeWithText("SIGUIENTE ›").performClick()
  compose.onNodeWithText("PERSONALIZACIÓN").assertIsDisplayed()
  capture("profile-03-personalizacion-claro")
  compose.onNodeWithText("Tema del sistema", substring = true).performScrollTo().performClick()
  compose.onNodeWithText("Negro").performClick()
  compose.onNodeWithText("Tema del sistema", substring = true).assertTextContains("Negro", substring = true)
  capture("profile-03-personalizacion-oscuro")
  compose.onNodeWithText("SIGUIENTE ›").performClick()
  capture("profile-04-activacion-oscuro")
  compose.onNodeWithText("ACTIVAR PERFIL").assertIsNotEnabled()
  compose.onNodeWithText("He revisado mi información y deseo activar mi perfil de cazador.").performScrollTo().performClick()
  compose.onNodeWithText("ACTIVAR PERFIL").performClick()
  compose.onNodeWithText("SISTEMA ACTIVADO").assertIsDisplayed()
  compose.onNodeWithText("ENTRAR AL SISTEMA").performClick()
  compose.runOnIdle {
   Assert.assertEquals("Auditoría Compose", store.profile!!.s("name"))
   Assert.assertEquals("dark", store.profile!!.s("theme"))
   Assert.assertEquals("routines", store.profile!!.s("trainingMode"))
   Assert.assertEquals(0, store.profile!!.a("weeklyCategories").getInt(0))
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
