package com.huntersystem.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import kotlin.math.*

/** Palettes are local assets: changing appearance never changes the exercise plan. */
data class EclipsePalette(val id:String,val name:String,val story:String,val background:Long,val panel:Long,val accent:Long,val secondary:Long)
val eclipsePalettes=listOf(
 EclipsePalette("eclipse","Eclipse","El despertar de las sombras",0xff090b16,0xff121626,0xffb6a2ff,0xff77d9ef),
 EclipsePalette("constellation","Constelación","Escribe tu siguiente capítulo",0xff0a1220,0xff142236,0xff8dccff,0xffd5c1ff),
 EclipsePalette("ice","Glaciar","Calma, enfoque y precisión",0xff09151e,0xff102632,0xff7ee7ee,0xffb4cfff),
 EclipsePalette("aurora","Carmesí","La voluntad del caballero",0xff190e18,0xff2a1726,0xffffa6c2,0xffe1b3ff),
 EclipsePalette("ceniza","Jade","Equilibrio en cada movimiento",0xff0a1716,0xff132a27,0xff89e2be,0xffa1dbe7),
 EclipsePalette("amanecer","Ámbar","Una nueva era comienza",0xff18130e,0xff292017,0xfff5d295,0xffedae9c)
)
fun eclipsePalette(id:String)=eclipsePalettes.firstOrNull{it.id==id}?:eclipsePalettes.first()
fun eclipseColors(id:String):HunterColors {
 val p=eclipsePalette(id)
 fun hex(value:Long)="#%06x".format(value and 0xffffff)
 return HunterColors(obj("--bg" to hex(p.background),"--panel" to hex(p.panel),"--surface" to hex(p.panel+0x040404),
  "--input" to hex(p.background+0x050609),"--line" to "#384255","--text" to "#f3f3fa","--muted" to "#b5bed1",
  "--accent" to hex(p.accent),"--secondary33" to hex(p.secondary),"--selected33" to hex(p.panel+0x10101c),
  "--metal26" to "#59617a","--ornament26" to hex(p.secondary)))
}
val LocalMotionEnabled=staticCompositionLocalOf{false}
val LocalVisualPreferences=staticCompositionLocalOf{org.json.JSONObject()}
fun visualMode(prefs:org.json.JSONObject)=prefs.s("mode","depth").takeIf{it in listOf("flat","depth","relic")} ?:"depth"
val LocalVisualTier=staticCompositionLocalOf{VisualTier.BALANCED}
@Composable fun ambientPhase(speed:Float=1f):State<Float> {
 if(!LocalMotionEnabled.current)return rememberUpdatedState(.18f)
 val animation=rememberInfiniteTransition(label="eclipse-ambient")
 return animation.animateFloat(0f,1f,infiniteRepeatable(tween((14000/speed.coerceIn(.5f,1.5f)).toInt(),easing=LinearEasing)),label="orbit")
}

enum class Sigil { Home, Mission, Routine, Exercise, Profile, Star, Arrow, Back, Close, Play, Pause, Reset, Mirror, Shield, Bell, Settings, Flame, Chart, Target, Book, Check, Plus, Clock, Crown, Layers, Search, Spark, Download, Expand }
@Suppress("UNUSED_PARAMETER")
@Composable
fun HunterIcon(icon:Sigil,modifier:Modifier=Modifier,color:Color=LocalHunter.current.accent,description:String?=null,filled:Boolean=false){
 val pixelKey=when(icon){
  Sigil.Home->"panel";Sigil.Mission->"missions";Sigil.Routine->"routines"
  Sigil.Exercise->"exercises";Sigil.Profile->"profile"
  Sigil.Star, Sigil.Spark->"milestone";Sigil.Arrow->"next"
  Sigil.Back->"back";Sigil.Close->"close";Sigil.Play->"play"
  Sigil.Pause->"pause";Sigil.Reset->"retry";Sigil.Mirror->"back_arrow"
  Sigil.Shield->"shield";Sigil.Bell->"notifications";Sigil.Settings->"settings"
  Sigil.Flame->"streak";Sigil.Chart->"progress";Sigil.Target->"goals"
  Sigil.Book->"book";Sigil.Check->"complete";Sigil.Plus->"add"
  Sigil.Clock->"timer";Sigil.Crown->"monarch"
  Sigil.Layers->"library";Sigil.Search->"search"
  Sigil.Download->"import";Sigil.Expand->"portal"
 }
 HunterPngIcon(pixelKey,modifier.size(24.dp),description)
}

