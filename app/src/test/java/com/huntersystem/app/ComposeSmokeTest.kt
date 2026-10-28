package com.huntersystem.app
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.json.JSONArray
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
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
 @Before fun setup(){val c=ApplicationProvider.getApplicationContext<Context>();c.filesDir.listFiles()?.filter{it.name.startsWith("hunter-system-native")}?.forEach{it.delete()};store=NativeStore(c);val profile=JSONArray(javaClass.classLoader!!.getResource("groups-golden.json")!!.readText()).objects()[7].o("p").copyJson().put("name","Cazador").put("weekPlan",arr(List(7){"train"})).put("animate",false).put("goalGroup","build").put("finalGoal","Aumentar mi fuerza").put("finalTarget","strength").put("weekFocus",arr(listOf("upper","upper","upper","lower","upper","core","upper")));store.saveProfile(profile);ui=NativeUi(store);compose.mainClock.autoAdvance=true;compose.setContent{HunterTheme(store){HunterRoot(ui)}};compose.mainClock.advanceTimeByFrame();compose.waitForIdle()}
 fun capture(name:String){compose.runOnIdle{val view=compose.activity.window.decorView;val image=Bitmap.createBitmap(view.width.coerceAtLeast(393),view.height.coerceAtLeast(851),Bitmap.Config.ARGB_8888);view.draw(android.graphics.Canvas(image));val qa=File("../qa");qa.mkdirs();File(qa,"native-$name.png").outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)}}}
 @Test(timeout=120000) fun sixScreensAndExerciseDialogRender(){
  for(tab in listOf("home","plan","routines","book","stats","profile")){
   compose.runOnIdle{ui.go(tab)};compose.waitForIdle();compose.onAllNodesWithText("HUNTER SYSTEM").onFirst().assertIsDisplayed();capture(tab)
  }
  compose.runOnIdle{ui.open("exercise",obj("id" to "chair"))};compose.waitForIdle();compose.onAllNodesWithText("Sentadilla a silla").onFirst().assertExists();compose.runOnIdle{ui.back()};compose.waitForIdle()
 }
 @Test(timeout=60000) fun firstLaunchProfileCanRender(){
  compose.runOnIdle{store.state.put("profile",JSONObject.NULL);store.save();ui.go("profile")};compose.waitForIdle()
  val qa=File("../qa");qa.mkdirs();File(qa,"initial-semantics.txt").writeText(compose.onRoot().printToString());capture("initial-profile")
  compose.onNodeWithText("Crea tu perfil").assertExists();compose.onAllNodesWithText("Nombre de cazador").onFirst().assertExists()
 }
 @Test(timeout=90000) fun generatedRoutineAssignmentPreservesDisplayedItems(){
  compose.runOnIdle{ui.go("routines");ui.library="offline"};compose.waitForIdle()
  compose.onNodeWithText("Tren superior").performScrollTo().performClick()
  compose.onNodeWithText("5–10 min").performScrollTo().performClick()
  compose.onNodeWithText("Generar nuevas rutinas").performScrollTo().performClick()
  compose.waitUntil(15000){ui.generated["offline"]!=null}
  val displayed=ui.generated["offline"]!!.first{it.i("level")==level(store.profile!!)}
  compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("Diseña tu próxima rutina"))).performScrollToNode(hasText("Abrir secuencia 2D"))
  compose.onAllNodesWithText("Abrir secuencia 2D").onFirst().performClick()
  compose.onNodeWithText("Usar esta rutina").performScrollTo().performClick()
  compose.onNodeWithText("Confirmar asignación").performScrollTo().performClick()
  compose.waitForIdle()
  org.junit.Assert.assertEquals(displayed.a("items").toString(),store.day(today()).a("items").toString())
  org.junit.Assert.assertEquals(displayed.s("name"),store.day(today()).s("title"))
 }

 @Test(timeout=120000) fun fiveColorsAndLegacyVisualPreferencesPersistWithoutChangingMissions(){
  val original=store.day(today()).a("items").toString()
  val originalTrainingKey=store.planner.profileKey(store.adjusted())
  compose.runOnIdle{
   val visual=store.state.o("eclipseAppearance").copyJson()
   visual.put("mode","relic").put("reduceMotion",true)
   store.state.put("eclipseAppearance",visual)
   store.save()
   ui.go("profile")
  };compose.waitForIdle()
  for(name in listOf("Eclipse","Glaciar","Carmesí","Jade","Ámbar"))
   compose.onNodeWithContentDescription("Tema $name").assertExists()
  compose.onAllNodesWithContentDescription("Tema Constelación").assertCountEquals(0)
  for(hidden in listOf("Profundidad y movimiento","Tamaño del acompañante","Velocidad ambiental","Reducir movimiento","Colección visual","Escenario"))
   compose.onAllNodesWithText(hidden).assertCountEquals(0)
  compose.onNodeWithContentDescription("Tema Glaciar").performScrollTo().performClick()
  compose.onNodeWithText("Guardar perfil").performScrollTo().performClick()
  compose.waitForIdle()
  org.junit.Assert.assertEquals("ice",store.profile!!.s("theme"))
  org.junit.Assert.assertEquals("Cambiar el tema conserva los datos de entrenamiento",originalTrainingKey,store.planner.profileKey(store.adjusted()))
  compose.onNodeWithText("Continuar").performClick()
  val reloaded=NativeStore(store.context)
  org.junit.Assert.assertEquals("ice",reloaded.profile!!.s("theme"))
  org.junit.Assert.assertEquals("relic",reloaded.state.o("eclipseAppearance").s("mode"))
  org.junit.Assert.assertTrue(reloaded.state.o("eclipseAppearance").b("reduceMotion"))
  org.junit.Assert.assertEquals(original,reloaded.day(today()).a("items").toString())
  compose.onNodeWithContentDescription("Tema Eclipse").performScrollTo().performClick()
  compose.onNodeWithText("Guardar perfil").performScrollTo().performClick();compose.waitForIdle()
  org.junit.Assert.assertEquals("eclipse",store.profile!!.s("theme"))
  org.junit.Assert.assertEquals(original,store.day(today()).a("items").toString())
  compose.onNodeWithText("Continuar").performClick()
  compose.runOnIdle{ui.go("home")};compose.waitForIdle();capture("color-home")
 }
 @Test(timeout=60000) fun profileCanBeOpenedFromAvatarWithoutBottomTab(){
   compose.onAllNodesWithText("Perfil").assertCountEquals(0)
   compose.onNodeWithContentDescription("Abrir perfil").assertIsDisplayed().performClick();compose.waitForIdle()
   compose.onNodeWithText("Tu perfil de cazador").assertExists()
   org.junit.Assert.assertEquals("profile",ui.tab)
   compose.runOnIdle{ui.back()};compose.waitForIdle()
   org.junit.Assert.assertEquals("home",ui.tab)
  }
  @Test(timeout=90000) @Config(qualifiers="w1280dp-h800dp-land-mdpi") fun tabletNavigationAndDialogRender(){
  compose.onNodeWithText("Inicio").assertIsDisplayed()
  compose.onNodeWithContentDescription("Abrir perfil").performClick();compose.waitForIdle();capture("tablet-profile")
  compose.runOnIdle{ui.open("exercise",obj("id" to "chair"))};compose.waitForIdle()
  compose.onNodeWithContentDescription("Cerrar ventana").assertIsDisplayed().performClick()
 }

 fun captureDialog(name:String){
  compose.onNode(isDialog()).assertExists()
  compose.runOnIdle {
   val view=requireNotNull(org.robolectric.shadows.ShadowDialog.getLatestDialog().window).decorView
   check(view.width>0 && view.height>0) { "El diálogo debe estar medido antes de capturarlo" }
   val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
   view.draw(android.graphics.Canvas(bitmap))
   val qa=File("../qa");qa.mkdirs();File(qa,"native-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
  }
 }
 @Test(timeout=120000) fun exerciseSessionNotificationsAndCompanionDialogsRender(){
  compose.runOnIdle{ui.open("exercise",obj("id" to "chair"))};compose.waitForIdle()
  compose.onNodeWithText("Reproducir").assertExists();captureDialog("exercise-dialog")
  compose.onNodeWithContentDescription("Cerrar ventana").performClick()
  compose.runOnIdle{ui.open("coach")};compose.waitForIdle();captureDialog("companion-dialog")
  compose.onNodeWithContentDescription("Cerrar ventana").performClick()
  compose.runOnIdle{ui.open("reminders")};compose.waitForIdle();captureDialog("reminders-dialog")
  compose.onNodeWithContentDescription("Cerrar ventana").performClick()
  compose.runOnIdle{ui.open("relic")};compose.waitForIdle();captureDialog("relic-dialog")
  compose.onNodeWithContentDescription("Cerrar ventana").performClick()
  val routine=store.catalog.routines.first{r->r.a("items").objects().all{x->store.catalog.byId[x.s("id")]?.let{store.rules.compatible(it,store.adjusted())}==true}}
  compose.runOnIdle{ui.open("session",obj("routineId" to routine.s("id")))};compose.waitForIdle()
  compose.onNodeWithText("Iniciar").assertExists();captureDialog("session-dialog")
 }
 @Test(timeout=120000) fun allThemePreviewsRender(){
  for(theme in eclipsePalettes){
   compose.runOnIdle{store.saveProfile(store.profile!!.copyJson().put("theme",theme.id));ui.go("home")};compose.waitForIdle()
   compose.onNodeWithText("Abrir misión de hoy").assertExists();capture("theme-"+theme.id)
  }
 }

}
