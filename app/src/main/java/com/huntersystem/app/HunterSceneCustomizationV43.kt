package com.huntersystem.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Los fondos se usan EXCLUSIVAMENTE en la tarjeta del cazador y en su selector.
 * No se aplican como wallpaper de ninguna pantalla del sistema.
 */
internal data class HunterScene(val id:String,val title:String,val subtitle:String)
internal val hunterSceneOptions=listOf(
 HunterScene("auto","Original","Adaptado a tu rango"),
 HunterScene("biblioteca-lunar","Biblioteca lunar","Conocimiento y estrellas"),
 HunterScene("jardin-sagrado","Jardín sagrado","Cascadas y flores"),
 HunterScene("palacio-submarino","Palacio submarino","Océano cristalino"),
 HunterScene("templo-del-ocaso","Templo del ocaso","Ruinas doradas"),
 HunterScene("ciudadela-boreal","Ciudadela boreal","Nieve y auroras"),
 HunterScene("reino-de-las-nubes","Reino de las nubes","Castillo flotante"),
 HunterScene("trono-volcanico","Trono volcánico","Fuego y obsidiana"),
 HunterScene("santuario-de-bambu","Santuario de bambú","Bosque bajo la luna")
)
internal fun monarchScenePath(scene:String,rank:String="E"):String =
 if(hunterSceneOptions.any{it.id==scene && it.id!="auto"})
  "hunter-scenes-hd/$scene.webp"
 else "ui33/scene-${rank.lowercase().takeIf{it in setOf("e","d","c","b","a","s")} ?: "e"}-0.webp"

internal fun monarchSceneFrameColor(frame:String,base:Color):Color=when(frame){
 "eclipse"->Color(0xFF9661F7)
 "crystal"->Color(0xFF8BE5EF)
 else->base
}

/** Capas idénticas para la vista previa y la tarjeta real de Sistema Monarca. */
@Composable
internal fun MonarchCardArtwork(
 scene:String,collection:String,gender:String,rank:String,
 lighting:String,atmosphere:String,modifier:Modifier=Modifier
){
 val art=resolvedHunterArtwork(collection,gender,rank)
 Box(modifier.background(Color(0xFF060E1E))){
  AssetImage(monarchScenePath(scene,rank),
   Modifier.matchParentSize(),ContentScale.Crop,description=null,
   alignment=Alignment.CenterEnd)
  val tint=when(lighting){
   "moon"->Color(0x282B66D9)
   "dawn"->Color(0x36E6B26B)
   "dusk"->Color(0x3A9D4978)
   else->Color.Transparent
  }
  if(lighting!="original")Box(Modifier.matchParentSize().background(tint))
  AssetImage(art,Modifier.align(Alignment.CenterEnd)
    .fillMaxHeight().fillMaxWidth(.83f),ContentScale.Crop,
    description=null,alignment=Alignment.TopEnd)
  val overlay=when(atmosphere){
   "mystic"->Color(0x391C2A67)
   "dramatic"->Color(0x6206081A)
   else->Color(0x18050E1D)
  }
  Box(Modifier.matchParentSize().background(overlay))
  // Protege el bloque de nivel sin cubrir el escenario y personaje de la derecha.
  Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(
   Color(0xF2050E20),Color(0xC8071429),Color(0x34061120),Color.Transparent
  ))))
  Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
   Color(0x12050B19),Color.Transparent,Color(0xBF050B18)
  ))))
 }
}

