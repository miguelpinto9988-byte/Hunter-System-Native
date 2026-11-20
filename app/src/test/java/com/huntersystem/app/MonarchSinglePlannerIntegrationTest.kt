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

    @Test fun fiveDayChallengeKeepsDailyMissionsAndEnforcesSeparateLimits() {
        val store=store()
        val monday=LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value-1).toLong())
        val repository=MonarchProgressRepository(store)
        val native = (0L..4L).associate { offset ->
            val date = monday.plusDays(offset)
            date to store.day(date.toString()).a("items").objects().map { it.s("id") }
        }
        repository.setMonarchConsent(monday.toString(),true,Category.UPPER,MonarchEncounter.FORTALEZA)
        val plan=repository.monarchPlan(monday)
        assertTrue(plan.accepted)
        assertEquals(MonarchEncounter.FORTALEZA, repository.monarchEncounter(monday.toString()))
        assertTrue(plan.days.any { it.ids.isNotEmpty() })
        val source = MonarchActivitySource(store)
        val profile = MonarchProfileAdapter.from(store)!!
        for (day in plan.days) {
            assertEquals(native.getValue(day.date), store.day(day.date.toString()).a("items")
                .objects().map { it.s("id") })
            assertEquals(profile.category(day.date), day.category)
            assertTrue(day.ids.size <= MonarchChallengePlanner.MAX_ACTIVITIES)
            if (day.category == null) assertTrue(day.ids.isEmpty())
            assertTrue(day.ids.all { id ->
                source.byId(id)?.let { it.category == day.category &&
                    EligibilityPolicy.compatible(it, profile, MonarchChallengePlanner.MAX_TECHNICAL)
                } == true
            })
        }
        val reloaded=MonarchProgressRepository(NativeStore(context)).monarchPlan(monday)
        assertEquals(plan.days.map { it.ids },reloaded.days.map { it.ids })
    }
    @Test fun exactRoutineWindowsUseReproducibleDistinctProposalsWithoutChangingMissions() {
        val store = store()
        val before = store.day(today()).toString()
        val day = LocalDate.parse(today())
        val p = store.adjusted()
        val proposals = listOf(10 to 3, 15 to 3, 20 to 4, 30 to 5, 45 to 6, 60 to 6)
            .flatMap { (minutes, steps) ->
                fun generate() = store.planner.generateRoutineOptions("offline", "upper", 2,
                    minutes, "synthetic-test-request", p, store.logs, day)
                val offers = generate()
                assertEquals(offers.map { it.toString() }, generate().map { it.toString() })
                assertTrue(offers.size <= 6)
                val sets = offers.map { it.a("items").objects().map { row -> row.s("id") }.toSet() }
                assertEquals(sets.size, sets.distinct().size)
                assertTrue(offers.all {
                    it.i("availableMinutes") == minutes && it.i("minutes") in 1..minutes &&
                        it.a("items").length() in 1..steps
                })
                offers
            }
        assertTrue(proposals.isNotEmpty())
        val repository = MonarchProgressRepository(store)
        val chosen = proposals.first()
        assertTrue(repository.canRunOptionalRoutine(day, chosen))
        assertEquals(0, repository.declareOptionalRoutine(day, chosen, false))
        assertEquals(10, repository.declareOptionalRoutine(day, chosen, true))
        assertEquals(0, repository.declareOptionalRoutine(day, chosen, true))
        assertEquals(before, store.day(today()).toString())
        assertFalse(store.logs.has(today()))

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

    @Test fun explicitRestAndAllSkippedDaysDoNotCountAsCompletedSessions() {
        val store = store()
        val items = store.day(today()).a("items")
        assertTrue(items.length() > 0)
        for (index in 0 until items.length()) store.setMissionStatus(today(), index, ParticipationStatus.SKIPPED)
        store.finish(today())
        assertEquals(0, store.stats().i("days"))
        val next = LocalDate.now().plusDays(1)
        store.saveProfile(store.profile!!.copyJson().put("weeklyCategories", arr(List(7) { 5 })))
        assertEquals("descanso", store.makeDay(next.toString()).s("progressCategory"))
        assertEquals(0, store.makeDay(next.toString()).a("items").length())
    }
}
