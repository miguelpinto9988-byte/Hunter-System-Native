package com.huntersystem.app

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class MonarchSpecialMissionTest {
 private val zone=ZoneId.of("America/Lima")
 private fun d(s:String)=LocalDate.parse(s)

 @Test fun sundayResetsCycleAndChangesRotatingDifficulty() {
  val dates=listOf("2026-10-25","2026-11-01","2026-11-08","2026-11-15","2026-11-22")
  val missions=dates.map{MonarchMissionCatalog.missionFor(d(it),2,zone)}
  assertEquals(listOf("guardia","fortaleza","trono","fortaleza","guardia"),missions.map{it.id})
  assertEquals(listOf(1,2,3,4,1),missions.map{it.cycleWeek})
  assertEquals(60,missions[0].goals.first{it.exerciseId=="squat"}.target)
  assertEquals(80,missions[1].goals.first{it.exerciseId=="squat"}.target)
  assertEquals(120,missions[2].goals.first{it.exerciseId=="squat"}.target)
  assertEquals(60,missions[2].goals.first{it.exerciseId=="new46-burpee"}.target)
  assertEquals(70,missions[2].goals.first{it.exerciseId=="pushup"}.target)
  // El patrón de niveles vuelve a empezar tras cuatro semanas, sin repetir todos los ejercicios.
  assertNotEquals(missions[0].goals,missions[4].goals)
  assertEquals("guardia",MonarchMissionCatalog.missionFor(d("2026-11-08"),1,zone).id)
 }

 @Test fun deadlinesRunFromSundayToFollowingSundayNotMinutes() {
  val spec=MonarchMissionCatalog.missionFor(d("2026-10-26"),2,zone)
  assertEquals("2026-10-25",spec.weekStart)
  val sunday= d("2026-11-01").atStartOfDay(zone).toInstant().toEpochMilli()
  assertEquals(sunday,spec.deadlineAt)
  assertEquals(d("2026-10-25"),MonarchMissionCatalog.weekStart(sunday-1,zone))
  assertEquals(d("2026-11-01"),MonarchMissionCatalog.weekStart(sunday,zone))
  val run=obj("id" to spec.id,"weekStart" to spec.weekStart,
   "deadlineAt" to spec.deadlineAt,"status" to "active","counts" to obj("squat" to 60))
  assertEquals("active",MonarchMissionCatalog.status(run,sunday-1))
  assertEquals("expired",MonarchMissionCatalog.status(run,sunday))
  assertEquals(0L,MonarchMissionCatalog.remaining(run,sunday))
 }

 @Test fun allPresetsHaveExistingOriginalExerciseIdsAndNoDuplicates() {
  val c=ApplicationProvider.getApplicationContext<Context>()
  val catalog=Catalog(c)
  (0..7).forEach{w->
   val spec=MonarchMissionCatalog.missionFor(d("2026-10-25").plusWeeks(w.toLong()),2,zone)
   assertEquals(spec.goals.size,spec.goals.map{it.exerciseId}.distinct().size)
   assertTrue(spec.goals.all{g->
    g.target>0 && catalog.byId[g.exerciseId]?.raw?.b("sourceExpansion2k")==false
   })
  }
 }

 @Test fun storedSnapshotDoesNotRerollWhileWeekIsRunningAndProgressIsCapped(){
  val spec=MonarchMissionCatalog.missionFor(d("2026-11-08"),2,zone)
  val run=obj("id" to spec.id,"status" to "active","weekStart" to spec.weekStart,
   "deadlineAt" to spec.deadlineAt,"cycleWeek" to spec.cycleWeek,
   "goals" to arr(spec.goals.map{obj("exerciseId" to it.exerciseId,"target" to it.target)}),
   "counts" to obj("squat" to 999,"new46-burpee" to 60,"pushup" to 70))
  val snapshot=MonarchMissionCatalog.fromRun(run)
  assertEquals(spec.goals,snapshot.goals)
  assertTrue(MonarchMissionCatalog.progress(run,snapshot)<1f) // falta cuarto objetivo
  run.o("counts").put(spec.goals.last().exerciseId,spec.goals.last().target)
  assertEquals(1f,MonarchMissionCatalog.progress(run,snapshot),.0001f)
 }

 @Test fun noProfileCannotActivateAndDoesNotAlterBackup() {
  val ctx=ApplicationProvider.getApplicationContext<Context>()
  ctx.filesDir.listFiles()?.filter{it.name.startsWith("hunter-system-native")}?.forEach{it.delete()}
  val store=NativeStore(ctx)
  val before=store.export()
  val fixed=d("2026-11-08").atStartOfDay(zone).toInstant().toEpochMilli()
  val spec=MonarchMissionCatalog.missionFor(d("2026-11-08"),2,zone)
  assertNotNull(MonarchMissionCatalog.eligibleReason(store,spec,fixed))
  try{store.startMonarchMission(fixed,zone);fail("No puede iniciar sin perfil")}
  catch(_:IllegalArgumentException){}
  assertEquals(before,store.export())
 }

 @Test fun sundayArchivesOldAttemptAndKeepsTheRecord() {
  val ctx=ApplicationProvider.getApplicationContext<Context>()
  ctx.filesDir.listFiles()?.filter{it.name.startsWith("hunter-system-native")}?.forEach{it.delete()}
  val store=NativeStore(ctx)
  val end=d("2026-11-01").atStartOfDay(zone).toInstant().toEpochMilli()
  store.state.put("monarchSpecialMission",obj(
   "id" to "guardia","weekStart" to "2026-10-25",
   "status" to "active","deadlineAt" to end,
   "counts" to obj("squat" to 30)))
  store.syncMonarchWeek(end,zone)
  assertNull(store.state.optJSONObject("monarchSpecialMission"))
  assertEquals("2026-11-01",store.state.s("monarchSpecialWeek"))
  assertEquals(1,store.state.a("monarchSpecialHistory").length())
  assertEquals("expired",store.state.a("monarchSpecialHistory").getJSONObject(0).s("status"))
  store.syncMonarchWeek(end,zone)
  assertEquals(1,store.state.a("monarchSpecialHistory").length())
 }
}