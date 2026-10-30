package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/** Modo de superficie y color de contorno: dos preferencias independientes. */
internal object HunterAppearanceV43 {
 data class Outline(val id:String,val name:String,val light:String,val dark:String)
 val outlines=listOf(
  Outline("eclipse","Eclipse","#315CEB","#6487FF"),
  Outline("nebula","Rosa Nébula","#BF287C","#FF77C4"),
  Outline("astral","Violeta Astral","#7645C7","#B289FF"),
  Outline("emerald","Esmeralda","#087E68","#58D9AC"),
  Outline("amber","Ámbar Solar","#A46A0B","#FFD074")
 )
 fun mode(raw:String?):String=if(raw=="light")"light" else "dark"
 fun outline(raw:String?):String=when(raw?.lowercase()){
  "eclipse","cyan","blue","ice","hielo","constellation"->"eclipse"
  "nebula","aurora","pink","coral","lavanda"->"nebula"
  "astral","umbral","violet"->"astral"
  "emerald","runa","teal","mint","abismo"->"emerald"
  "amber","amanecer","gold"->"amber"
  else->"eclipse"
 }
 fun originalOutline(theme:String?):String=outline(theme)
 fun borderHex(mode:String,outline:String):String{
  val swatch=outlines.firstOrNull{it.id==outline}?:outlines.first()
  return if(mode=="light")swatch.light else swatch.dark
 }
 // Segunda tonalidad derivada del mismo color, sin acentos ajenos al perfil.
 fun accentShade(hex:String,light:Boolean):String{
  val value=hex.removePrefix("#").toInt(16)
  val target=if(light)0x243454 else 0xDCE7FF
  val fraction=if(light).15 else .12
  fun channel(shift:Int):Int{
   val from=(value shr shift) and 255
   val to=(target shr shift) and 255
   return (from+(to-from)*fraction).toInt().coerceIn(0,255)
  }
  return "#%02X%02X%02X".format(channel(16),channel(8),channel(0))
 }
 fun tokens(modeValue:String?,outlineValue:String?):JSONObject{
  val bright=mode(modeValue)=="light"
  val currentMode=if(bright)"light" else "dark"
  val selectedAccent=borderHex(currentMode,outline(outlineValue))
  return obj(
   "--mode" to currentMode,
   "--bg" to if(bright)"#F5F7FC" else "#090D16",
   "--panel" to if(bright)"#FFFFFF" else "#141B29",
   "--surface" to if(bright)"#FBFCFF" else "#182334",
   "--input" to if(bright)"#EFF2F8" else "#1D293B",
   "--line" to if(bright)"#B4C0D2" else "#40506A",
   "--text" to if(bright)"#18243B" else "#EBF1FE",
   "--muted" to if(bright)"#59677D" else "#ADBDD3",
   "--accent" to selectedAccent,
   "--secondary33" to accentShade(selectedAccent,bright),
   "--selected33" to if(bright)"#E6EAF3" else "#273348",
   "--outlineAccent" to selectedAccent
  )
 }
}

@Composable
internal fun HunterModePicker(selected:String,onSelect:(String)->Unit){
 val c=LocalHunter.current
 HText("TEMA",size=15,bold=true,color=c.text)
 HText("Elige la apariencia general de la aplicación.",size=13,color=c.muted)
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){
  for((key,label,description,symbol) in listOf(
   listOf("light","Claro","Fondo luminoso","☀"),
   listOf("dark","Oscuro","Fondo nocturno","☾")
  )){
   val active=HunterAppearanceV43.mode(selected)==key
   val shape=RoundedCornerShape(16.dp)
   Column(Modifier.weight(1f).clip(shape).background(c.input)
    .monarchPressable(shape,active=active,role=Role.RadioButton){onSelect(key)}
    .semantics{this.selected=active;contentDescription="Tema "+label}
    .padding(11.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
    HText("$symbol  $label",size=17,bold=true,
     color=if(active)c.outlineAccent else c.text)
    HText(description,size=12,color=c.muted,maxLines=2)
   }
  }
 }
}

@Composable
internal fun HunterOutlinePicker(selected:String,mode:String,onSelect:(String)->Unit){
 val c=LocalHunter.current
 HText("COLOR DE CONTORNOS",size=15,bold=true)
 HText("El color elegido se aplica a marcos, títulos destacados, iconos, botones y barras.",
  size=13,color=c.muted)
 for(group in HunterAppearanceV43.outlines.chunked(3)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
   for(swatch in group){
    val active=HunterAppearanceV43.outline(selected)==swatch.id
    val shape=RoundedCornerShape(13.dp)
    Column(Modifier.weight(1f).clip(shape).background(c.input)
     .monarchPressable(shape,active=active,role=Role.RadioButton){onSelect(swatch.id)}
     .semantics{this.selected=active;contentDescription="Contorno "+swatch.name}
     .padding(horizontal=4.dp,vertical=11.dp),
     horizontalAlignment=Alignment.CenterHorizontally,
     verticalArrangement=Arrangement.spacedBy(7.dp)){
      Box(Modifier.size(25.dp).clip(CircleShape)
       .background(parseColor(HunterAppearanceV43.borderHex(mode,swatch.id))),
       contentAlignment=Alignment.Center){
        if(active)HText("✓",size=15,bold=true,color=if(mode=="light")Color.White else Color.Black)
       }
      HText(swatch.name,size=11,bold=active,maxLines=2)
    }
   }
   repeat(3-group.size){Spacer(Modifier.weight(1f))}
  }
 }
}

@Composable
internal fun HunterAppearancePreview(mode:String,outline:String){
 val palette=HunterColors(HunterAppearanceV43.tokens(mode,outline))
 CompositionLocalProvider(LocalHunter provides palette){
  val c=LocalHunter.current
  HText("VISTA PREVIA",size=15,bold=true)
  HText("Vista previa de marcos y textos destacados. Se aplicará al guardar.",size=13,color=c.muted)
  HPanel(padding=14,decor=false){
   Row(verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){
     HText("◆ SISTEMA MONARCA",size=15,bold=true,color=c.accent)
     HText(if(c.isLight)"Modo claro" else "Modo oscuro",size=13,color=c.muted)
    }
    HText("RANGO E",size=13,color=c.accent,bold=true)
   }
   HProgress(.56f)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    HPanel(Modifier.weight(1f),padding=10,decor=false){
     HText("Misiones",size=15,bold=true)
     HText("3 activas",size=12,color=c.muted)
    }
    HPanel(Modifier.weight(1f),padding=10,decor=false){
     HText("Objetivos",size=15,bold=true)
     HText("2 completados",size=12,color=c.muted)
    }
   }
   val selectedShape=RoundedCornerShape(13.dp)
   Box(Modifier.clip(selectedShape).background(c.selected)
    .monarchOutline(selectedShape,active=true).padding(10.dp)){
    HText("Ventana seleccionada",size=13,bold=true,color=c.outlineAccent)
   }
  }
 }
}
