package com.huntersystem.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de contrato de catálogo, sin presentar las 800 propuestas como ejercicios aprobados. */
class Hunter2000CatalogTest {
    private fun exercise(id:String,extra:Boolean=false):JSONObject=JSONObject().apply {
        put("id",id);put("name","Prueba $id");put("group","Tren superior")
        put("category","Fuerza");put("unit","reps");put("planning49",obj("group" to "upper"))
        put("sourceExpansion2k",extra);put("pendingTechnicalReview",extra)
        put("approvedForAutoplan",false);put("autoProgressionApproved",false)
    }
    private val original by lazy { JSONArray().apply {
        for(i in 1..1200)put(exercise("legacy_$i"))
    }.toString() }
    private val extra by lazy { JSONArray().apply {
        for(i in 1..800)put(exercise("hx_test_$i",true))
    }.toString() }
    private fun catalog(withExtra:Boolean, expansion:String=extra)=Catalog(loader={path->
        when(path){
            "catalog.json"->original
            "catalog-hunter2000-extra.json"->if(withExtra)expansion else error("Recursos no importados")
            "routines.json","profile-form.json","catalog-sports.json"->"[]"
            else->"{}"
        }
    })

    @Test fun funcionaCon1200MientrasElPaqueteEstaPendiente(){
        val c=catalog(false)
        assertEquals(1200,c.exercises.size)
        assertEquals(1200,c.byId.size)
        assertTrue(c.expansionExercises.isEmpty())
    }

    @Test fun conserva2000IdsParaLecturaYSeleccionaLos1200Originales(){
        val c=catalog(true)
        assertEquals(1200,c.originalExercises.size)
        assertEquals(800,c.expansionExercises.size)
        assertEquals(1200,c.exercises.size)
        assertEquals(2000,c.byId.size)
        val rules=Rules(c)
        for(e in c.expansionExercises){
            assertFalse(e.raw.optBoolean("approvedForAutoplan"))
            assertFalse(e.validationBlocked)
            assertTrue(rules.reasons(e,JSONObject()).isNotEmpty())
        }
    }

    @Test fun deportesDisponiblesYMaterialesObligatorios() {
        val c=Catalog(loader={java.io.File("src/main/assets",it).readText()})
        val originalDisciplines = setOf(
            "Fútbol", "Calistenia", "Yoga", "Pilates", "Baloncesto", "Voleibol",
            "Tenis", "Atletismo", "Boxeo", "Gimnasia", "Levantamiento de pesas", "Ciclismo"
        )
        assertTrue(c.sportExercises.map { it.sport }.toSet().containsAll(originalDisciplines))
        assertTrue(c.sportExercises.all{it.raw.getJSONArray("instructions").length()>0})
        val wallDrill=c.byId.getValue("sport-track-a-wall")
        val profile=JSONObject().put("age",30).put("difficulty","basic")
        assertFalse(Rules(c).compatible(wallDrill,profile))
        profile.put("materials46",JSONArray().put("wall"))
        assertTrue(Rules(c).compatible(wallDrill,profile))
        assertFalse(Rules(c).compatible(c.byId.getValue("sport-futbol-sole"),profile))
    }

    @Test fun usaSoloElInventarioOriginalParaNuevosPlanesYResuelveElHistorial(){
        val c=Catalog(loader={java.io.File("src/main/assets",it).readText()})
        val profile=JSONObject().put("age",30).put("difficulty","basic")
        val rules=Rules(c)
        val basic=c.byId.getValue("hx_core_0002")
        assertTrue(rules.reasons(basic,profile).joinToString(),rules.compatible(basic,profile))
        assertFalse(rules.compatible(c.byId.getValue("hx_upper_0002"),profile))
        assertFalse(rules.compatible(c.byId.getValue("hx_upper_0001"),profile))
        val planner=Planner(c,rules)
        val date=java.time.LocalDate.parse("2026-11-16")
        val coreProfile=profile.copyJson().put("weeklyCategories",arr(List(7){1}))
        val native=checkNotNull(MonarchProfileAdapter.from(coreProfile,date))
        val source=MonarchActivitySource(c,rules,coreProfile)
        val originalIds=c.originalExercises.map { it.id }.toSet()
        assertEquals(1200,originalIds.size)
        assertTrue(source.all().all { it.id in originalIds })
        assertTrue(source.byId(basic.id)==null)
        assertTrue(source.savedExercise(basic.id)?.automaticEligible==true)
        val core=com.huntersystem.app.planning.Category.CENTRAL
        val selected=com.huntersystem.app.planning.DailyMission(date,core,
            com.huntersystem.app.planning.ProfileSignature.of(native),source.signature(),
            listOf(basic.id),"Candidato de expansión admisible")
        val plannedIds=planner.choose(coreProfile,"strength","hunter",
            date=date.toString(),trainingProfile=native,pinned=selected).a("items").objects().map{it.s("id")}
        assertTrue(plannedIds.isNotEmpty())
        assertTrue(plannedIds.all { it in originalIds })
        val day=obj("items" to arr(listOf(planner.libraryDose(basic,profile,"strength"))))
        assertTrue(planner.steps(day,profile).any{it.s("id")==basic.id})
        val legacy=c.byId.getValue("heelcontrolled")
        assertEquals("heel_slow",legacy.id)
        assertEquals("heelcontrolled",c.originalExercise("heelcontrolled")?.id)
        assertTrue(c.matchesExercise(legacy,normalize("Deslizamiento de talón con retorno lento")))
        assertFalse(c.exercises.any{it.id=="heelcontrolled"||it.id=="birdcontrolled"})
        day.put("items",arr(listOf(obj("id" to "heelcontrolled","sets" to 1,"target" to 5))))
        assertTrue(planner.steps(day,profile).any{it.s("id")=="heel_slow"})
    }

    @Test(expected=IllegalArgumentException::class)
    fun rechazaIdentificadoresDuplicados(){
        val arr=JSONArray(extra);arr.put(0,exercise("legacy_1",true))
        catalog(true,arr.toString())
    }
}
