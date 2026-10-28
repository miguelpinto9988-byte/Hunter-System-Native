package com.huntersystem.app

import org.junit.Assert.*
import org.junit.Test

class ProfileEquipmentTest {
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
