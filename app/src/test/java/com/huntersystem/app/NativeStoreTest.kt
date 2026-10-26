package com.huntersystem.app
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.json.JSONArray
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.ZonedDateTime
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w393dp-h851dp-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeStoreTest {
 lateinit var context:Context
 @Before fun prepare(){context=ApplicationProvider.getApplicationContext();context.filesDir.listFiles()?.filter{it.name.startsWith("hunter-system-native")}?.forEach{it.delete()}}
 fun profile():JSONObject {val tests=JSONArray(javaClass.classLoader!!.getResource("groups-golden.json")!!.readText()).objects();return tests[7].o("p").copyJson().put("name","Cazador").put("animate",false).put("goalGroup","build").put("finalGoal","Aumentar mi fuerza").put("finalTarget","strength").put("weekPlan",arr(List(7){"train"})).put("weekFocus",arr(List(7){"upper"}))}
 fun store()=NativeStore(context).also{it.saveProfile(profile())}
 @Test(timeout=30000) fun checkboxesDoNotInventQuantitiesAndPersist(){val s=store();val d=s.day(today());assertTrue(d.a("items").length()>0);s.toggleMission(today(),0);assertEquals(0,s.stats().i("series"));assertEquals(0,s.stats().i("reps"));assertTrue(s.logs.o(today()).a("items").getJSONObject(0).b("missionState"));val reopened=NativeStore(context);assertTrue(reopened.logs.o(today()).a("items").getJSONObject(0).b("missionState"));assertEquals(0,reopened.stats().i("series"));val l=reopened.log();for(i in 1 until l.a("items").length())reopened.toggleMission(today(),i);for(i in 0 until l.a("missionTasks").length())reopened.toggleTask(today(),i);reopened.feedback(today(),6,"Sesión hecha");reopened.finish(today());assertEquals(1,reopened.stats().i("days"));assertEquals(0,reopened.stats().i("series"));assertEquals(0,reopened.stats().i("reps"));assertTrue(reopened.stats().i("xp")>0)}
 @Test(timeout=30000) fun futurePreviewDoesNotCreateRecords(){val s=store();val before=s.export();for(i in 30L..39L)s.day(plus(today(),i));assertEquals(before,s.export());assertEquals(0,s.logs.length())}
 @Test(timeout=30000) fun routineAssignmentCopiesExactItemsAndProtectsConfirmedWork(){val s=store();val r=obj("id" to "test-exact","name" to "Mi secuencia","category" to "Fuerza","items" to s.day(today()).a("items").copyJson());s.assign(r,today());assertEquals(r.a("items").toString(),s.day(today()).a("items").toString());s.confirmSet(today(),0,0,3.0,0.0);assertEquals(1,s.stats().i("series"));val recorded=s.export();try{s.assign(r,today());fail("Debe conservar el registro")}catch(_:IllegalArgumentException){};assertEquals(recorded,s.export());s.saveProfile(profile().put("trainingScale",.5));assertTrue(s.logs.o(today()).a("items").getJSONObject(0).a("actual").getJSONObject(0).b("done"))}
 @Test(timeout=30000) fun restoreKeepsLegacyFieldsAndRejectsBrokenBackup(){val s=store();s.state.put("legacyUnknown",obj("retained" to true));s.save();val backup=JSONObject(s.export());s.restore(backup);assertTrue(s.state.o("legacyUnknown").b("retained"));val before=s.export();try{s.restore(obj("profile" to JSONObject(),"cycles" to JSONArray(),"logs" to JSONObject()));fail("Perfil incompleto")}catch(_:IllegalArgumentException){};assertEquals(before,s.export());val reloaded=NativeStore(context);assertTrue(reloaded.state.o("legacyUnknown").b("retained"))}
 @Test(timeout=30000) fun svgAndNavigationDrawWithNativeCanvas(){val names=listOf("chair","row","plank","new46-pullup");for(id in names){val v=NativeSvgView(context);v.load("demos30/$id.svg");v.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY));v.layout(0,0,360,500);val a=Bitmap.createBitmap(360,500,Bitmap.Config.ARGB_8888);v.draw(Canvas(a));val pixels=IntArray(360*500);a.getPixels(pixels,0,360,0,0,360,500);assertTrue(id,pixels.count{it!=0}>10000);v.fraction=.5f;val b=Bitmap.createBitmap(360,500,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));val next=IntArray(pixels.size);b.getPixels(next,0,360,0,0,360,500);assertTrue("Se anima $id",pixels.indices.count{pixels[it]!=next[it]}>20);v.release()}}
 @Test fun reminderTimesRespectChosenDays(){val r=obj("enabled" to true,"mode" to "daily","time" to "18:00","days" to arr(listOf(1)));val next=ReminderScheduler.next(r,ZonedDateTime.parse("2026-10-23T19:00:00-05:00[America/Lima]"));assertEquals("2026-10-26T18:00-05:00[America/Lima]",next.toString());r.put("mode","interval").put("start","09:00").put("end","20:00").put("interval",3);assertEquals("2026-10-26T18:00-05:00[America/Lima]",ReminderScheduler.next(r,ZonedDateTime.parse("2026-10-26T17:00:00-05:00[America/Lima]")).toString())}
 @Test fun roundsKeepExerciseOrderAndOnlyRestBetweenRounds(){val s=store();val p=s.adjusted().put("blockMode22","rounds");val d=obj("items" to arr(listOf(obj("id" to "chair","sets" to 2,"target" to 6),obj("id" to "row","sets" to 2,"target" to 6))));val steps=s.planner.steps(d,p);assertEquals(listOf("chair","row","chair","row"),steps.filter{it.s("kind")=="exercise"}.map{it.s("id")});assertEquals(1,steps.count{it.s("kind")=="rest"})}
 @Test fun pauseOnlyChangesPendingDaysAndCanResume(){val s=store();val tomorrow=plus(today(),1);s.toggleMission(today(),0);val original=s.day(today()).toString();s.review("pausar");assertEquals(original,s.day(today()).toString());assertEquals(0,s.day(tomorrow).a("items").length());s.review("mantener");assertTrue(s.day(tomorrow).a("items").length()>0);assertEquals(original,s.day(today()).toString())}

 @Test fun reductionSurvivesReloadAndExplicitSecondsKeepTheirUnit(){val s=store();val date=plus(today(),1);val before=s.day(date).a("items").getJSONObject(0).i("target");s.review("reducir");val target=s.day(date).a("items").getJSONObject(0).i("target");assertTrue(target<before);val reopened=NativeStore(context);reopened.refreshWeek();assertEquals(target,reopened.day(date).a("items").getJSONObject(0).i("target"));val item=obj("id" to "chair","unit" to "sec","target" to 20);assertEquals(20,s.planner.workSeconds(s.catalog.byId["chair"]!!,item))}

 @Test fun changedRestrictionsBlockMarkingAnIncompatibleManualMission(){val s=store();val routine=obj("id" to "manual-floor","name" to "Press","category" to "Fuerza","items" to arr(listOf(obj("id" to "press","sets" to 2,"target" to 8,"rest" to 60))));s.assign(routine,today());s.saveProfile(profile().put("restrictions",obj("noFloor" to true)));try{s.toggleMission(today(),0);fail("No debe confirmar un ejercicio incompatible")}catch(_:IllegalArgumentException){};assertFalse(s.logs.o(today()).a("items").getJSONObject(0).b("missionState"));assertEquals(0,s.stats().i("series"))}

}
