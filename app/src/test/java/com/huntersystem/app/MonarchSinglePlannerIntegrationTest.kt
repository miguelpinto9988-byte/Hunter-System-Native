package com.huntersystem.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.huntersystem.app.planning.*
import java.time.LocalDate
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Integración del único Planner con las pantallas/registros nativos antiguos. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class MonarchSinglePlannerIntegrationTest {
    private lateinit var context: Context

    @Before fun reset() {
        context = ApplicationProvider.getApplicationContext()
        context.filesDir.listFiles()?.filter {
            it.name.startsWith("hunter-system-native")
        }?.forEach { it.delete() }
    }

    private fun store(): NativeStore {
        val fixture = JSONArray(javaClass.classLoader!!
            .getResource("groups-golden.json")!!.readText()).objects()[7].o("p")
        val p=fixture.copyJson()
            .put("name","Cazador")
            .put("weekPlan",arr(List(7){"train"}))
            .put("weekFocus",arr(List(7){"upper"}))
            .put("dailyMissions",3)
        return NativeStore(context).also { it.saveProfile(p) }
    }

    @Test fun nativeDayContainsOnlySinglePlayablePlan() {
        val store=store()
        val day=store.day(today())
        val profile=MonarchProfileAdapter.from(store)!!
        val chosen=day.a("items").objects()
        assertTrue(day.b("unifiedPlanning"))
        assertEquals(profile.category(LocalDate.parse(today()))?.code,
            day.s("progressCategory"))
        assertTrue(chosen.isNotEmpty())
        val source=MonarchActivitySource(store)
        for (item in chosen) {
            val meta=source.byId(item.s("id"))!!
            assertTrue(EligibilityPolicy.compatible(meta,profile))
            assertTrue(store.rules.compatibleWithMission(store.catalog.byId.getValue(meta.id),
                store.adjusted(), item))
        }
        val persisted=NativeStore(context).day(today())
        assertEquals(chosen.map{it.s("id")},
            persisted.a("items").objects().map{it.s("id")})
    }

    @Test fun skippedMissionAndNotesStayOnTheHistoricalLog() {
        val store=store()
        val date=today()
        val original=store.day(date).a("items").getJSONObject(0).s("id")
        store.setMissionStatus(date,0,ParticipationStatus.SKIPPED,"Omitida hoy")
        val restored=NativeStore(context)
        assertTrue(restored.activeLog(restored.logs.optJSONObject(date)))
        assertEquals(original,restored.log().a("items").getJSONObject(0).s("id"))
        assertEquals("SKIPPED",restored.log().a("items").getJSONObject(0).s("progressStatus"))
        assertTrue(MonarchProgressRepository(restored).participation().any {
            it.date==LocalDate.parse(date)&&it.variantId==original&&
                it.status==ParticipationStatus.SKIPPED
        })
    }

    @Test fun fiveDayChallengeCannotInventDifferentNativeActivities() {
        val store=store()
        val monday=LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value-1).toLong())
        val repository=MonarchProgressRepository(store)
        repository.setMonarchConsent(monday.toString(),true,Category.UPPER)
        val plan=repository.monarchPlan(monday)
        assertTrue(plan.accepted)
        assertTrue(plan.days.any { it.ids.isNotEmpty() })
        for(day in plan.days){
            val nativeIds=store.day(day.date.toString()).a("items")
                .objects().map { it.s("id") }.toSet()
            assertEquals("Mismo plan "+day.date, nativeIds,day.ids.toSet())
        }
        val reloaded=MonarchProgressRepository(NativeStore(context)).monarchPlan(monday)
        assertEquals(plan.days.map { it.ids },reloaded.days.map { it.ids })
    }
    @Test fun explicitlySavedEffortUsesTodayCategoryNotAnotherDay() {
        val store=store()
        val day=store.day(today())
        store.feedback(today(),5,"Registro voluntario")
        val log=store.logs.o(today())
        assertEquals(day.s("progressCategory"),log.s("categoryRpe"))
        assertEquals(5,log.i("rpe"))
        val r=MonarchProgressRepository(NativeStore(context))
        assertTrue(r.efforts().any {
            it.date==LocalDate.parse(today())&&it.rpe==5&&
                it.category==CategoryMapper.normalize(day.s("progressCategory"))
        })
    }
    @Test fun writtenGoalStaysAdvisoryAndTheExplicitCalendarStaysInControl() {
        val store = store()
        val text = "No quiero hacer saltos. Tengo dolor. Quiero caminar."
        val existing = store.profile!!.copyJson()
        val originalRestrictions = existing.o("restrictions").toString()
        val originalWeek = existing.a("weeklyCategories").toString()
        store.saveProfile(existing.put("finalGoal", text))
        assertEquals(text, store.profile!!.s("custom_goal"))
        assertEquals(originalRestrictions, store.adjusted().o("restrictions").toString())
        assertEquals(originalWeek, store.profile!!.a("weeklyCategories").toString())
        assertEquals(setOf("caminar"), PersonalGoalInterpreter.interpret(text).tags)
    }

    @Test fun interruptionAndGlobalEffortArePersistedWithoutInventingCompletion() {
        val store = store()
        val day = LocalDate.parse(today())
        val ids = store.day(today()).a("items").objects().map { it.s("id") }
        assertTrue(ids.isNotEmpty())
        store.interruptSession(today())
        val restored = NativeStore(context)
        val repository = MonarchProgressRepository(restored)
        assertTrue(repository.efforts().isEmpty())
        assertEquals(listOf(SessionInterruption(day, Category.UPPER)), repository.interruptions())
        assertTrue(repository.plannedParticipation().none {
            it.date == day && it.status != ParticipationStatus.PENDING
        })
        restored.feedback(today(), 8, "Valoración voluntaria")
        val observations = MonarchProgressRepository(NativeStore(context)).efforts()
        assertEquals(setOf(null, Category.UPPER), observations.filter { it.date == day }.map { it.category }.toSet())
        assertTrue(observations.filter { it.date == day }.all { it.rpe == 8 && !it.completed })
        assertEquals(ids, NativeStore(context).day(today()).a("items").objects().map { it.s("id") })
    }
}
