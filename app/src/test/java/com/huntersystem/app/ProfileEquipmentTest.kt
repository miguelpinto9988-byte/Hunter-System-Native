package com.huntersystem.app

import org.junit.Assert.*
import org.junit.Test

class ProfileEquipmentTest {
 @Test fun profileFormContainsOnlyVisibleSectionsAndFields(){
  val form=Catalog(loader={java.io.File("src/main/assets",it).readText()}).profileForm.objects()
  assertEquals(5,form.size)
  assertEquals(listOf("Foto de perfil","Tu compañero","Objetivo y equipo","¿Desafíos o rutinas completas?","Perfil"),form.map{it.s("title")})
  val visible=form.flatMap{it.a("fields").objects()}.map{it.s("name")}
  for(retired in listOf("trainingScale","materials46","capabilities46","scene33","collection33","animate","gym_barbell","gym_machine"))
   assertFalse("Campo obsoleto: $retired",retired in visible)
  assertTrue("equipment" in visible)
  assertTrue("trainingMode" in visible)
  val companion=form[1].a("fields").objects().map{it.s("name")}
  val objective=form[2].a("fields").objects().map{it.s("name")}
  assertTrue(listOf("difficulty","goalGroup","finalGoal","equipment").all{it in objective})
  assertTrue(listOf("difficulty","goalGroup","finalGoal").none{it in companion})
  for(key in listOf("difficulty","goalGroup","finalGoal","equipment"))
   assertEquals("Campo duplicado: $key",1,visible.count{it==key})
 }
 @Test fun optionsDependOnSelectedEquipment(){
  assertTrue(profileAllowedMaterials("body").contains("pullbar"))
  assertTrue(profileAllowedMaterials("body").contains("mat"))
  assertFalse(profileAllowedMaterials("body").contains("rack"))
  assertFalse(profileAllowedMaterials("body").contains("kb"))
  assertTrue(profileAllowedMaterials("db").contains("kb"))
  assertFalse(profileAllowedMaterials("db").contains("rack"))
  assertTrue(profileAllowedMaterials("gym").contains("rack"))
  assertEquals(42,profileAllowedMaterials("gym").size)
  assertFalse(profileAllowedCapabilities("body").contains("olympic"))
  assertFalse(profileAllowedCapabilities("body").contains("overhead"))
  assertTrue(profileAllowedCapabilities("gym").contains("olympic"))
 }
 @Test fun hiddenMachineOptionsDoNotLeakIntoBodyweightProfile(){
  val selected=mapOf(
   "gym_barbell" to "true","gym_machine" to "true",
   "materials46|space" to "true","materials46|rack" to "true","materials46|kb" to "true",
   "capabilities46|singlebalance" to "true","capabilities46|olympic" to "true")
  assertEquals(emptyList<String>(),selectedProfileGym("body",selected))
  assertEquals(listOf("space"),selectedProfileMaterials("body",selected))
  assertEquals(listOf("singlebalance"),selectedProfileCapabilities("body",selected))
  assertEquals(listOf("barbell","machine"),selectedProfileGym("gym",selected))
  assertEquals(listOf("space","rack","kb"),selectedProfileMaterials("gym",selected))
  assertEquals(listOf("singlebalance","olympic"),selectedProfileCapabilities("gym",selected))
 }
 @Test fun compactOptionsKeepExplicitKeysAndDoNotInferMissingEquipment(){
  val body=profileCompactEquipment("body")
  val dumbbells=profileCompactEquipment("db")
  val gym=profileCompactEquipment("gym")
  assertTrue(body.size<=6)
  assertTrue(dumbbells.size<=7)
  assertTrue(gym.size<=10)
  assertFalse(body.flatMap{it.keys}.any{it.startsWith("gym_")})
  assertFalse(dumbbells.flatMap{it.keys}.any{it.startsWith("gym_")})
  assertTrue(gym.flatMap{it.keys}.contains("gym_barbell"))
  assertFalse(profileCompactSelected(profileCompactMaterials[0],mapOf("materials46|space" to "true")))
  assertTrue(profileCompactSelected(profileCompactMaterials[0],mapOf(
   "materials46|space" to "true","materials46|wall" to "true")))
 }
 @Test fun previouslySavedDetailedEquipmentIsNotSilentlyDiscarded(){
  val stored=mapOf(
   "materials46|space" to "true",
   "materials46|rack" to "true",
   "materials46|abwheel" to "true",
   "capabilities46|nordic" to "true"
  )
  val previous=profileLegacySelections("body",stored)
  assertTrue("materials46|rack" in previous)
  assertTrue("materials46|abwheel" in previous)
  assertTrue("capabilities46|nordic" in previous)
  assertFalse("materials46|space" in previous)
  assertEquals(listOf("space","abwheel"),selectedProfileMaterials("body",stored).sorted().reversed())
  assertTrue(profileCompactCapabilities("body").none{it.keys.contains("capabilities46|olympic")})
 }
}
