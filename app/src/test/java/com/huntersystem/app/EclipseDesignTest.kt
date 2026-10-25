package com.huntersystem.app

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class EclipseDesignTest {
 private fun luminance(color:Long):Double {
  fun channel(shift:Int):Double {val x=((color shr shift) and 255)/255.0;return if(x<=.04045)x/12.92 else ((x+.055)/1.055).pow(2.4)}
  return .2126*channel(16)+.7152*channel(8)+.0722*channel(0)
 }
 private fun contrast(a:Long,b:Long):Double {val x=luminance(a);val y=luminance(b);return (max(x,y)+.05)/(min(x,y)+.05)}
 @Test fun textAndButtonLabelsMeetNormalTextContrastInEveryPalette(){
  for(p in eclipsePalettes){
   assertTrue("${p.id} body",contrast(0xfff3f3fa,p.panel)>=4.5)
   assertTrue("${p.id} muted",contrast(0xffb5bed1,p.panel)>=4.5)
   assertTrue("${p.id} primary label",contrast(p.background,p.accent)>=4.5)
   assertTrue("${p.id} secondary label",contrast(p.background,p.secondary)>=4.5)
  }
 }
 @Test fun oldAndNewThemesRemainResolvable(){
  for(p in eclipsePalettes)assertEquals(p.id,resolveV39Theme(p.id))
  assertEquals("eclipse",resolveV39Theme("unknown"));assertEquals("ice",resolveV39Theme("hielo"));assertEquals("constellation",resolveV39Theme("purple"))
 }
 @Test fun relicRotationPreservesGeometryAndHasRealDepth(){
  val p=RelicPoint(.65f,0f,.65f);val q=rotateRelic(p,.8f,.3f)
  assertEquals(p.x*p.x+p.y*p.y+p.z*p.z,q.x*q.x+q.y*q.y+q.z*q.z,.00001f)
  assertNotEquals(p.x,q.x);assertNotEquals(p.z,q.z)
 }
}
