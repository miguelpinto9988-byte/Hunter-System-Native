package com.huntersystem.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Picture
import android.view.View
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.caverock.androidsvg.SVG
import org.w3c.dom.Element
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.xml.sax.InputSource
import java.io.StringWriter
import kotlin.math.*

private data class SvgMotion(val node:Element,val attribute:String,val values:List<String>,val times:List<Double>,val seconds:Double,val begin:Double,val discrete:Boolean)
class NativeSvgView(context:Context):View(context){
 private var motions=emptyList<SvgMotion>();private var doc:org.w3c.dom.Document?=null;private var path="";private var tint="";private var ready=false
 private val cache=object:android.util.LruCache<Int,Picture>(100){}
 private var animator:ValueAnimator?=null;private var cycle=4.0
 var playing=false;set(v){if(field!=v){field=v;updateAnimation()}}
 var speed=1f;set(v){if(field!=v){field=v.coerceIn(.25f,2f);updateAnimation()}}
 var mirrored=false;set(v){field=v;invalidate()}
 var crop=false;set(v){field=v;invalidate()}
 var fraction=0f;set(v){field=v.coerceIn(0f,1f);invalidate()}
 fun load(asset:String,accent:String="#aacce0",metal:String="#494c68",ornament:String="#8b8fb1"){
  val colors="$accent|$metal|$ornament";if(path==asset&&tint==colors)return
  path=asset;tint=colors;animator?.cancel();cache.evictAll();motions=emptyList();doc=null
  if(asset.isBlank())return
  val source=context.assets.open(asset).bufferedReader().use{it.readText()}.replace("viewbox=","viewBox=").replace("preserveaspectratio=","preserveAspectRatio=").replace("currentColor",accent).replace("var(--accent)",accent).replace("var(--cyan)",accent).replace("var(--metal26)",metal).replace("var(--ornament26)",ornament)
  require(!Regex("<!\\s*(DOCTYPE|ENTITY)",RegexOption.IGNORE_CASE).containsMatchIn(source));val factory=DocumentBuilderFactory.newInstance();factory.isNamespaceAware=true;factory.isExpandEntityReferences=false;runCatching{factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true)};runCatching{factory.setFeature("http://xml.org/sax/features/external-general-entities",false)};runCatching{factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false)}
  val d=factory.newDocumentBuilder().parse(InputSource(StringReader(source)));doc=d
  val animations=d.getElementsByTagName("animate");val nodes=(0 until animations.length).mapNotNull{animations.item(it) as? Element};motions=nodes.mapNotNull{e->val target=e.parentNode as? Element?:return@mapNotNull null;val values=e.getAttribute("values").split(';').filter{it.isNotBlank()};if(values.isEmpty())return@mapNotNull null;val times=e.getAttribute("keyTimes").split(';').mapNotNull{it.toDoubleOrNull()}.takeIf{it.size==values.size}?:values.indices.map{it.toDouble()/max(1,values.lastIndex)};val seconds=e.getAttribute("dur").removeSuffix("s").toDoubleOrNull()?:4.0;SvgMotion(target,e.getAttribute("attributeName"),values,times,seconds,e.getAttribute("begin").removeSuffix("s").toDoubleOrNull()?:0.0,e.getAttribute("calcMode")=="discrete")};nodes.forEach{it.parentNode.removeChild(it)}
  val scripts=d.getElementsByTagName("script");while(scripts.length>0)scripts.item(0).parentNode.removeChild(scripts.item(0))
  cycle=motions.maxOfOrNull{it.seconds}?:4.0;ready=true;updateAnimation();invalidate()
 }
 private val numbers=Regex("[-+]?(?:[0-9]*\\.)?[0-9]+(?:[eE][-+]?[0-9]+)?")
 private fun interpolate(a:String,b:String,t:Double):String{
  if(a==b)return a
  if(a.matches(Regex("#[0-9a-fA-F]{6}"))&&b.matches(Regex("#[0-9a-fA-F]{6}"))){val av=a.drop(1).toInt(16);val bv=b.drop(1).toInt(16);val channels=listOf(16,8,0).map{s->(((av shr s)and 255)*(1-t)+((bv shr s)and 255)*t).roundToInt()};return "#"+channels.joinToString(""){it.toString(16).padStart(2,'0')}}
  val av=numbers.findAll(a).map{it.value.toDouble()}.toList();val bv=numbers.findAll(b).map{it.value.toDouble()}.toList();if(av.size!=bv.size||av.isEmpty())return if(t<.5)a else b
  var i=0;return numbers.replace(a){val n=av[i]+(bv[i]-av[i])*t;i++;String.format(java.util.Locale.US,"%.3f",n)}
 }
 private fun picture(index:Int):Picture?{
  cache.get(index)?.let{return it};val d=doc?:return null;val elapsed=index/99.0*cycle
  for(m in motions){val phase=((elapsed-m.begin)/m.seconds).let{it-floor(it)};var i=m.times.indexOfLast{it<=phase}.coerceAtLeast(0).coerceAtMost(m.values.lastIndex);val j=min(i+1,m.values.lastIndex);val denom=m.times[j]-m.times[i];val t=if(m.discrete||denom<=0)0.0 else ((phase-m.times[i])/denom).coerceIn(0.0,1.0);m.node.setAttribute(m.attribute,interpolate(m.values[i],m.values[j],t))}
  val writer=StringWriter();TransformerFactory.newInstance().newTransformer().transform(DOMSource(d),StreamResult(writer));val svg=SVG.getFromString(writer.toString());val vb=svg.documentViewBox;val w=vb?.width()?.roundToInt()?.coerceAtLeast(1)?:720;val h=vb?.height()?.roundToInt()?.coerceAtLeast(1)?:1000;val p=svg.renderToPicture(w,h);cache.put(index,p);return p
 }
 override fun onDraw(canvas:Canvas){super.onDraw(canvas);val p=picture((fraction*99).roundToInt())?:return;val scale=if(crop)max(width.toFloat()/p.width,height.toFloat()/p.height) else min(width.toFloat()/p.width,height.toFloat()/p.height);canvas.save();canvas.translate((width-p.width*scale)/2,(height-p.height*scale)/2);if(mirrored){canvas.translate(p.width*scale,0f);canvas.scale(-scale,scale)}else canvas.scale(scale,scale);canvas.drawPicture(p);canvas.restore()}
 private fun updateAnimation(){if(!ready)return;animator?.cancel();animator=null;if(!playing||!isAttachedToWindow||windowVisibility!=VISIBLE||visibility!=VISIBLE)return;animator=ValueAnimator.ofFloat(0f,1f).apply{duration=(cycle*1000/speed).toLong();repeatCount=ValueAnimator.INFINITE;interpolator=LinearInterpolator();addUpdateListener{fraction=it.animatedValue as Float};start();currentPlayTime=(fraction*duration).toLong()}}
 override fun onAttachedToWindow(){super.onAttachedToWindow();updateAnimation()}
 override fun onDetachedFromWindow(){animator?.cancel();animator=null;super.onDetachedFromWindow()}
 override fun onWindowVisibilityChanged(v:Int){super.onWindowVisibilityChanged(v);if(ready)updateAnimation()}
 fun release(){animator?.cancel();animator=null;cache.evictAll();doc=null;ready=false}
}
@Composable fun OriginalSvg(path:String,modifier:Modifier=Modifier,play:Boolean=false,speed:Float=1f,mirror:Boolean=false,progress:Float?=null,accent:String="#aacce0",metal:String="#494c68",ornament:String="#8b8fb1",crop:Boolean=false){
 AndroidView(factory={NativeSvgView(it)},modifier=modifier,update={v->v.load(path,accent,metal,ornament);v.speed=speed;v.mirrored=mirror;v.crop=crop;if(progress!=null&&!play)v.fraction=progress;v.playing=play},onRelease={it.release()})
}
