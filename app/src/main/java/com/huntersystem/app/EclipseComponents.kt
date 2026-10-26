package com.huntersystem.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import org.json.JSONObject
import kotlin.math.*

@Composable fun SectionTitle(title:String,subtitle:String?=null,icon:Sigil=Sigil.Spark){
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
  Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(LocalHunter.current.selected),contentAlignment=Alignment.Center){HunterIcon(icon,Modifier.size(22.dp))}
  Column(Modifier.weight(1f)){Heading(title,size=26);if(subtitle!=null)HText(subtitle,size=14,color=LocalHunter.current.muted)}
 }
}
@Composable fun StatusPill(label:String,icon:Sigil=Sigil.Star){
 Row(Modifier.clip(CircleShape).background(LocalHunter.current.accent.copy(alpha=.10f)).border(1.dp,LocalHunter.current.accent.copy(alpha=.25f),CircleShape)
  .padding(horizontal=10.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
  HunterIcon(icon,Modifier.size(14.dp));HText(label,size=12,color=LocalHunter.current.accent,bold=true)
 }
}
@Composable fun ProgressRing(fraction:Float,label:String,modifier:Modifier=Modifier){
 val c=LocalHunter.current;val progress by animateFloatAsState(fraction.coerceIn(0f,1f),tween(if(LocalMotionEnabled.current)450 else 0),label="progress-ring")
 Box(modifier.size(84.dp).semantics(mergeDescendants=true){contentDescription="$label: ${(fraction*100).roundToInt()} por ciento"},contentAlignment=Alignment.Center){
  Canvas(Modifier.fillMaxSize().padding(4.dp)){
   val stroke=Stroke(5.dp.toPx(),cap=StrokeCap.Round)
   drawArc(c.line.copy(alpha=.6f),-90f,360f,false,style=stroke)
   drawArc(Brush.sweepGradient(listOf(c.accent,c.secondary,c.accent)),-90f,progress*360f,false,style=stroke)
  }
  Column(horizontalAlignment=Alignment.CenterHorizontally){Heading("${(fraction*100).roundToInt()}%",size=25);HText(label,size=10,color=c.muted)}
 }
}
@Composable fun StatCard(value:String,label:String,icon:Sigil,modifier:Modifier=Modifier){
 HPanel(modifier,padding=15,decor=false){HunterIcon(icon,Modifier.size(20.dp));Heading(value,size=30);HText(label,size=12,color=LocalHunter.current.muted)}
}

@Composable fun CompanionStage(character:String,modifier:Modifier=Modifier,prefs:JSONObject=LocalVisualPreferences.current,description:String?=null){
 val c=LocalHunter.current;val phase=ambientPhase(prefs.n("speed",1.0).toFloat());val mode=visualMode(prefs)
 val glow=prefs.b("aura",true);val size=prefs.n("size",1.0).toFloat().coerceIn(.8f,1.2f)
 Box(modifier.clip(RoundedCornerShape(24.dp)),contentAlignment=Alignment.Center){
  if(glow)Canvas(Modifier.fillMaxSize()){
   val center=Offset(this.size.width*.5f,this.size.height*.5f);val radius=this.size.minDimension*.5f
   drawCircle(Brush.radialGradient(listOf(c.accent.copy(alpha=.18f),Color.Transparent),center,radius),radius,center)
   drawOval(c.secondary.copy(alpha=.18f),Offset(this.size.width*.16f,this.size.height*.84f),androidx.compose.ui.geometry.Size(this.size.width*.68f,this.size.height*.05f))
  }
  if(mode!="flat")ConstellationField(Modifier.fillMaxSize())
  if(mode=="relic")RelicScene(Modifier.align(Alignment.CenterEnd).fillMaxWidth(.58f).fillMaxHeight(.9f))
  AssetImage("ui33/companion-$character.webp",Modifier.fillMaxHeight(.80f).fillMaxWidth(if(mode=="relic").54f else .68f).align(if(mode=="relic")Alignment.CenterStart else Alignment.Center)
   .graphicsLayer{
    scaleX=size;scaleY=size
    if(mode=="depth"){translationY=sin(phase.value*PI.toFloat()*2)*5.dp.toPx();rotationY=sin(phase.value*PI.toFloat()*2)*5f;cameraDistance=16*density}
   },description=description)
 }
}

@Composable fun ThemeGallery(selected:String,onSelect:(String)->Unit){
 SectionTitle("Elige tu universo","Seis atmósferas para tu entrenamiento",Sigil.Layers)
 for(row in eclipsePalettes.chunked(2))Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
  for(theme in row){val c=eclipseColors(theme.id);val chosen=selected==theme.id
   Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(c.bg)
    .border(if(chosen)2.dp else 1.dp,if(chosen)c.accent else c.line,RoundedCornerShape(20.dp))
    .clickable(role=Role.RadioButton){onSelect(theme.id)}.semantics{this.selected=chosen;contentDescription="Tema ${theme.name}"}.padding(13.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
    CompositionLocalProvider(LocalHunter provides c){
     Box(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(c.selected,c.panel)))){
      ConstellationField(Modifier.fillMaxSize());HunterIcon(Sigil.Crown,Modifier.align(Alignment.Center).size(30.dp))
      if(chosen)HunterIcon(Sigil.Check,Modifier.align(Alignment.TopEnd).padding(5.dp).size(16.dp))
     }
     HText(theme.name,size=18,bold=true,color=c.text)
     Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){for(color in listOf(c.accent,c.secondary,c.panel))Box(Modifier.size(10.dp).background(color,CircleShape))}
    }
   }
  }
 }
 HText(eclipsePalette(selected).story,size=15,color=LocalHunter.current.muted)
}
@Composable fun CompanionPicker(selected:String,onSelect:(String)->Unit){
 SectionTitle("Tu acompañante","Selecciona quién camina contigo",Sigil.Shield)
 val characters=listOf(Triple("knight","igris","IGRIS"),Triple("warrior","iron","IRON"),Triple("bear","tank","TANK"),Triple("insect","beru","BERU"),Triple("scientist","dokja","DOKJA KIM"))
 Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)){
  for((id,key,name)in characters){val active=id==selected;val c=LocalHunter.current
   Column(Modifier.width(112.dp).clip(RoundedCornerShape(20.dp)).background(if(active)c.selected else c.input)
    .border(if(active)2.dp else 1.dp,if(active)c.accent else c.line,RoundedCornerShape(20.dp))
    .clickable(role=Role.RadioButton){onSelect(id)}.semantics{this.selected=active;contentDescription="Acompañante $name"}.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
    AssetImage("ui33/companion-$key.webp",Modifier.height(120.dp).fillMaxWidth())
    HText(name,size=13,bold=true)
    HunterIcon(if(active)Sigil.Check else Sigil.Plus,Modifier.size(16.dp),if(active)c.accent else c.muted)
   }
  }
 }
 CompanionStage(companionKey(obj("companionCharacter" to selected)),Modifier.fillMaxWidth().height(230.dp))
}
@Composable fun AppearanceControls(ui:NativeUi){
 val s=ui.store;val prefs=s.state.o("eclipseAppearance")
 fun change(key:String,value:Any){ui.attempt{s.state.put("eclipseAppearance",s.state.o("eclipseAppearance").copyJson().put(key,value));s.save()}}
 HPanel{
  SectionTitle("Profundidad y movimiento","Elige cómo se ve tu sistema",Sigil.Layers)
  Chips(listOf("flat" to "2D","depth" to "2.5D","relic" to "3D"),visualMode(prefs)){change("mode",it)}
  HText(when(visualMode(prefs)){"flat"->"Ilustraciones planas y composición limpia.";"relic"->"Reliquia geométrica 3D junto a tu personaje ilustrado. Abre el visor para girarla.";else->"Ilustraciones en capas con movimiento suave y profundidad."},size=15,color=LocalHunter.current.muted)
  if(visualMode(prefs)=="relic")HButton("Explorar reliquia 3D",Modifier.fillMaxWidth()){ui.open("relic")}
  HText("Tamaño del acompañante",size=15,bold=true)
  Chips(listOf("0.8" to "Compacto","1.0" to "Normal","1.2" to "Grande"),prefs.n("size",1.0).toString()){change("size",it.toDouble())}
  HText("Velocidad ambiental",size=15,bold=true)
  Chips(listOf("0.5" to "Lenta","1.0" to "Normal","1.5" to "Rápida"),prefs.n("speed",1.0).toString()){change("speed",it.toDouble())}
  HCheck("Aura del acompañante",prefs.b("aura",true)){change("aura",!prefs.b("aura",true))}
  HCheck("Acceso flotante al acompañante",prefs.b("floating",true)){change("floating",!prefs.b("floating",true))}
  HCheck("Reducir movimiento",prefs.b("reduceMotion")){change("reduceMotion",!prefs.b("reduceMotion"))}
  HText("Estos ajustes visuales se guardan al cambiarlos.",size=13,color=LocalHunter.current.muted)
 }
}
@Composable fun RelicViewer(){
 Eyebrow("SALA DE EVOLUCIÓN");Heading("El núcleo de tu poder")
 RelicScene(Modifier.fillMaxWidth().height(340.dp),interactive=true)
 HText("Arrastra la reliquia para girarla.",size=17)
 HText("Geometría 3D con perspectiva y luz. El movimiento ambiental respeta el ajuste Reducir movimiento.",size=15,color=LocalHunter.current.muted)
}
@Composable fun ExerciseStage(content:@Composable BoxScope.()->Unit){
 val c=LocalHunter.current
 Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Brush.verticalGradient(listOf(c.input,c.panel))).border(1.dp,c.accent.copy(alpha=.25f),RoundedCornerShape(26.dp))){
  ConstellationField(Modifier.matchParentSize());content()
 }
}
@Composable fun EmptyState(title:String,body:String,icon:Sigil=Sigil.Search){
 HPanel{HunterIcon(icon,Modifier.size(36.dp));Heading(title,size=27);HText(body,size=16,color=LocalHunter.current.muted)}
}
@Composable fun AccessWelcome(){
 HPanel(padding=22){
  Box(Modifier.fillMaxWidth().height((220 * androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)).dp).clip(RoundedCornerShape(18.dp))){
   AssetImage("eclipse/portal.webp",Modifier.fillMaxSize(),ContentScale.Crop)
   Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(LocalHunter.current.bg,Color.Transparent))))
   Column(Modifier.align(Alignment.CenterStart).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){HunterIcon(Sigil.Crown,Modifier.size(32.dp));Eyebrow("TU HISTORIA EMPIEZA AQUÍ");Heading("Despierta\ntu potencial.",size=32)}
  }
  HText("Crea tu perfil para preparar tus misiones, elegir tu acompañante y dar forma a tu entrenamiento.",size=16,color=LocalHunter.current.muted)
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){StatusPill("Perfil local",Sigil.Shield);StatusPill("A tu ritmo",Sigil.Clock)}
 }
}
@Composable fun EclipseLoading(error:String?,retry:()->Unit){
 val c=eclipseColors("eclipse")
 CompositionLocalProvider(LocalHunter provides c){
  Box(Modifier.fillMaxSize().background(c.bg),contentAlignment=Alignment.Center){
   ConstellationField(Modifier.fillMaxSize())
   Column(Modifier.widthIn(max=420.dp).padding(30.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(22.dp)){
    RelicScene(Modifier.size(170.dp));Heading("HUNTER SYSTEM",size=30);Eyebrow("ECLIPSE · ASCENSIÓN")
    HText(error?:"Preparando tu próximo capítulo…",size=16,color=c.muted)
    if(error==null)androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth(),color=c.accent,trackColor=c.panel)
    else HButton("Reintentar",primary=true,onClick=retry)
   }
  }
 }
}
