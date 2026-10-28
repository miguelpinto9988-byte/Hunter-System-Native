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
}
