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
        put("category","Fuerza");put("unit","reps")
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

    @Test fun fusiona2000IdsSinUsarLaRevisionComoBloqueo(){
        val c=catalog(true)
        assertEquals(1200,c.originalExercises.size)
        assertEquals(800,c.expansionExercises.size)
        assertEquals(2000,c.exercises.size)
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
        assertEquals(12,c.sportExercises.map{it.sport}.distinct().size)
        assertTrue(c.sportExercises.all{
            it.raw.getJSONArray("instructions").length()>0 &&
                java.io.File("src/main/assets",it.demo).isFile
        })
        val wallDrill=c.byId.getValue("sport-track-a-wall")
        val profile=JSONObject().put("age",30).put("difficulty","basic")
        assertFalse(Rules(c).compatible(wallDrill,profile))
        profile.put("materials46",JSONArray().put("wall"))
        assertTrue(Rules(c).compatible(wallDrill,profile))
        assertFalse(Rules(c).compatible(c.byId.getValue("sport-futbol-sole"),profile))
    }

    @Test fun incorporaHunter2000YResuelveAliasHistoricos(){
        val c=Catalog(loader={java.io.File("src/main/assets",it).readText()})
        val profile=JSONObject().put("age",30).put("difficulty","basic")
        val rules=Rules(c)
        val basic=c.byId.getValue("hx_core_0002")
        assertTrue(rules.reasons(basic,profile).joinToString(),rules.compatible(basic,profile))
        assertFalse(rules.compatible(c.byId.getValue("hx_upper_0002"),profile))
        assertFalse(rules.compatible(c.byId.getValue("hx_upper_0001"),profile))
        assertTrue(MonarchActivitySource(c,rules,profile).all().any{it.id==basic.id&&it.automaticEligible})
        val planner=Planner(c,rules)
        val coreProfile=profile.copyJson().put("weekFocus",arr(List(7){"core"}))
        val dayOfTest="2026-11-16"
        assertTrue((0..15).any{ seed ->
            planner.choose(coreProfile,"strength","hunter-$seed",date=dayOfTest)
                .a("items").objects().any{c.byId[it.s("id")]?.hunter2000==true}
        })
        assertTrue((0..15).any{ variant ->
            planner.generateRoutine("home","core",0,variant,"short",1,coreProfile,date=dayOfTest)
                ?.a("items")?.objects()?.any{c.byId[it.s("id")]?.hunter2000==true}==true
        })
        val day=obj("items" to arr(listOf(planner.libraryDose(basic,profile,"strength"))))
        assertTrue(planner.steps(day,profile).any{it.s("id")==basic.id})
        val legacy=c.byId.getValue("heelcontrolled")
        assertEquals("heel_slow",legacy.id)
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
