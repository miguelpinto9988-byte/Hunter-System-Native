package com.huntersystem.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/**
 * Una misma evolución E/D/C/B/A/S en selector, vista previa y Panel Monarca.
 * Las nuevas ilustraciones tienen transparencia y se muestran completas (Fit).
 * Nunca dejar el hero vacío si una imagen no está instalada en los assets.
 */
internal fun hunterEvolutionAssetPath(collection:String,gender:String,rank:String):String {
 val who=when(collection){
  "reader","regressor","hunter-male","hunter-female"->collection
  "alternate"->when(chosenHunterArtwork("alternate",gender)){
   "ui34/reader-d.webp"->"reader"
   "ui34/regressor-e.webp"->"regressor"
   else->if(gender=="female")"hunter-female" else "hunter-male"
  }
  else->if(gender=="female")"hunter-female" else "hunter-male"
 }
 val safe=rank.lowercase().takeIf{it in setOf("e","d","c","b","a","s")} ?: "e"
 return "hunter-evolutions-v43/$who-$safe.webp"
}

@Composable
internal fun HunterEvolutionImage(
 collection:String,gender:String,rank:String,fallback:String,
 modifier:Modifier=Modifier,scale:ContentScale=ContentScale.Fit,
 description:String?=null,alignment:Alignment=Alignment.Center
){
 val context=LocalContext.current
 val requested=hunterEvolutionAssetPath(collection,gender,rank)
 val available=remember(context,requested){
  runCatching{context.assets.open(requested).use{true}}.getOrDefault(false)
 }
 AssetImage(if(available)requested else fallback,modifier,scale,description,alignment)
}
