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

@Composable fun CompanionStage(character:String,modifier:Modifier=Modifier,description:String?=null){
 val c=LocalHunter.current;val tier=LocalVisualTier.current
 val phase=ambientPhase(if(tier==VisualTier.MAX)1f else .65f)
 val mode=when(tier){VisualTier.MAX->"relic";VisualTier.BALANCED->"depth";VisualTier.SAVER->"flat"}
 val glow=tier!=VisualTier.SAVER
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
    if(tier!=VisualTier.SAVER){
     translationY=sin(phase.value*PI.toFloat()*2)*(if(tier==VisualTier.MAX)7 else 3).dp.toPx()
     rotationY=sin(phase.value*PI.toFloat()*2)*(if(tier==VisualTier.MAX)5f else 2f)
     cameraDistance=16*density
    }
   },description=description)
 }
}

// Five simple color choices for new and existing hunter profiles.
// Older palette IDs stay supported by EclipseVisuals for saved profiles.
val profileColorThemes=listOf("eclipse","ice","aurora","ceniza","amanecer")
@Composable fun ThemeGallery(selected:String,onSelect:(String)->Unit){
 SectionTitle("Color del sistema","Elige uno de cinco colores",Sigil.Layers)
 val themes=profileColorThemes.map(::eclipsePalette)
 for(row in themes.chunked(2))Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
  for(theme in row){val c=eclipseColors(theme.id);val chosen=selected==theme.id
   Row(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(c.panel)
    .monarchPressable(RoundedCornerShape(16.dp),active=chosen,role=Role.RadioButton){onSelect(theme.id)}
    .semantics{this.selected=chosen;contentDescription="Tema ${theme.name}"}
    .padding(horizontal=12.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
    Box(Modifier.size(25.dp).clip(CircleShape).background(c.accent))
    HText(theme.name,Modifier.weight(1f),size=14,color=c.text,bold=chosen,maxLines=1)
    if(chosen)HunterIcon(Sigil.Check,Modifier.size(15.dp),c.accent)
   }
  }
  if(row.size==1)Spacer(Modifier.weight(1f))
 }
}
@Composable fun CompanionPicker(selected:String,onSelect:(String)->Unit){
 SectionTitle("Tu acompañante","Selecciona quién camina contigo",Sigil.Shield)
 val characters=listOf(Triple("knight","igris","IGRIS"),Triple("warrior","iron","IRON"),Triple("bear","tank","TANK"),Triple("insect","beru","BERU"),Triple("scientist","dokja","DOKJA KIM"))
 Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)){
  for((id,key,name)in characters){val active=id==selected;val c=LocalHunter.current
   Column(Modifier.width(112.dp).clip(RoundedCornerShape(20.dp)).background(if(active)c.selected else c.input)
    .monarchPressable(RoundedCornerShape(20.dp),active=active,role=Role.RadioButton){onSelect(id)}.semantics{this.selected=active;contentDescription="Acompañante $name"}.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
    AssetImage("ui33/companion-$key.webp",Modifier.height(120.dp).fillMaxWidth())
    HText(name,size=13,bold=true)
    HunterIcon(if(active)Sigil.Check else Sigil.Plus,Modifier.size(16.dp),if(active)c.accent else c.muted)
   }
  }
 }
 CompanionStage(companionKey(obj("companionCharacter" to selected)),Modifier.fillMaxWidth().height(230.dp))
}

// Local art selections used by both the picker and home.
/** Respeta selecciones V33 previas y las cuatro portadas del perfil V43.12. */
/**
 * Ilustraciones verticales originales de calidad alta.
 * Se eliminan los retratos V43 de baja resolución del selector.
 */
