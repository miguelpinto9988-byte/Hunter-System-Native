package com.huntersystem.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class StandaloneProfileNavigationTest {
 private lateinit var context:Context
 @Before fun clearExistingProfile(){
  context=ApplicationProvider.getApplicationContext()
  context.filesDir.listFiles()?.filter{it.name.startsWith("hunter-system-native")}?.forEach{it.delete()}
 }
 private fun sampleProfile()=JSONArray(
  javaClass.classLoader!!.getResource("groups-golden.json")!!.readText()
 ).objects()[7].o("p").copyJson()
  .put("name","Cazador")
  .put("goalGroup","build")
  .put("finalGoal","Aumentar mi fuerza")
  .put("weekPlan",arr(List(7){"train"}))
  .put("weekFocus",arr(List(7){"upper"}))

 @Test fun firstLaunchOnlyShowsStandaloneEditorUntilProfileIsSaved(){
  val store=NativeStore(context)
  val ui=NativeUi(store)
  assertNull(store.profile)
  assertEquals("home",ui.tab)
  assertTrue(ui.profileEditorOpen)
  assertTrue(mustShowStandaloneProfile(false,ui.profileEditorOpen,false))
  ui.go("plan")
  assertTrue(ui.profileEditorOpen)
  assertEquals("home",ui.tab)
  ui.closeProfile()
  assertTrue(ui.profileEditorOpen)

  store.saveProfile(sampleProfile())
  assertNotNull(store.profile)
  assertTrue(store.state.a("cycles").length()>0)
  assertNotEquals("Completa tu perfil",store.day(today()).s("title"))
  ui.profileSaved()
  assertFalse(ui.profileEditorOpen)
  assertEquals("home",ui.tab)
  assertFalse(mustShowStandaloneProfile(true,ui.profileEditorOpen,false))

  val reopened=NativeUi(NativeStore(context))
  assertFalse(reopened.profileEditorOpen)
  assertEquals("home",reopened.tab)
 }

 @Test fun avatarRouteEditsStandaloneAndSaveReturnsToPanelWithPlan(){
  val store=NativeStore(context)
  store.saveProfile(sampleProfile())
  val ui=NativeUi(store)
  ui.go("plan")
  assertEquals("plan",ui.tab)
  ui.go("profile")
  assertTrue(ui.profileEditorOpen)
  assertEquals("plan",ui.tab)
  assertTrue(mustShowStandaloneProfile(true,ui.profileEditorOpen,false))
  assertFalse(mustShowStandaloneProfile(true,ui.profileEditorOpen,true))
  ui.back()
  assertFalse(ui.profileEditorOpen)
  assertEquals("home",ui.tab)

  ui.go("profile")
  store.saveProfile(sampleProfile().put("weekFocus",arr(List(7){"core"})))
  ui.profileSaved()
  assertEquals("home",ui.tab)
  assertFalse(ui.profileEditorOpen)
  assertEquals("core",store.profile!!.a("weekFocus").optString(0))
  assertEquals("core",NativeStore(context).profile!!.a("weekFocus").optString(0))
 }
}
