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
 // The fixtures compare historical definitions. Alias resolution has its own catalog test.
 @Test fun classificationAndDoses(){val historical=c.originalExercises.associateBy{it.id};val selected=listOf("chair","goblet","row").map{c.byId[it]!!};for(test in rows("groups").filter{it.o("p").i("age") in 18..64}){val p=test.o("p");for(row in test.a("rows").objects()){val e=historical[row.s("id")]?:c.byId.getValue(row.s("id"));val label=p.s("difficulty")+"/"+e.id;assertEquals(label,row.a("reasons").strings(),r.groupReasons(e,p));for((i,kind)in listOf("strength","cardio","recovery").withIndex()){assertEquals(label,row.a("scores").optDouble(i),r.score(e,p,kind,selected),.001);val x=obj("id" to row.s("id"),"sets" to 3,"target" to 40,"rest" to 10,"safetyNote" to "BASE");assertEquals(label,canonical(row.a("doses").getJSONObject(i)),canonical(r.groupDose(e,p,kind,x,JSONObject(),"2026-10-20")))}}}}
 @Test fun historicalCompatibilityPools(){
  for(test in rows("groups").filter{it.o("p").i("age") in 18..64}){
   val profile=test.o("p")
   for(kind in listOf("strength","cardio","recovery")){
    val actual=c.historicalExercises.asSequence()
     .filter{it.sport.isBlank()&&!it.hunter2000}
     .filter{exercise->
      when(kind){
       "strength"->exercise.type in setOf("upper","lower","core")
       "cardio"->exercise.type=="cardio"||exercise.meta.b("jumpPower")
       else->exercise.type=="mobility"
      }
     }
     .filter{r.gate(profile).ok&&r.compatible(it,profile)}.map{it.id}.toList()
    val expected=test.o("pools").a(kind).strings().filter{c.byId[it]?.id==it}
    assertEquals("${profile.s("difficulty")} $kind",expected,actual)
   }
  }
 }
 @Test fun ageDoesNotChangeAutomaticEligibility(){
  val adult=rows("groups").first().o("p")
  val reference=r.pool(adult,"strength").map { it.id }
  for(age in listOf(12,70,105)){
   val profile=adult.copyJson().put("age",age)
   assertEquals(r.gate(adult),r.gate(profile))
   assertEquals(reference,r.pool(profile,"strength").map { it.id })
  }
 }
 @Test fun missionSelection(){
  for(test in rows("missions")){
   val p=test.o("p")
   val day=test.s("date")
   val kind=test.s("kind")
   val selected=planner.choose(p,kind,test.s("seed"),JSONObject(),day)
   val again=planner.choose(p,kind,test.s("seed"),JSONObject(),day)
   val items=selected.a("items").objects()
   assertEquals(test.s("seed"),canonical(itemsToArray(items)),canonical(again.a("items")))
   assertEquals(items.size,items.map{it.s("id")}.toSet().size)
   val profile=MonarchProfileAdapter.from(p,java.time.LocalDate.parse(day))!!
   val source=MonarchActivitySource(c,r,p)
   for(item in items){
    val e=c.byId[item.s("id")]!!
    val metadata=source.byId(e.id)!!
    assertTrue(test.s("seed")+"/"+e.id,
     com.huntersystem.app.planning.EligibilityPolicy.compatible(metadata,profile))
    assertTrue(r.compatibleWithMission(e,p,item))
    assertTrue(item.i("sets")>=1&&item.i("target")>=1)
   }
   for(own in test.a("own").objects()){
    assertEquals("own "+test.s("seed")+"/"+own.s("id"),
     canonical(own.o("dose")),
     canonical(planner.libraryDose(c.byId[own.s("id")]!!,p,kind).put("id",own.s("id"))))
   }
  }
 }
 private fun itemsToArray(items:List<JSONObject>)=arr(items)
 @Test fun independentLibrary(){
  for(test in rows("library")){
   val type=test.o("category").s("id")
   val level=test.i("level")
   val p=test.o("p")
   val date=test.s("date")
   val actual=planner.generateRoutine(test.s("mode"),type,level,0,
    test.o("range").s("id"),1,p,JSONObject(),date)?:continue
   val repeated=planner.generateRoutine(test.s("mode"),type,level,0,
    test.o("range").s("id"),1,p,JSONObject(),date)
   assertEquals(canonical(actual.a("items")),canonical(repeated?.a("items")))
   val adjusted=p.copyJson().put("difficulty",listOf("basic","intermediate","advanced")[level])
   val profile=MonarchProfileAdapter.from(adjusted,java.time.LocalDate.parse(date))!!
   val source=MonarchActivitySource(c,r,adjusted)
   val items=actual.a("items").objects()
   assertTrue(items.isNotEmpty())
   assertEquals(items.size,items.map{it.s("id")}.toSet().size)
   for(item in items){
    val e=c.byId[item.s("id")]!!
    val metadata=source.byId(e.id)!!
    assertTrue(com.huntersystem.app.planning.EligibilityPolicy.compatible(
     metadata,profile,if(level==0)2 else 3))
    assertTrue(r.compatible(e,adjusted))
    assertTrue(item.i("sets")>=1&&item.i("target")>=1)
   }
  }
 }
}
