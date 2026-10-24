package com.huntersystem.app
import java.io.File
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Test
import org.junit.Assert.*
class ParityTest {
 val c=Catalog(loader={File("src/main/assets",it).readText()});val r=Rules(c);val planner=Planner(c,r)
 fun rows(n:String)=JSONArray(javaClass.classLoader!!.getResource("$n-golden.json")!!.readText()).objects()
 fun canonical(v:Any?):Any?=when(v){is JSONObject->v.entries().associate{it.first to canonical(it.second)}.toSortedMap();is JSONArray->(0 until v.length()).map{canonical(v.opt(it))};is Number->v.toDouble();JSONObject.NULL->null;else->v}
 @Test fun classificationAndDoses(){val selected=listOf("chair","goblet","row").map{c.byId[it]!!};for(test in rows("groups")){val p=test.o("p");for(row in test.a("rows").objects()){val e=c.byId[row.s("id")]!!;val label=p.s("difficulty")+"/"+e.id;assertEquals(label,row.a("reasons").strings(),r.groupReasons(e,p));for((i,kind)in listOf("strength","cardio","recovery").withIndex()){assertEquals(label,row.a("scores").optDouble(i),r.score(e,p,kind,selected),.001);val x=obj("id" to e.id,"sets" to 3,"target" to 40,"rest" to 10,"safetyNote" to "BASE");assertEquals(label,canonical(row.a("doses").getJSONObject(i)),canonical(r.groupDose(e,p,kind,x,JSONObject(),"2026-10-20")))}}}}
 @Test fun compatibilityPools(){for(test in rows("groups")){val p=test.o("p");for(kind in listOf("strength","cardio","recovery")){val actual=r.pool(p,kind).map{it.id};val expected=test.o("pools").a(kind).strings();assertEquals("${p.s("difficulty")} ${p.o("health")} $kind missing=${expected-actual.toSet()} extra=${actual-expected.toSet()}",expected,actual)}}}
 @Test fun missionSelection(){for(test in rows("missions")){val p=test.o("p");val actual=planner.choose(p,test.s("kind"),test.s("seed"),JSONObject(),test.s("date"));val expected=test.o("result");assertEquals(test.s("seed"),canonical(expected.a("items")),canonical(actual.a("items")));for(own in test.a("own").objects()){assertEquals("own ${test.s("seed")} ${own.s("id")}",canonical(own.o("dose")),canonical(planner.libraryDose(c.byId[own.s("id")]!!,p,test.s("kind"))))}}}
 @Test fun independentLibrary(){for(test in rows("library")){val expected=test.optJSONObject("result");val actual=planner.generateRoutine(test.s("mode"),test.o("category").s("id"),test.i("level"),0,test.o("range").s("id"),1,test.o("p"),JSONObject(),test.s("date"));val label="${test.s("mode")} ${test.o("category").s("id")} ${test.i("level")} ${test.o("range").s("id")}";assertEquals(label,expected==null,actual==null);if(expected!=null&&actual!=null){assertEquals(label,canonical(expected.a("items")),canonical(actual.a("items")));assertEquals(label,expected.i("minutes"),actual.i("minutes"))}}}
}
