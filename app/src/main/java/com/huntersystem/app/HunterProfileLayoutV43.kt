package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
 * El perfil conserva una sola pantalla independiente, con tres etapas
 * y trece secciones numeradas. No crea una cuarta ruta ni un menú lateral.
 */
@Composable
internal fun HunterProfileStageTabs(stage:Int,onSelect:(Int)->Unit){
 val colors=LocalHunter.current
 val tabs=listOf("MI CAZADOR","MI PLAN","SISTEMA")
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
  tabs.forEachIndexed{index,title->
   val active=stage==index
   val shape=RoundedCornerShape(14.dp)
   Column(Modifier.weight(1f).clip(shape)
    .background(if(active)colors.selected else colors.panel)
    .monarchPressable(shape,active=active,role=Role.Tab){onSelect(index)}
    .semantics{selected=active;contentDescription="Etapa "+(index+1)+": "+title}
    .padding(vertical=13.dp,horizontal=4.dp),
    horizontalAlignment=Alignment.CenterHorizontally,
    verticalArrangement=Arrangement.spacedBy(3.dp)){
    HText("0"+(index+1),size=18,bold=true,color=if(active)colors.accent else colors.muted)
    HText(title,size=11,bold=active,color=if(active)colors.text else colors.muted,maxLines=1)
   }
  }
 }
}

@Composable
internal fun HunterProfileBanner(isNew:Boolean,stage:Int){
 val colors=LocalHunter.current
 val shape=RoundedCornerShape(24.dp)
 Box(Modifier.fillMaxWidth().clip(shape)
  .background(Brush.horizontalGradient(listOf(colors.panel,colors.surface,colors.bg)))
  .monarchOutline(shape)){
  Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=19.dp),
   verticalArrangement=Arrangement.spacedBy(7.dp)){
   Row(verticalAlignment=Alignment.CenterVertically,
    horizontalArrangement=Arrangement.spacedBy(9.dp)){
    HText("◆",size=24,color=colors.accent,bold=true)
    HText("HUNTER SYSTEM",size=27,bold=true,maxLines=1)
   }
   Eyebrow("SISTEMA MONARCA  ·  PERFIL DEL CAZADOR")
   Heading(if(isNew)"Crea tu perfil" else "Tu perfil de cazador",size=29)
   HText(when(stage){
    0->"Tu historia comienza aquí. Elige tu identidad y el mundo que la acompañará."
    1->"Configura tus objetivos, nivel, modalidad y calendario."
    else->"Elige tu compañero y revisa las opciones finales antes de guardar."
   },size=15,color=colors.muted)
  }
  Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter)
   .background(Brush.horizontalGradient(listOf(Color.Transparent,colors.accent,colors.secondary,Color.Transparent))))
 }
}

@Composable
internal fun HunterProfileNumberedHeading(number:Int,title:String,subtitle:String){
 val colors=LocalHunter.current
 Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
   horizontalArrangement=Arrangement.spacedBy(8.dp)){
   HText(number.toString().padStart(2,'0'),size=25,bold=true,color=colors.accent)
   HText(title.uppercase(),size=17,bold=true,maxLines=2)
   Box(Modifier.weight(1f).height(2.dp)
    .background(Brush.horizontalGradient(listOf(colors.accent,colors.accent.copy(alpha=.20f),Color.Transparent))))
  }
  HText(subtitle,size=13,color=colors.muted)
 }
}

@Composable
internal fun HunterProfileSection(
 number:Int,title:String,subtitle:String,
 content:@Composable ColumnScope.()->Unit
){
 Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)){
  HunterProfileNumberedHeading(number,title,subtitle)
  HPanel(padding=17){content()}
 }
}

/** Misma ilustración y escenario elegidos que mostrará el Panel Monarca. */
@Composable
internal fun HunterProfileCardPreview(
 collection:String,gender:String,scene:String,name:String,level:Int,xp:Int
){
 val colors=LocalHunter.current
 val levelSafe=level.coerceAtLeast(1)
 val xpSafe=xp.coerceAtLeast(0)
 val rank=rankKey(levelSafe)
 val shape=RoundedCornerShape(22.dp)
 BoxWithConstraints(Modifier.fillMaxWidth()){
  val compact=maxWidth<380.dp
  Box(Modifier.fillMaxWidth().height(if(compact)450.dp else 494.dp)
   .clip(shape).background(colors.bg)
   .monarchOutline(shape)
   .semantics{contentDescription="Vista previa de Sistema Monarca, cazador "+name}){
   AssetImage(chosenSceneArtwork(scene),Modifier.matchParentSize(),ContentScale.Crop,
    description=null,alignment=Alignment.Center)
   HunterEvolutionImage(collection,gender,rank,profileMonarchHunterArtwork(collection,gender,rank),
    Modifier.align(Alignment.BottomEnd).fillMaxWidth(.89f).fillMaxHeight(.96f),
    ContentScale.Fit,description=null,alignment=Alignment.BottomEnd)
   Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(
    colors.bg.copy(alpha=.99f),colors.bg.copy(alpha=.89f),
     colors.bg.copy(alpha=.20f),Color.Transparent
   ))))
   Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
    colors.bg.copy(alpha=.24f),Color.Transparent,colors.bg.copy(alpha=.84f)
   ))))
   Column(Modifier.fillMaxSize().padding(if(compact)14.dp else 19.dp),
    verticalArrangement=Arrangement.spacedBy(8.dp)){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
     HText("◆ SISTEMA MONARCA",size=if(compact)11 else 13,bold=true,color=colors.accent)
     HText("RANGO "+rank,size=12,bold=true,color=colors.accent)
    }
    Column(Modifier.weight(1f).fillMaxWidth(.62f),
     verticalArrangement=Arrangement.spacedBy(9.dp)){
     Spacer(Modifier.height(7.dp))
     HText("TU EVOLUCIÓN",size=12,bold=true,color=colors.accent)
     Heading("NIVEL\n"+levelSafe.toString().padStart(2,'0'),size=if(compact)34 else 43)
     HText(name.ifBlank{"Cazador"}.take(34),size=17,bold=true,maxLines=2)
     HText("Cada sesión cuenta.",size=13,color=colors.text.copy(alpha=.8f))
         }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
     HText("EXPERIENCIA",size=12,bold=true,color=colors.text)
     HText(xpSafe.toString()+" XP",size=14,bold=true,color=colors.accent)
    }
    HProgress((xpSafe%300)/300f)
    HText((xpSafe%300).toString()+" / 300 XP hacia el siguiente nivel",size=12,color=colors.text)
   }
  }
 }
}
