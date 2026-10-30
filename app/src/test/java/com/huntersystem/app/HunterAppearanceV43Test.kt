package com.huntersystem.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** La apariencia nunca depende del nivel, del registro ni del catálogo. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class HunterAppearanceV43Test {
 @Test fun onlyTwoModesAndExactlyFiveBorderColors() {
  assertEquals("light",HunterAppearanceV43.mode("light"))
  assertEquals("dark",HunterAppearanceV43.mode("dark"))
  assertEquals("dark",HunterAppearanceV43.mode("aurora"))
  assertEquals(5,HunterAppearanceV43.outlines.size)
  assertEquals(5,HunterAppearanceV43.outlines.map{it.id}.distinct().size)
 }

 @Test fun borderChoiceDoesNotChangeSurfaceThemeOrText() {
  for(mode in listOf("light","dark")){
   val original=HunterAppearanceV43.tokens(mode,"eclipse")
   for(swatch in HunterAppearanceV43.outlines){
    val changed=HunterAppearanceV43.tokens(mode,swatch.id)
    for(key in listOf("--mode","--bg","--panel","--surface","--input",
      "--line","--text","--muted","--selected33")){
     assertEquals("Color $key in $mode",original.getString(key),changed.getString(key))
    }
    val expected=HunterAppearanceV43.borderHex(mode,swatch.id)
    assertEquals(expected,changed.getString("--outlineAccent"))
    assertEquals(expected,changed.getString("--accent"))
    assertEquals(HunterAppearanceV43.accentShade(expected,mode=="light"),
      changed.getString("--secondary33"))
   }
  }
 }

 @Test fun allFiveChoicesUpdateHighlightColorsWithoutChangingSurfaces() {
  for(mode in listOf("light","dark")){
   val expected=HunterAppearanceV43.outlines.map{
    HunterAppearanceV43.tokens(mode,it.id).getString("--accent")
   }
   assertEquals(5,expected.distinct().size)
   assertEquals(expected,HunterAppearanceV43.outlines.map{
    HunterAppearanceV43.tokens(mode,it.id).getString("--outlineAccent")
   })
  }
 }

 @Test fun differentThemeMeansDifferentSurfacesAndReadableText() {
  val light=HunterAppearanceV43.tokens("light","nebula")
  val dark=HunterAppearanceV43.tokens("dark","nebula")
  assertNotEquals(light.getString("--bg"),dark.getString("--bg"))
  assertNotEquals(light.getString("--text"),dark.getString("--text"))
  assertNotEquals(light.getString("--outlineAccent"),dark.getString("--outlineAccent"))
  assertEquals("nebula",HunterAppearanceV43.outline("aurora"))
  assertEquals("eclipse",HunterAppearanceV43.outline("?"))
 }
}
