package com.huntersystem.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class GroupEvolutionTest {
 private val catalog=Catalog(loader={File("src/main/assets",it).readText()})
 private val profile=obj("weekFocus" to arr(listOf("upper","upper","lower","upper","lower","cardio","mobility")))
 private fun record(date:String,group:String,rpe:Int,fatigue:Boolean=false):JSONObject=
  obj("date" to date,"kind" to if(group=="cardio")"cardio" else if(group=="mobility")"recovery" else "strength",
   "trainingGroup" to group,"complete" to true,"pain" to false,"rpe" to rpe,"fatigue" to fatigue)
 private fun records(vararg rs:JSONObject):JSONObject=JSONObject().apply{for(r in rs)put(r.s("date"),r)}

 @Test fun eachFamilyUsesOnlyItsOwnPreviousEvaluation(){
  val logs=records(record("2026-10-26","upper",2),record("2026-10-27","lower",9))
  val upper=GroupEvolution.evaluate(logs,"2026-10-28","upper",profile,catalog)
  val lower=GroupEvolution.evaluate(logs,"2026-10-28","lower",profile,catalog)
  assertEquals(1,upper.steps)
  assertEquals(-1,lower.steps)
  for(group in listOf("core","cardio","mobility"))assertEquals(group,0,GroupEvolution.evaluate(logs,"2026-10-28",group,profile,catalog).steps)
  assertEquals("2026-10-26",upper.lastDate)
  assertEquals(2,upper.lastRpe)
 }
 @Test fun nextOccurrenceCanBeDaysOrWeeksLater(){
  val logs=records(record("2026-10-26","upper",2))
  assertEquals(1,GroupEvolution.evaluate(logs,"2026-10-28","upper",profile,catalog).steps)
  assertEquals(1,GroupEvolution.evaluate(logs,"2026-11-10","upper",profile,catalog).steps)
  assertEquals(0,GroupEvolution.evaluate(logs,"2026-11-20","upper",profile,catalog).steps)
  assertEquals(0,GroupEvolution.evaluate(logs,"2026-10-26","upper",profile,catalog).steps)
 }
 @Test fun threeWeekAndQuarterReviewsRequireEvidenceAndStayBounded(){
  val logs=records(
   record("2026-08-06","cardio",3),record("2026-08-13","cardio",3),
   record("2026-08-20","cardio",3),record("2026-09-02","cardio",3),
   record("2026-09-17","cardio",3),record("2026-10-10","cardio",3),
   record("2026-10-18","cardio",3),record("2026-10-24","cardio",3))
  val result=GroupEvolution.evaluate(logs,"2026-10-28","cardio",profile,catalog)
  assertEquals(2,result.steps)
  assertEquals(3,result.samples21)
  assertEquals(8,result.samples90)
  assertEquals(0,GroupEvolution.evaluate(logs,"2026-10-28","lower",profile,catalog).steps)
 }
 @Test fun fatigueBlocksIncreasingDifficultyEvenAfterGoodMonth(){
  val logs=records(record("2026-10-11","upper",3),record("2026-10-18","upper",3),
   record("2026-10-27","upper",8,true))
  assertTrue(GroupEvolution.evaluate(logs,"2026-10-28","upper",profile,catalog).steps<0)
  val incomplete=record("2026-10-27","core",1).put("complete",false)
  assertEquals(0,GroupEvolution.evaluate(records(incomplete),"2026-10-28","core",profile,catalog).steps)
 }
 @Test fun exercisesFromActualSessionDetermineGroupAndNotOtherMuscles(){
  val upper=catalog.exercises.first{it.type=="upper"}
  val lower=catalog.exercises.first{it.type=="lower"}
  val day=obj("date" to "2026-10-27","kind" to "strength","items" to arr(listOf(
   obj("id" to upper.id),obj("id" to lower.id),obj("id" to upper.id))))
  assertEquals("upper",GroupEvolution.sessionGroup(day,profile,catalog))
  val oldLog=day.copyJson().put("complete",true).put("rpe",2)
  assertEquals("upper",GroupEvolution.logGroup(oldLog,profile,catalog))
  val basic= obj("id" to upper.id,"target" to 10,"sets" to 2)
  val applied=GroupEvolution.apply(upper,basic,GroupEvolution.Adjustment("upper",1,"2026-10-27",2))
  if(upper.raw.optJSONObject("doseCap46")==null&&!upper.impact&&!upper.power)
   assertEquals(11,applied.i("target"))
  else assertEquals(10,applied.i("target"))
  assertEquals(10,GroupEvolution.apply(upper,basic,GroupEvolution.Adjustment("lower",-1)).i("target"))
  assertEquals(10,basic.i("target"))
 }
}
