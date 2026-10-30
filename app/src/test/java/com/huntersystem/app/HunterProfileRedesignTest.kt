package com.huntersystem.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Cada opción gráfica del perfil debe corresponder a un recurso real de la APK. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class HunterProfileRedesignTest {
 @Test fun eightHdScenesArePackagedAndSelectable(){
  val context=ApplicationProvider.getApplicationContext<Context>()
  assertEquals(8,hunterProfileSceneChoices.size)
  for((key,label)in hunterProfileSceneChoices){
   assertTrue(label.isNotBlank())
   val resource=chosenSceneArtwork(key)
   assertEquals("hunter-scenes-hd/"+key+".webp",resource)
   context.assets.open(resource).use{assertTrue(it.read()!=-1)}
  }
  assertEquals("ui33/scene-e-0.webp",chosenSceneArtwork("sanctuary"))
 }
 @Test fun fourHuntersAndAllRanksUseAvailableArtwork(){
  val context=ApplicationProvider.getApplicationContext<Context>()
  for(key in listOf("reader","regressor","hunter-male","hunter-female")){
   val picture=chosenHunterArtwork(key,"unspecified")
   context.assets.open(picture).use{assertTrue("No existe "+picture,it.read()!=-1)}
   for(rank in listOf("E","D","C","B","A","S")){
    val hero=profileMonarchHunterArtwork(key,"unspecified",rank)
    context.assets.open(hero).use{assertTrue("No existe "+hero,it.read()!=-1)}
   }
  }
 }
 @Test fun previousVisualValuesRemainCompatible(){
  assertEquals("ui34/hunter-female-e.webp",chosenHunterArtwork("hunter","female"))
  assertEquals("ui34/hunter-male-e.webp",chosenHunterArtwork("hunter","unspecified"))
  assertEquals("ui34/reader-e.webp",chosenHunterArtwork("reader","unspecified"))
 }
}