/** A small 3D mesh with perspective projection, face sorting and directional lighting. */
internal data class RelicPoint(val x:Float,val y:Float,val z:Float)
internal fun rotateRelic(p:RelicPoint,yaw:Float,pitch:Float):RelicPoint {
 val x=p.x*cos(yaw)+p.z*sin(yaw);val z=-p.x*sin(yaw)+p.z*cos(yaw)
 return RelicPoint(x,p.y*cos(pitch)-z*sin(pitch),p.y*sin(pitch)+z*cos(pitch))
}
@Composable fun RelicScene(modifier:Modifier=Modifier,interactive:Boolean=false){
 val c=LocalHunter.current;val phase=ambientPhase();var yaw by remember{mutableFloatStateOf(0f)};var pitch by remember{mutableFloatStateOf(.18f)}
 val vertices=remember{listOf(RelicPoint(0f,-1.35f,0f),RelicPoint(-.65f,0f,-.65f),RelicPoint(.65f,0f,-.65f),RelicPoint(.65f,0f,.65f),RelicPoint(-.65f,0f,.65f),RelicPoint(0f,1.35f,0f))}
 val faces=remember{listOf(listOf(0,1,2),listOf(0,2,3),listOf(0,3,4),listOf(0,4,1),listOf(5,2,1),listOf(5,3,2),listOf(5,4,3),listOf(5,1,4))}
 Canvas(modifier.semantics{contentDescription=if(interactive)"Reliquia tridimensional. Arrastra para girar." else "Reliquia de rango tridimensional"}.then(if(interactive)Modifier.pointerInput(Unit){detectDragGestures{change,amount->change.consume();yaw+=amount.x*.012f;pitch=(pitch+amount.y*.008f).coerceIn(-.8f,.8f)}}else Modifier)){
  val radius=size.minDimension*.31f;val center=Offset(size.width/2,size.height/2)
  drawCircle(Brush.radialGradient(listOf(c.accent.copy(alpha=.16f),Color.Transparent),center,radius*1.6f),radius*1.6f,center)
  drawOval(c.accent.copy(alpha=.2f),Offset(center.x-radius,center.y+radius*1.2f),Size(radius*2,radius*.25f))
  val points=vertices.map{rotateRelic(it,yaw+phase.value*2*PI.toFloat(),pitch)}
  val projected=points.map{val perspective=3.8f/(3.8f+it.z);Offset(center.x+it.x*radius*perspective,center.y+it.y*radius*perspective)}
  for(face in faces.sortedByDescending{it.sumOf{i->points[i].z.toDouble()}}){
   val a=points[face[0]];val b=points[face[1]];val d=points[face[2]]
   val nx=(b.y-a.y)*(d.z-a.z)-(b.z-a.z)*(d.y-a.y);val ny=(b.z-a.z)*(d.x-a.x)-(b.x-a.x)*(d.z-a.z);val nz=(b.x-a.x)*(d.y-a.y)-(b.y-a.y)*(d.x-a.x)
   val length=sqrt(nx*nx+ny*ny+nz*nz).coerceAtLeast(.01f)
   val light=((nx*.25f-ny*.6f+nz*.7f)/length*.5f+.5f).coerceIn(.1f,1f)
   val p=Path().apply{moveTo(projected[face[0]].x,projected[face[0]].y);face.drop(1).forEach{lineTo(projected[it].x,projected[it].y)};close()}
   drawPath(p,lerp(c.panel,c.accent,.16f+light*.7f));drawPath(p,c.secondary.copy(alpha=.55f),style=Stroke(1.dp.toPx()))
  }
  drawCircle(c.accent.copy(alpha=.3f),radius*1.55f,center,style=Stroke(.7.dp.toPx()))
 }
}

@Composable fun ConstellationField(modifier:Modifier=Modifier){
 val c=LocalHunter.current;val tier=LocalVisualTier.current
 val phase=ambientPhase(if(tier==VisualTier.MAX)1f else .65f).value
 Canvas(modifier){
  for(i in 0 until tier.stars){
   val x=((i*37+12)%97)/100f;val y=((i*61+20)%91)/100f
   val move=if(tier==VisualTier.MAX)sin((phase*2*PI+i*.7).toFloat())*.014f else 0f
   val center=Offset((x+move)*size.width,(y-move*.4f)*size.height)
   val large=i%5==0
   val alpha=if(tier==VisualTier.MAX).65f else if(tier==VisualTier.BALANCED).42f else .2f
   drawCircle(c.secondary.copy(alpha=alpha),if(large)2.dp.toPx() else 1.dp.toPx(),center)
   if(tier==VisualTier.MAX&&large)drawCircle(c.accent.copy(alpha=.1f),6.dp.toPx(),center)
   if(tier!=VisualTier.SAVER&&i%4==0&&i+1<tier.stars){
    val nx=((i*37+49)%97)/100f;val ny=((i*61+81)%91)/100f
    drawLine(c.accent.copy(alpha=.075f),center,Offset(nx*size.width,ny*size.height),1f)
   }
  }
 }
}
