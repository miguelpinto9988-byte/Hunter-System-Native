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
            "routines.json","profile-form.json"->"[]"
            else->"{}"
        }
    })

    @Test fun funcionaCon1200MientrasElPaqueteEstaPendiente(){
        val c=catalog(false)
        assertEquals(1200,c.exercises.size)
        assertEquals(1200,c.byId.size)
        assertTrue(c.expansionExercises.isEmpty())
    }

    @Test fun fusiona2000IdsSinAutorizarLas800Propuestas(){
        val c=catalog(true)
        assertEquals(1200,c.originalExercises.size)
        assertEquals(800,c.expansionExercises.size)
        assertEquals(2000,c.exercises.size)
        assertEquals(2000,c.byId.size)
        val rules=Rules(c)
        for(e in c.expansionExercises){
            assertFalse(e.raw.optBoolean("approvedForAutoplan"))
            assertTrue(rules.reasons(e,JSONObject()).isNotEmpty())
        }
    }

    @Test(expected=IllegalArgumentException::class)
    fun rechazaIdentificadoresDuplicados(){
        val arr=JSONArray(extra);arr.put(0,exercise("legacy_1",true))
        catalog(true,arr.toString())
    }
}
