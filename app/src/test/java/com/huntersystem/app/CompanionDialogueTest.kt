package com.huntersystem.app

import org.junit.Assert.*
import org.junit.Test

class CompanionDialogueTest {
 private val companions=listOf("knight","warrior","bear","insect","scientist")
 private fun profile(key:String)=obj("companionCharacter" to key,"difficulty" to "intermediate")

 @Test fun renderingTiersRespectHdMaximumAndStableFallback(){
  assertEquals(VisualTier.BALANCED,VisualTier.from("invalid"))
  assertEquals(listOf(1080,1080,720),listOf(VisualTier.MAX,VisualTier.BALANCED,VisualTier.SAVER).map{it.imageEdge})
  assertTrue(VisualTier.entries.all{it.imageEdge<=1080})
  assertTrue(VisualTier.SAVER.imageEdge<VisualTier.BALANCED.imageEdge)
  assertEquals(listOf(30,12,4),listOf(VisualTier.MAX,VisualTier.BALANCED,VisualTier.SAVER).map{it.stars})
  assertFalse(VisualTier.SAVER.animated)
  assertTrue(VisualTier.MAX.animated)
 }
 @Test fun allFiveCompanionsSpeakDifferentlyAtEveryTier(){
  for(tier in VisualTier.entries){
   val outputs=companions.map{CompanionDialogue.message(profile(it),tier,
    DialogueContext(DialogueMoment.WELCOME,group="upper"),"2026-10-28")}
   assertEquals("Voces duplicadas en "+tier,5,outputs.toSet().size)
  }
 }
 @Test fun balancedAndMaximumUseRealExerciseAndDifficultyContext(){
  val scene=DialogueContext(DialogueMoment.EXERCISE,group="lower",
   exercise="Sentadilla a silla",target=8,sets=3,set=2,cue="Apoya ambos pies.")
  for(tier in listOf(VisualTier.BALANCED,VisualTier.MAX)){
   val answer=CompanionDialogue.message(profile("scientist"),tier,scene,"2026-10-28")
   assertTrue(answer.contains("Sentadilla a silla"))
   assertTrue(answer.contains("8 repeticiones"))
  }
  val elaborate=CompanionDialogue.message(profile("knight"),VisualTier.MAX,scene)
  val brief=CompanionDialogue.message(profile("knight"),VisualTier.SAVER,scene)
  assertTrue(elaborate.length>brief.length)
  assertFalse(brief.contains("Sentadilla"))
  assertTrue(elaborate.contains("Apoya ambos pies"))
 }
 @Test fun batterySavingLinesArePrewrittenAndContextIndependent(){
  val p=profile("bear")
  val one=CompanionDialogue.message(p,VisualTier.SAVER,DialogueContext(DialogueMoment.SURVEY,group="upper"),"2026-10-28")
  val two=CompanionDialogue.message(p,VisualTier.SAVER,DialogueContext(DialogueMoment.SURVEY,group="cardio",rpe=9),"2026-10-30")
  assertEquals(one,two)
 }
 @Test fun highEffortChangesGuidanceWithoutClaimingPerformance(){
  val normal=CompanionDialogue.message(profile("scientist"),VisualTier.BALANCED,
   DialogueContext(DialogueMoment.COMPLETED,group="upper",rpe=3))
  val fatigue=CompanionDialogue.message(profile("scientist"),VisualTier.BALANCED,
   DialogueContext(DialogueMoment.COMPLETED,group="upper",rpe=9,fatigue=true))
  assertTrue(fatigue.contains("fatiga")||fatigue.contains("esfuerzo elevado"))
  assertNotEquals(normal,fatigue)
  assertTrue(fatigue.contains("9 de 10"))
 }
}
