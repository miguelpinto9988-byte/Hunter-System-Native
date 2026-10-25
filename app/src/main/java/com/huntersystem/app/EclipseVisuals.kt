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
import org.json.JSONObject
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
val LocalVisualPreferences=staticCompositionLocalOf{JSONObject()}
fun visualMode(prefs:JSONObject)=prefs.s("mode","depth").takeIf{it in listOf("flat","depth","relic")}?:"depth"
@Composable fun ambientPhase(speed:Float=1f):State<Float> {
 if(!LocalMotionEnabled.current)return rememberUpdatedState(.18f)
 val animation=rememberInfiniteTransition(label="eclipse-ambient")
 return animation.animateFloat(0f,1f,infiniteRepeatable(tween((14000/speed.coerceIn(.5f,1.5f)).toInt(),easing=LinearEasing)),label="orbit")
}

enum class Sigil { Home, Mission, Routine, Exercise, Profile, Star, Arrow, Back, Close, Play, Pause, Reset, Mirror, Shield, Bell, Settings, Flame, Chart, Target, Book, Check, Plus, Clock, Crown, Layers, Search, Spark, Download, Expand }
@Composable fun HunterIcon(icon:Sigil,modifier:Modifier=Modifier,color:Color=LocalHunter.current.accent,description:String?=null,filled:Boolean=false){
 Canvas(modifier.size(24.dp).then(if(description!=null)Modifier.semantics{contentDescription=description} else Modifier)){
  val unit=size.minDimension/24f
  scale(unit,unit,pivot=Offset.Zero){
   val stroke=Stroke(1.65f,cap=StrokeCap.Round,join=StrokeJoin.Round)
   fun line(x:Float,y:Float,x2:Float,y2:Float)=drawLine(color,Offset(x,y),Offset(x2,y2),1.65f,StrokeCap.Round)
   fun path(vararg points:Float,close:Boolean=false){val p=Path();p.moveTo(points[0],points[1]);for(i in 2 until points.size step 2)p.lineTo(points[i],points[i+1]);if(close)p.close();drawPath(p,color,style=if(filled&&close)Fill else stroke)}
   fun circle(x:Float,y:Float,r:Float)=drawCircle(color,r,Offset(x,y),style=stroke)
   when(icon){
    Sigil.Home->{path(3f,10f,12f,3f,21f,10f);path(5f,9f,5f,21f,10f,21f,10f,15f,14f,15f,14f,21f,19f,21f,19f,9f)}
    Sigil.Mission->{path(7f,4f,4f,4f,4f,21f,20f,21f,20f,4f,17f,4f);path(8f,2f,16f,2f,16f,6f,8f,6f,close=true);path(8f,13f,11f,16f,17f,10f)}
    Sigil.Routine->{path(12f,2f,22f,8f,12f,14f,2f,8f,close=true);path(3f,13f,12f,18f,21f,13f);path(3f,17f,12f,22f,21f,17f)}
    Sigil.Exercise->{line(7f,12f,17f,12f);path(4f,7f,8f,7f,8f,17f,4f,17f,close=true);path(16f,7f,20f,7f,20f,17f,16f,17f,close=true);line(2f,10f,2f,14f);line(22f,10f,22f,14f)}
    Sigil.Profile->{circle(12f,7f,4f);val p=Path();p.moveTo(4f,21f);p.cubicTo(4f,11f,20f,11f,20f,21f);drawPath(p,color,style=stroke)}
    Sigil.Star, Sigil.Spark->{path(12f,2f,15f,9f,22f,12f,15f,15f,12f,22f,9f,15f,2f,12f,9f,9f,close=true)}
    Sigil.Arrow->{line(4f,12f,20f,12f);path(14f,6f,20f,12f,14f,18f)}
    Sigil.Back->{line(20f,12f,4f,12f);path(10f,6f,4f,12f,10f,18f)}
    Sigil.Close->{line(6f,6f,18f,18f);line(6f,18f,18f,6f)}
    Sigil.Play->path(7f,3f,21f,12f,7f,21f,close=true)
    Sigil.Pause->{path(5f,4f,9f,4f,9f,20f,5f,20f,close=true);path(15f,4f,19f,4f,19f,20f,15f,20f,close=true)}
    Sigil.Reset->{drawArc(color,-75f,290f,false,Offset(4f,4f),Size(16f,16f),style=stroke);path(12f,1f,12f,7f,17f,4f)}
    Sigil.Mirror->{line(12f,3f,12f,21f);path(8f,6f,3f,12f,8f,18f,close=true);path(16f,6f,21f,12f,16f,18f,close=true)}
    Sigil.Shield->{path(12f,2f,21f,6f,19f,16f,12f,22f,5f,16f,3f,6f,close=true);path(8f,12f,11f,15f,16f,9f)}
    Sigil.Bell->{path(4f,18f,6f,14f,6f,9f,8f,5f,12f,3f,16f,5f,18f,9f,18f,14f,20f,18f,close=true);drawArc(color,0f,180f,false,Offset(10f,19f),Size(4f,3f),style=stroke)}
    Sigil.Settings->{circle(12f,12f,4f);circle(12f,12f,9f);for(i in 0..7){val a=i*PI/4;line((12+8*cos(a)).toFloat(),(12+8*sin(a)).toFloat(),(12+11*cos(a)).toFloat(),(12+11*sin(a)).toFloat())}}
    Sigil.Flame->{path(12f,2f,11f,9f,16f,7f,20f,14f,18f,20f,12f,22f,6f,20f,4f,14f,8f,7f,8f,13f,12f,2f,close=true)}
    Sigil.Chart->{line(4f,20f,21f,20f);line(6f,15f,6f,20f);line(12f,10f,12f,20f);line(18f,5f,18f,20f);path(3f,11f,10f,6f,14f,8f,21f,2f)}
    Sigil.Target->{circle(12f,12f,9f);circle(12f,12f,5f);drawCircle(color,1.8f,Offset(12f,12f))}
    Sigil.Book->{path(12f,6f,8f,3f,2f,3f,2f,19f,8f,19f,12f,22f,16f,19f,22f,19f,22f,3f,16f,3f,12f,6f,12f,22f);line(5f,8f,8f,8f);line(16f,8f,19f,8f)}
    Sigil.Check->path(4f,12f,10f,18f,21f,5f)
    Sigil.Plus->{line(4f,12f,20f,12f);line(12f,4f,12f,20f)}
    Sigil.Clock->{circle(12f,12f,9f);path(12f,6f,12f,12f,16f,15f)}
    Sigil.Crown->{path(3f,6f,8f,11f,12f,3f,16f,11f,21f,6f,19f,19f,5f,19f,close=true);line(6f,22f,18f,22f)}
    Sigil.Layers->{path(12f,2f,22f,7f,12f,12f,2f,7f,close=true);path(2f,12f,12f,17f,22f,12f);path(2f,17f,12f,22f,22f,17f)}
    Sigil.Search->{circle(10f,10f,7f);line(15f,15f,22f,22f)}
    Sigil.Download->{path(12f,2f,12f,16f);path(6f,10f,12f,16f,18f,10f);path(3f,17f,3f,22f,21f,22f,21f,17f)}
    Sigil.Expand->{path(3f,9f,3f,3f,9f,3f);path(15f,3f,21f,3f,21f,9f);path(21f,15f,21f,21f,15f,21f);path(9f,21f,3f,21f,3f,15f)}
   }
  }
 }
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
 val c=LocalHunter.current
 Canvas(modifier){
  val points=listOf(.12f to .20f,.26f to .11f,.39f to .25f,.67f to .12f,.83f to .31f,.69f to .48f,.90f to .68f,.54f to .80f,.30f to .66f)
  points.zipWithNext().forEach{(a,b)->drawLine(c.accent.copy(alpha=.10f),Offset(a.first*size.width,a.second*size.height),Offset(b.first*size.width,b.second*size.height),1f)}
  points.forEachIndexed{i,p->drawCircle(c.secondary.copy(alpha=if(i%3==0).7f else .3f),if(i%3==0)2.dp.toPx() else 1.dp.toPx(),Offset(p.first*size.width,p.second*size.height))}
 }
}
