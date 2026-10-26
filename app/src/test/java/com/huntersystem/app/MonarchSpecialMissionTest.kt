package com.huntersystem.app

import org.junit.Test
import org.junit.Assert.*
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class MonarchSpecialMissionTest{
 @Test fun presetsHaveDistinctExistingExercisesAndRestWindows(){
  assertEquals(3,MonarchMissionCatalog.missions.size)
  val extreme=MonarchMissionCatalog.spec("trono")
  assertEquals(100,extreme.goals.first{it.exerciseId=="squat"}.target)
  assertEquals(50,extreme.goals.first{it.exerciseId=="new46-burpee"}.target)
  assertEquals(7L,extreme.cooldownDays)
  val ctx=ApplicationProvider.getApplicationContext<Context>()
  val store=NativeStore(ctx)
  MonarchMissionCatalog.missions.forEach{m->
   assertTrue(m.minutes in 25..65)
   assertTrue(m.cooldownDays>=4L)
   assertEquals(m.goals.size,m.goals.map{it.exerciseId}.distinct().size)
   assertTrue(m.goals.all{it.target>0&&store.catalog.byId.containsKey(it.exerciseId)})
  }
 }
 @Test fun countdownUsesRealDeadlineAndCannotShowOverfilledProgress(){
  val start=1700000000000L
  val m=MonarchMissionCatalog.spec("trono")
  val run=obj("id" to m.id,"status" to "active","startedAt" to start,
   "deadlineAt" to (start+TimeUnit.MINUTES.toMillis(60)),
   "counts" to obj("squat" to 999,"new46-burpee" to 50,"pushup" to 30))
  assertEquals("active",MonarchMissionCatalog.status(run,start+1000))
  assertEquals("expired",MonarchMissionCatalog.status(run,start+TimeUnit.MINUTES.toMillis(60)))
  assertEquals(1f,MonarchMissionCatalog.progress(run,m),.0001f)
  assertEquals(0L,MonarchMissionCatalog.remaining(run,start+TimeUnit.DAYS.toMillis(10)))
  assertEquals(start+TimeUnit.MINUTES.toMillis(60)+TimeUnit.DAYS.toMillis(7),
   MonarchMissionCatalog.cooldownEnd(run,m))
 }
 @Test fun noProfileCannotStartAndDoesNotMutateBackup(){
  val ctx=ApplicationProvider.getApplicationContext<Context>()
  ctx.filesDir.listFiles()?.filter{it.name.startsWith("hunter-system-native")}?.forEach{it.delete()}
  val store=NativeStore(ctx)
  val before=store.export()
  assertNotNull(MonarchMissionCatalog.eligibleReason(store,MonarchMissionCatalog.spec("trono"),System.currentTimeMillis()))
  try{store.startMonarchMission("trono");fail("Debe rechazar perfiles sin verificar")}
  catch(_:IllegalArgumentException){}
  assertEquals(before,store.export())
 }
 @Test fun completionIsNotInferredFromPartialManualCounts(){
  val m=MonarchMissionCatalog.spec("guardia")
  val run=obj("id" to m.id,"status" to "active","deadlineAt" to 1000L,"counts" to obj("squat" to 40,"jumpjack" to 60))
  assertTrue(MonarchMissionCatalog.progress(run,m)<1f)
  assertEquals("active",MonarchMissionCatalog.status(run,0))
 }
}