fun chosenHunterArtwork(collection:String,gender:String):String{
 val selected=if(collection=="alternate")
   listOf("hunter","reader","regressor")[(java.time.LocalDate.now().toEpochDay()%3).toInt()]
  else collection
 return when(selected){
  // El lector con abrigo y fragmentos es el arte Monarca solicitado.
  "reader"->"ui34/reader-d.webp"
  "regressor"->"ui34/regressor-e.webp"
  "hunter-male"->"ui34/hunter-male-e.webp"
  "hunter-female"->"ui34/hunter-female-e.webp"
  else->if(gender=="female")"ui34/hunter-female-e.webp" else "ui34/hunter-male-e.webp"
 }
}
internal val hunterProfileSceneChoices=listOf(
 "biblioteca-lunar" to "Biblioteca lunar",
 "palacio-submarino" to "Palacio submarino",
 "reino-de-las-nubes" to "Ciudad celestial",
 "ciudadela-boreal" to "Ciudadela nevada",
 "trono-volcanico" to "Trono volcánico",
 "santuario-de-bambu" to "Santuario lunar",
 "jardin-sagrado" to "Jardín sagrado",
 "templo-del-ocaso" to "Templo del ocaso"
)
fun chosenSceneArtwork(scene:String):String{
 if(hunterProfileSceneChoices.any{it.first==scene})
  return "hunter-scenes-hd/"+scene+".webp"
 // Compatibilidad con escenas guardadas en versiones anteriores.
 val index=when(scene){
  "sanctuary"->0
  "journey"->1
  else->(java.time.LocalDate.now().toEpochDay()%2).toInt()
 }
 return "ui33/scene-e-"+index+".webp"
}
@Composable fun HunterPicker(selected:String,gender:String,onSelect:(String)->Unit){
 val c=LocalHunter.current
 val choices=listOf(
  Triple("reader","Reader","EL OBSERVADOR"),
  Triple("regressor","Regressor","EL QUE RETROCEDE"),
  Triple("hunter-male","Cazador","EL QUE ASCIENDE"),
  Triple("hunter-female","Cazadora","LA QUE DESAFÍA")
 )
 HText("Desliza para descubrir los cuatro protagonistas.",size=13,color=c.muted)
 Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
  horizontalArrangement=Arrangement.spacedBy(12.dp)){
  for((key,label,subtitle)in choices){
   val active=selected==key||
    (selected=="hunter"&&key==if(gender=="female")"hunter-female" else "hunter-male")
   val shape=RoundedCornerShape(18.dp)
   Column(Modifier.width(204.dp).clip(shape)
    .background(if(active)c.selected else c.input)
    .monarchPressable(shape,active=active,role=Role.RadioButton){onSelect(key)}
    .semantics{this.selected=active;contentDescription="Cazador "+label}
    .padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally,
    verticalArrangement=Arrangement.spacedBy(8.dp)){
    // Fit respeta toda la ilustración incluso en teléfonos estrechos.
    Box(Modifier.fillMaxWidth().height(278.dp).clip(RoundedCornerShape(12.dp))
     .background(c.panel),contentAlignment=Alignment.Center){
     HunterEvolutionImage(key,gender,"e",chosenHunterArtwork(key,gender),
      Modifier.fillMaxSize().padding(3.dp),ContentScale.Fit,
      description="Ilustración HD completa de "+label,alignment=Alignment.Center)
    }
    HText(label,size=15,bold=true,maxLines=1,color=if(active)c.accent else c.text)
    HText(subtitle,size=10,color=c.muted,maxLines=1)
    HunterIcon(if(active)Sigil.Check else Sigil.Plus,Modifier.size(17.dp),
     if(active)c.accent else c.muted)
   }
  }
 }
}
@Composable fun BackgroundPicker(
 selected:String,collection:String,gender:String,onSelect:(String)->Unit
){
 val c=LocalHunter.current
 HText("Desliza para explorar los escenarios HD. Cada uno es un lugar diferente.",
  size=13,color=c.muted)
 val choices=listOf("auto" to "Alternar cada día")+hunterProfileSceneChoices
 Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
  horizontalArrangement=Arrangement.spacedBy(10.dp)){
  for((key,label)in choices){
   val active=selected==key
   val shape=RoundedCornerShape(16.dp)
   Column(Modifier.width(144.dp).clip(shape)
    .background(if(active)c.selected else c.input)
    .monarchPressable(shape,active=active,role=Role.RadioButton){onSelect(key)}
    .semantics{this.selected=active;contentDescription="Ambiente "+label}
    .padding(7.dp),horizontalAlignment=Alignment.CenterHorizontally,
    verticalArrangement=Arrangement.spacedBy(7.dp)){
    Box(Modifier.fillMaxWidth().height(162.dp).clip(RoundedCornerShape(11.dp))
     .background(c.panel)){
     AssetImage(chosenSceneArtwork(key),Modifier.matchParentSize(),
      ContentScale.Crop,description="Ambiente "+label)
     if(active)Box(Modifier.align(Alignment.TopEnd).padding(5.dp)
      .background(c.accent,RoundedCornerShape(9.dp)).padding(4.dp)){
      HText("✓",size=12,bold=true,color=c.bg)
     }
    }
    HText(label,size=12,bold=active,maxLines=2)
   }
  }
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
 Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Brush.verticalGradient(listOf(c.input,c.panel))).monarchOutline(RoundedCornerShape(26.dp))){
  ConstellationField(Modifier.matchParentSize());content()
 }
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
