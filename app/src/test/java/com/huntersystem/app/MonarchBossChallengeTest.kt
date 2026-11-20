package com.huntersystem.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.huntersystem.app.planning.*
import org.json.JSONArray
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowDialog
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w393dp-h851dp-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MonarchBossChallengeTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private lateinit var context:Context
    private lateinit var store:NativeStore
    private val date=LocalDate.of(2026,11,20)
    private val start=1_800_000_000_000L

    @Before fun setup(){
        context=ApplicationProvider.getApplicationContext()
        context.filesDir.listFiles()?.filter { it.name.startsWith("hunter-system-native") }?.forEach { it.delete() }
        val fixture=JSONArray(javaClass.classLoader!!.getResource("groups-golden.json")!!.readText())
            .objects()[7].o("p")
        val profile=fixture.copyJson().put("name","Cazador").put("animate",false).put("theme","dark")
            .put("weeklyCategories",arr(List(7){5}))
        store=NativeStore(context)
        store.saveProfile(profile)
    }

    @Test fun drawAcrossCategoriesOnlyUsesCompatibleRepetitionActivities() {
        val profile=TrainingProfile(Experience.INTERMEDIATE,date,List(7){5})
        val compatible=Category.values().mapIndexed { index,category ->
            ActivityMetadata("id-$index",category,"family-$index","type-$index",2,Intensity.LOW,
                emptySet(),"body",false,true,displayName="Actividad $index",workUnit=VolumeUnit.REPETITIONS)
        }
        val candidates=compatible+listOf(
            compatible[0].copy(id="duplicate"),
            compatible[0].copy(id="timed",displayName="Tiempo",workUnit=VolumeUnit.SECONDS),
            compatible[0].copy(id="equipment",displayName="Equipo",equipment=setOf("db")),
            compatible[0].copy(id="advanced",displayName="Nivel",technicalLevel=4),
            compatible[0].copy(id="no-demo",displayName="Sin demo")
        )
        val offer=planMonarchBoss(MonarchBoss.IGRIS,profile,candidates,7){it!="no-demo"}!!
        assertEquals(5,offer.goals.size)
        assertEquals(compatible.map { it.displayName }.toSet(),
            offer.goals.map { goal -> candidates.single { it.id==goal.id }.displayName }.toSet())
        assertTrue(offer.goals.all { it.target in 80..100 })
        assertEquals(offer,planMonarchBoss(MonarchBoss.IGRIS,profile,candidates.reversed(),7){it!="no-demo"})
        assertNull(planMonarchBoss(MonarchBoss.IGRIS,profile,compatible.dropLast(1),7))
    }

    @Test fun accumulateAcrossDaysWithoutChangingNormalMissionsAndKeepProgressAfterReload(){
        val snapshot=store.logs.toString()
        val repository=MonarchBossChallenges(store)
        val offer=repository.offer(date)!!
        val goal=offer.goals.first()
        assertNull(repository.current())
        repository.start(offer,date,start)
        assertThrows(IllegalArgumentException::class.java){repository.start(offer,date,start+1)}
        val dayThree=start+2*86_400_000L
        repository.record(start,goal.id,11,0,start)
        repository.record(start,goal.id,12,11,dayThree)
        assertThrows(IllegalArgumentException::class.java){repository.record(start,goal.id,12,11,dayThree)}
        assertThrows(IllegalStateException::class.java){
            repository.record(start,goal.id,3,23,start+monarchBossDuration)
        }
        val restored=NativeStore(context)
        val reopened=MonarchBossChallenges(restored)
        val run=reopened.current()!!
        assertEquals(3,run.day(dayThree))
        assertEquals(23,run.offer.goals.first().count)
        assertEquals(BossStatus.EXPIRED,run.status(start+monarchBossDuration))
        assertEquals(snapshot,restored.logs.toString())
        assertEquals(MonarchBoss.IGRIS,reopened.offer(date.plusDays(5))!!.boss)
        val chosen=reopened.offer(date.plusDays(5),MonarchBoss.BERU)!!
        assertTrue(chosen.goals.all { it.target in 135..170 })
        reopened.start(chosen,date.plusDays(5),start+monarchBossDuration)
        assertEquals(chosen,MonarchBossChallenges(NativeStore(context)).current()!!.offer)
        assertEquals(1,restored.state.a("monarchBossHistory").length())
    }

    @Test fun legacyBeruRetainsHisTargetsAndProgressWhenUpdated() {
        val repository=MonarchBossChallenges(store)
        val original=repository.offer(date)!!
        repository.start(original,date,start)
        store.state.o("monarchBossChallenge").put("boss","BERU")
        store.save()
        val restored=NativeStore(context)
        val migrated=MonarchBossChallenges(restored)
        val goal=original.goals.first()
        migrated.record(start,goal.id,12,0,start+1)
        val saved=MonarchBossChallenges(NativeStore(context)).current()!!
        assertEquals(MonarchBoss.BERU,saved.offer.boss)
        assertEquals(goal.copy(count=12),saved.offer.goals.first())
        assertEquals(start+monarchBossDuration,saved.deadline)
    }

    @Test fun selectedDifficultyIncreasesTheRangeKeepingProfileFilters() {
        val repository=MonarchBossChallenges(store)
        val offers=MonarchBoss.entries.map { repository.offer(date,it)!! }
        assertEquals(listOf(75..110,90..125,105..140,120..155,135..170),
            offers.map { it.boss.repetitions })
        for(offer in offers) {
            assertEquals(5,offer.goals.distinctBy { it.id }.size)
            assertTrue(offer.goals.all { it.target in offer.boss.repetitions && repository.compatible(it.id) })
        }
    }

    @Test fun challengeOnlyCompletesWhenAllDeclaredTargetsAreReached(){
        val repository=MonarchBossChallenges(store)
        val offer=repository.offer(date)!!
        repository.start(offer,date,start)
        offer.goals.forEach { goal -> repository.record(start,goal.id,goal.target,0,start+1) }
        assertEquals(BossStatus.COMPLETED,repository.current()!!.status(start+2))
        assertThrows(IllegalStateException::class.java){repository.abandon(start,start+2)}
    }

    @Test(timeout=120000) fun eachNewBossDisplaysItsCoverInTheChallengeWindow() {
        val ui=NativeUi(store)
        compose.setContent { HunterTheme(store) { HunterRoot(ui) } }
        val output=File("../qa").apply { mkdirs() }
        for(boss in MonarchBoss.entries.filter { it!=MonarchBoss.IGRIS }) {
            compose.runOnIdle {
                ui.stack.clear()
                ui.open("monarch-boss",obj("selectedBoss" to boss.name))
            }
            val cover=hasContentDescription("Ilustración de ${boss.title}")
            compose.waitUntil(10_000) { compose.onAllNodes(cover).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(cover).assertIsDisplayed()
            compose.runOnIdle {
                val view=ShadowDialog.getLatestDialog()?.window?.decorView ?: return@runOnIdle
                if(view.width>0&&view.height>0) {
                    val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(bitmap))
                    File(output,"native-monarca-${boss.name.lowercase()}.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
                    }
                }
            }
        }
    }

    @Test(timeout=120000) fun catalogDemonstrationPrecedesCounterAndOnlyExplicitSaveAddsRepetitions(){
        val ui=NativeUi(store)
        compose.setContent{HunterTheme(store){HunterRoot(ui)}}
        compose.onNodeWithText("Ver Misiones Monarca").performClick()
        compose.onNodeWithText("Igris · Inicial · 75–110 reps",substring=true).performScrollTo().performClick()
        compose.onNodeWithText("Beru · Monarca · 135–170 reps").performClick()
        compose.onNodeWithText("135–170").assertExists()
        val repository=MonarchBossChallenges(store)
        val offer=repository.offer(LocalDate.now(),MonarchBoss.BERU)!!
        val goal=offer.goals.first()
        val name=store.catalog.byId.getValue(goal.id).name
        compose.onNodeWithText(name).performScrollTo().performClick()
        compose.onNodeWithText("El registro se habilita al aceptar el desafío.").assertExists()
        compose.runOnIdle { ui.back() }
        compose.onNodeWithText("135–170").assertExists()
        compose.onNodeWithText("Aceptar desafío").performScrollTo().performClick()
        compose.onNodeWithText(name).performScrollTo().performClick()
        compose.onNodeWithText("DEMOSTRACIÓN").assertExists()
        compose.onNodeWithText("Registrar repeticiones").assertExists()
        compose.onNodeWithText("Reproducir demostración").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("+").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0,repository.current()!!.offer.goals.first().count) }
        compose.onNodeWithText("Guardar repeticiones").performScrollTo().performClick()
        compose.onNodeWithText("1 / ${goal.target} repeticiones acumuladas").assertExists()
        compose.runOnIdle {
            assertEquals(MonarchBoss.BERU,repository.current()!!.offer.boss)
            assertEquals(1,repository.current()!!.offer.goals.first().count)
        }
    }
}