@Composable
internal fun HunterSceneCustomizer(
 scene:String,collection:String,gender:String,
 lighting:String,atmosphere:String,frame:String,
 name:String,level:Int,xp:Int,
 onScene:(String)->Unit,onLighting:(String)->Unit,
 onAtmosphere:(String)->Unit,onFrame:(String)->Unit
){
 val c=LocalHunter.current
 Spacer(Modifier.height(9.dp))
 SectionTitle("Personalización del entorno",
  "Elige el ambiente de la tarjeta Monarca.",Sigil.Layers)
 LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)){
  items(hunterSceneOptions,key={it.id}){item->
   val active=scene==item.id
   Column(Modifier.width(151.dp).clip(RoundedCornerShape(16.dp))
     .background(if(active)c.selected else c.input)
     .border(if(active)2.dp else 1.dp,
       if(active)c.accent else c.line,RoundedCornerShape(16.dp))
     .clickable(role=Role.RadioButton){onScene(item.id)}
     .semantics{selected=active;contentDescription="Ambiente ${item.title}"}
     .padding(7.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
    Box(Modifier.fillMaxWidth().height(128.dp)
     .clip(RoundedCornerShape(11.dp)).background(c.panel)){
     AssetImage(monarchScenePath(item.id),Modifier.matchParentSize(),
      ContentScale.Crop,description=null,alignment=Alignment.CenterEnd)
     if(active)Box(Modifier.align(Alignment.TopEnd).padding(5.dp)
       .background(c.accent,RoundedCornerShape(11.dp)).padding(5.dp)){
      HText("✓",size=13,bold=true,color=c.bg)
     }
    }
    HText(item.title,size=13,bold=active,maxLines=2)
    HText(item.subtitle,size=11,color=c.muted,maxLines=2)
   }
  }
 }
 Spacer(Modifier.height(8.dp))
 Heading("Ajustes del entorno",size=23)
 HSelect("Iluminación",lighting,listOf(
  "original" to "Original","moon" to "Luna llena",
  "dawn" to "Amanecer","dusk" to "Atardecer"
 ),onChange=onLighting)
 HSelect("Atmósfera",atmosphere,listOf(
  "serene" to "Serena","mystic" to "Mística","dramatic" to "Dramática"
 ),onChange=onAtmosphere)
 HSelect("Marco",frame,listOf(
  "monarch" to "Monarca","eclipse" to "Eclipse","crystal" to "Cristal"
 ),onChange=onFrame)
 Spacer(Modifier.height(8.dp))
 SectionTitle("Vista previa","Así quedará tu cazador en Sistema Monarca.",Sigil.Layers)
 HunterMonarchCardPreview(scene,collection,gender,lighting,atmosphere,frame,
   name,level,xp)
 HText("Los fondos generales de las demás pantallas permanecen limpios.",
   size=13,color=c.muted)
}

@Composable
internal fun HunterMonarchCardPreview(
 scene:String,collection:String,gender:String,
 lighting:String,atmosphere:String,frame:String,
 name:String,level:Int,xp:Int
){
 val c=LocalHunter.current
 val rank=rankKey(level.coerceAtLeast(1))
 val shape=RoundedCornerShape(23.dp)
 Box(Modifier.fillMaxWidth().height(405.dp).clip(shape)
  .background(c.panel)
  .border(2.dp,monarchSceneFrameColor(frame,c.accent),shape)){
  MonarchCardArtwork(scene,collection,gender,rank,lighting,atmosphere,
   Modifier.matchParentSize())
  Column(Modifier.fillMaxSize().padding(15.dp),
   verticalArrangement=Arrangement.spacedBy(10.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
    Eyebrow("◆ SISTEMA MONARCA")
    HText("RANGO $rank",size=13,color=c.accent,bold=true)
   }
   Column(Modifier.weight(1f).fillMaxWidth(.60f),
    verticalArrangement=Arrangement.spacedBy(11.dp)){
    HText("TU EVOLUCIÓN",size=13,color=c.accent,bold=true)
    Heading("NIVEL\n"+level.coerceAtLeast(1).toString().padStart(2,'0'),size=36)
    HText(name.ifBlank{"Cazador"}.take(30),size=17,bold=true)
    HText("Cada sesión cuenta.",size=13,color=c.muted)
    HText("EVOLUCIONAR ↗",size=13,bold=true,color=c.accent)
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
    HText("EXPERIENCIA",size=13,color=c.muted,bold=true)
    HText("${xp.coerceAtLeast(0)} XP",size=14,bold=true,color=c.accent)
   }
   HProgress((xp.coerceAtLeast(0)%300)/300f)
   HText("${xp.coerceAtLeast(0)%300} / 300 XP hacia el siguiente nivel",
    size=12,color=c.muted)
  }
 }
}
