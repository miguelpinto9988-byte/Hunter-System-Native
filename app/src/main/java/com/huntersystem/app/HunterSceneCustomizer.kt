package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal data class HunterSceneOption(val id:String,val title:String,val subtitle:String)
internal val hunterScenes = listOf(
 HunterSceneOption("biblioteca-lunar","Biblioteca lunar","Sabiduría y misterio"),
 HunterSceneOption("jardin-sagrado","Jardín sagrado","Flores y cascadas"),
 HunterSceneOption("palacio-submarino","Palacio submarino","Reino oceánico"),
 HunterSceneOption("reino-de-las-nubes","Reino de las nubes","Palacios flotantes"),
 HunterSceneOption("templo-del-ocaso","Templo del ocaso","Sol dorado"),
 HunterSceneOption("ciudadela-boreal","Ciudadela boreal","Aurora y nieve"),
 HunterSceneOption("trono-volcanico","Trono volcánico","Fortaleza de fuego"),
 HunterSceneOption("santuario-de-bambu","Santuario de bambú","Noche serena")
)
internal fun hunterSceneAsset(scene:String,rank:String="e"):String {
 val grade=rank.lowercase().takeIf{it in listOf("e","d","c","b","a","s")} ?: "e"
 return if(hunterScenes.any{it.id==scene}) "hunter-scenes-hd/$scene.webp"
  else "ui33/scene-$grade-0.webp"
}
internal fun monarchSceneFrameColor(frame:String,accent:Color):Color =
 when(frame){"royal"->Color(0xFFD5B869);"minimal"->Color(0xFF61758B);else->accent}

@Composable
internal fun MonarchCardArtwork(
 scene:String,collection:String,gender:String,rank:String,
 lighting:String,atmosphere:String,modifier:Modifier=Modifier
){
 Box(modifier.background(Color(0xFF061124))){
  AssetImage(hunterSceneAsset(scene,rank),Modifier.matchParentSize(),
   ContentScale.Crop,description=null)
  if(lighting=="moon")Box(Modifier.matchParentSize().background(Color(0x202A4DAD)))
  if(lighting=="sunset")Box(Modifier.matchParentSize().background(Color(0x20D69A55)))
  if(lighting=="mystic")Box(Modifier.matchParentSize().background(Color(0x206B33AD)))
  if(atmosphere=="mist")Box(Modifier.matchParentSize().background(Color(0x258DB3D0)))
  if(atmosphere=="epic")Box(Modifier.matchParentSize().background(Color(0x155A4DFF)))
  AssetImage(resolvedHunterArtwork(collection,gender,rank),
   Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.85f),
   ContentScale.Crop,description="Cazador de rango $rank",
   alignment=Alignment.TopEnd)
  Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
   listOf(Color(0xE9061124),Color(0xBD061124),Color(0x33061124),Color.Transparent))))
  Box(Modifier.matchParentSize().background(Brush.verticalGradient(
   listOf(Color.Transparent,Color.Transparent,Color(0xB5050A18)))))
 }
}
@Composable
internal fun HunterSceneCustomizer(
 scene:String,collection:String,gender:String,lighting:String,
 atmosphere:String,frame:String,name:String,level:Int,xp:Int,
 onScene:(String)->Unit,onLighting:(String)->Unit,
 onAtmosphere:(String)->Unit,onFrame:(String)->Unit
){
 val c=LocalHunter.current
 SectionTitle("Personalización del entorno",
  "Elige el fondo de tu tarjeta Monarca",Sigil.Layers)
 val items=listOf(HunterSceneOption("auto","Original Monarca","Según rango"))+hunterScenes
 Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
  horizontalArrangement=Arrangement.spacedBy(9.dp)){
  for(item in items){
   val active=scene==item.id
   Column(Modifier.width(137.dp).clip(RoundedCornerShape(16.dp))
    .background(if(active)c.selected else c.input)
    .border(if(active)2.dp else 1.dp,if(active)c.accent else c.line,RoundedCornerShape(16.dp))
    .clickable(role=Role.RadioButton){onScene(item.id)}
    .semantics{selected=active}.padding(7.dp),
    verticalArrangement=Arrangement.spacedBy(6.dp)){
    Box(Modifier.fillMaxWidth().height(122.dp).clip(RoundedCornerShape(10.dp))
     .background(c.panel)){
     AssetImage(hunterSceneAsset(item.id),Modifier.fillMaxSize(),
      ContentScale.Crop,description=item.title)
     if(active)Box(Modifier.align(Alignment.TopEnd).padding(4.dp)
      .clip(RoundedCornerShape(5.dp)).background(c.accent).padding(4.dp)){
      HText("✓",size=14,color=c.bg,bold=true)
     }
    }
    HText(item.title,size=14,bold=active,maxLines=2)
    HText(item.subtitle,size=11,color=c.muted,maxLines=1)
   }
  }
 }
 Spacer(Modifier.height(12.dp))
 SectionTitle("Ajustes del entorno","Elige la atmósfera",Sigil.Spark)
 HSelect("Iluminación",lighting,listOf("original" to "Natural",
  "moon" to "Luna llena","sunset" to "Atardecer","mystic" to "Mística"),onChange=onLighting)
 HSelect("Ambiente",atmosphere,listOf("serene" to "Sereno",
  "mist" to "Niebla","epic" to "Épico"),onChange=onAtmosphere)
 HSelect("Marco",frame,listOf("monarch" to "Monarca",
  "minimal" to "Minimalista","royal" to "Real"),onChange=onFrame)
 Spacer(Modifier.height(12.dp))
 SectionTitle("Vista previa","Así se verá tu tarjeta en el Panel",Sigil.Profile)
 val rank=rankKey(level.coerceAtLeast(1))
 val shape=RoundedCornerShape(if(frame=="minimal")14.dp else 24.dp)
 Box(Modifier.fillMaxWidth().height(360.dp).clip(shape).background(c.bg)
  .border(if(frame=="royal")2.dp else 1.dp,
   monarchSceneFrameColor(frame,c.accent),shape)){
  MonarchCardArtwork(scene,collection,gender,rank,lighting,atmosphere,
   Modifier.matchParentSize())
  Column(Modifier.fillMaxSize().padding(15.dp),
   verticalArrangement=Arrangement.spacedBy(8.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
    HText("◆ SISTEMA MONARCA",size=12,color=c.accent,bold=true)
    HText("RANGO $rank",size=12,color=c.accent,bold=true)
   }
   Spacer(Modifier.height(7.dp))
   HText("TU EVOLUCIÓN",size=12,color=c.accent,bold=true)
   Heading("NIVEL\n"+level.coerceAtLeast(1).toString().padStart(2,'0'),size=36)
   HText(name.ifBlank{"Cazador"},size=17,bold=true,maxLines=1)
   HText("Cada sesión cuenta.",size=13,color=c.muted)
   HText("EVOLUCIONAR ↗",size=13,color=c.accent,bold=true)
   Spacer(Modifier.weight(1f))
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
    HText("EXPERIENCIA",size=12,color=c.muted)
    HText("$xp XP",size=13,color=c.accent,bold=true)
   }
   HProgress((xp%300).coerceAtLeast(0)/300f)
   HText("${(xp%300).coerceAtLeast(0)} / 300 XP hacia el siguiente nivel",
    size=11,color=c.muted)
  }
 }
 HText("Solo la tarjeta Monarca utiliza estos escenarios; las demás pantallas mantienen el fondo limpio.",
  size=13,color=c.muted)
}
