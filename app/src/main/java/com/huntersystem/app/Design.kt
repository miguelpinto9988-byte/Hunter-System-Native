package com.huntersystem.app

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import org.json.JSONObject
import kotlin.math.*

val Barlow=FontFamily(Font(R.font.barlow),Font(R.font.barlow_semibold,FontWeight.SemiBold))
fun parseColor(value:String):Color{if(value.startsWith("#"))return Color(android.graphics.Color.parseColor(value));val n=Regex("[-+]?[0-9]*\\.?[0-9]+").findAll(value).map{it.value.toFloat()}.toList();if(n.size<3)return Color.White;val h=((n[0]%360)+360)%360/60;val s=n[1]/100;val l=n[2]/100;val c=(1-abs(2*l-1))*s;val x=c*(1-abs(h%2-1));val m=l-c/2;val rgb=when(h.toInt()){0->floatArrayOf(c,x,0f);1->floatArrayOf(x,c,0f);2->floatArrayOf(0f,c,x);3->floatArrayOf(0f,x,c);4->floatArrayOf(x,0f,c);else->floatArrayOf(c,0f,x)};return Color(rgb[0]+m,rgb[1]+m,rgb[2]+m,if(n.size>3)n[3] else 1f)}
data class HunterColors(val data:JSONObject){fun color(key:String)=parseColor(data.s("--$key","#ffffff"));val bg=color("bg");val panel=color("panel");val surface=color("surface");val input=color("input");val line=color("line");val text=color("text");val muted=color("muted");val accent=color("accent");val secondary=color("secondary33");val selected=color("selected33");fun hex(key:String)="#%06x".format(color(key).toArgb()and 0xffffff)}
val LocalHunter=staticCompositionLocalOf{HunterColors(JSONObject())}
/** Paletas oficiales del rediseño V39, independientes de temas históricos.
 * Se preservan los demás tokens y las funciones del catálogo existente.
 * Los colores de actividades permanecen en V39WeekPicker (no dependen del tema).
 */
private fun v39VisualTokens(name:String,source:JSONObject,accent:String):JSONObject = HunterDesignSystem.tokens(name,source,accent)

/** Valores históricos se convierten a uno de los cuatro temas V39; no alteran datos del usuario. */
fun resolveV39Theme(value:String?,gender:String=""):String=when(value?.lowercase()){
 "eclipse","umbral","abismo","runa","hielo","lavanda","aurora","ceniza","amanecer","constellation","ice"->value.lowercase()
 "auto"->if(gender=="female")"aurora" else "eclipse"
 "pink","coral","red"->"aurora"
 "amber","gold"->"amanecer"
 "violet","emerald","teal"->"ceniza"
 else->"eclipse"
}
@Composable fun HunterTheme(store:NativeStore,content: @Composable () -> Unit){
 store.revision.intValue
 val p=store.profile
 val theme=resolveV39Theme(p?.s("theme"),p?.s("gender")?:"")
 val colors=HunterColors(v39VisualTokens(theme,store.catalog.design.o("colors").o(theme),p?.s("themeAccent","default")?:"default"))
 val tier=visualTier(store)
 val motion=LocalMonarchMotion.current
 val enabled=motion.copy(reduced=motion.reduced||tier==VisualTier.SAVER)
 CompositionLocalProvider(LocalHunter provides colors,LocalVisualTier provides tier,LocalMonarchMotion provides enabled,LocalMotionEnabled provides !enabled.reduced,
  LocalVisualPreferences provides store.state.o("eclipseAppearance")){
  MaterialTheme(
   colorScheme=darkColorScheme(primary=colors.accent,onPrimary=colors.bg,surface=colors.panel,
    onSurface=colors.text,background=colors.bg,onBackground=colors.text,outline=colors.line),
   typography=Typography(bodyLarge=TextStyle(fontFamily=Barlow,fontSize=18.sp),
    labelLarge=TextStyle(fontFamily=Barlow,fontSize=17.sp))
  ){content()}
 }
}
@Composable fun HText(text:String,modifier:Modifier=Modifier,size:Int=18,color:Color=LocalHunter.current.text,bold:Boolean=false,maxLines:Int=Int.MAX_VALUE){Text(text,modifier,color=color,fontFamily=Barlow,fontSize=size.sp,lineHeight=(size*1.4).sp,fontWeight=if(bold)FontWeight.SemiBold else FontWeight.Normal,maxLines=maxLines)}
@Composable fun Heading(text:String,modifier:Modifier=Modifier,size:Int=36){Text(text,modifier,fontFamily=Barlow,fontWeight=FontWeight.Bold,fontSize=size.sp,lineHeight=(size*1.12).sp,color=LocalHunter.current.text)}
@Composable fun Eyebrow(text:String,modifier:Modifier=Modifier){Text(text,modifier,fontFamily=Barlow,fontSize=12.sp,lineHeight=16.sp,letterSpacing=1.6.sp,fontWeight=FontWeight.SemiBold,color=LocalHunter.current.accent)}
/** Tarjetas y acciones con microinteracciones sobre graphicsLayer: no provocan nuevo layout. */
@Composable fun HPanel(
 modifier:Modifier=Modifier,onClick:(()->Unit)?=null,decor:Boolean=true,
 padding:Int=24,content:@Composable ColumnScope.()->Unit
){
 val c=LocalHunter.current
 val motion=LocalMonarchMotion.current
 val interaction=remember{MutableInteractionSource()}
 val pressed by interaction.collectIsPressedAsState()
 val scale by animateFloatAsState(if(onClick!=null&&pressed&&!motion.reduced).985f else 1f,
    animationSpec=motion.spec(125),label="panel-press")
 val shape=RoundedCornerShape(HunterDesignSystem.Dimensions.cornerCard)
  Box(modifier.fillMaxWidth()
    .then(if(onClick!=null)Modifier.graphicsLayer{scaleX=scale;scaleY=scale} else Modifier)
    .clip(shape).background(Brush.verticalGradient(listOf(c.surface.copy(alpha=.95f),c.panel.copy(alpha=.89f))))
    .monarchOutline(shape,pressed=pressed)
    .then(if(onClick!=null)Modifier.clickable(
        interactionSource=interaction,indication=LocalIndication.current,
        role=Role.Button,onClick=onClick) else Modifier)){
   if(decor)Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
      .background(Brush.horizontalGradient(listOf(c.accent.copy(alpha=.22f),
        c.secondary.copy(alpha=.10f),Color.Transparent))))
   Column(Modifier.fillMaxWidth().padding(padding.dp),
     verticalArrangement=Arrangement.spacedBy(HunterDesignSystem.Dimensions.spacing),
     content=content)
 }
}
@Composable fun HButton(
 text:String,modifier:Modifier=Modifier,primary:Boolean=false,
 enabled:Boolean=true,onClick:()->Unit
){
 val c=LocalHunter.current
 val motion=LocalMonarchMotion.current
 val shape=RoundedCornerShape(HunterDesignSystem.Dimensions.cornerButton)
 val interaction=remember{MutableInteractionSource()}
 val pressed by interaction.collectIsPressedAsState()
 val scale by animateFloatAsState(if(enabled&&pressed&&!motion.reduced).964f else 1f,
    animationSpec=motion.spec(115),label="button-press")
 val fill by animateColorAsState(if(primary)c.accent else c.surface,
     animationSpec=if(motion.reduced)snap() else tween(HunterDesignSystem.Dimensions.animationMs),
     label="hunter-action")
 Box(modifier.defaultMinSize(minHeight=HunterDesignSystem.Dimensions.touchTarget)
    .graphicsLayer{scaleX=scale;scaleY=scale}.clip(shape)
    .background(if(primary)Brush.horizontalGradient(listOf(fill,c.secondary))
      else Brush.verticalGradient(listOf(fill,c.input)))
    .monarchOutline(shape,active=primary,pressed=pressed,enabled=enabled)
    .clickable(enabled=enabled,interactionSource=interaction,
      indication=LocalIndication.current,role=Role.Button,onClick=onClick)
    .padding(horizontal=16.dp,vertical=11.dp),contentAlignment=Alignment.Center){
  HText(text,color=if(!enabled)c.muted else if(primary)c.bg else c.text,size=16,bold=true)
 }
}
@Composable fun HChip(
 text:String,selected:Boolean=false,modifier:Modifier=Modifier,onClick:()->Unit
){
 val c=LocalHunter.current
 val motion=LocalMonarchMotion.current
 val shape=RoundedCornerShape(14.dp)
 val interaction=remember{MutableInteractionSource()}
 val pressed by interaction.collectIsPressedAsState()
 val scale by animateFloatAsState(if(pressed&&!motion.reduced).965f else 1f,
    animationSpec=motion.spec(115),label="chip-press")
 val fill by animateColorAsState(if(selected)c.selected else c.input,
    animationSpec=if(motion.reduced)snap() else tween(HunterDesignSystem.Dimensions.animationMs),
    label="hunter-chip")
 Box(modifier.defaultMinSize(minHeight=HunterDesignSystem.Dimensions.touchTarget)
   .graphicsLayer{scaleX=scale;scaleY=scale}
   .clip(shape).background(fill)
   .monarchOutline(shape,active=selected,pressed=pressed)
   .semantics { this.selected=selected }
   .clickable(interactionSource=interaction,indication=LocalIndication.current,
      role=Role.Button,onClick=onClick)
   .padding(horizontal=13.dp,vertical=11.dp),contentAlignment=Alignment.Center){
   HText(text,size=15,color=if(selected)c.accent else c.text,bold=selected)
 }
}
@Composable
fun HInput(
 value:String,onChange:(String)->Unit,label:String="",modifier:Modifier=Modifier,
 number:Boolean=false,multiline:Boolean=false
){
 val c=LocalHunter.current
 val shape=RoundedCornerShape(HunterDesignSystem.Dimensions.cornerField)
 val interaction=remember{MutableInteractionSource()}
 val focused by interaction.collectIsFocusedAsState()
 Column(modifier,verticalArrangement=Arrangement.spacedBy(5.dp)){
  if(label.isNotEmpty())HText(label,size=16,color=c.muted)
  BasicTextField(
   value=value,onValueChange=onChange,
   modifier=Modifier.fillMaxWidth()
    .defaultMinSize(minHeight=HunterDesignSystem.Dimensions.touchTarget)
    .clip(shape).background(c.input).monarchOutline(shape,focused=focused)
    .padding(12.dp),
   textStyle=TextStyle(fontFamily=Barlow,fontSize=18.sp,color=c.text,lineHeight=25.sp),
   singleLine=!multiline,
   keyboardOptions=KeyboardOptions(
    keyboardType=if(number)KeyboardType.Decimal else KeyboardType.Text),
   cursorBrush=SolidColor(c.accent),
   interactionSource=interaction
  )
 }
}
@Composable fun HSelect(label:String,value:String,options:List<Pair<String,String>>,modifier:Modifier=Modifier,onChange:(String)->Unit){var expanded by remember{mutableStateOf(false)};Column(modifier,verticalArrangement=Arrangement.spacedBy(5.dp)){if(label.isNotEmpty())HText(label,size=16,color=LocalHunter.current.muted);Box{HButton((options.firstOrNull{it.first==value}?.second?:value)+"  ▾",Modifier.fillMaxWidth()){expanded=true};DropdownMenu(expanded,{expanded=false}){for((id,name)in options)DropdownMenuItem(text={HText(name,size=17)},onClick={onChange(id);expanded=false})}}}}
@Composable fun HCheck(label:String,checked:Boolean,modifier:Modifier=Modifier,enabled:Boolean=true,onChange:()->Unit){val c=LocalHunter.current;Row(modifier.fillMaxWidth().clickable(enabled=enabled,onClick=onChange).padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Box(Modifier.size(23.dp).clip(RoundedCornerShape(4.dp)).background(if(checked)c.accent else c.input).monarchOutline(RoundedCornerShape(4.dp),active=checked,enabled=enabled),contentAlignment=Alignment.Center){if(checked)HText("✓",size=18,color=c.bg,bold=true)};HText(label,Modifier.weight(1f),size=17,color=if(enabled)c.text else c.muted)}}
@Composable fun HProgress(fraction:Float,modifier:Modifier=Modifier){
 val c=LocalHunter.current
 val motion=LocalMonarchMotion.current
 val target=fraction.coerceIn(0f,1f)
 val animated by animateFloatAsState(target,animationSpec=motion.spec(330),
     label="hunter-progress")
 Box(modifier.fillMaxWidth().height(9.dp)
    .semantics{progressBarRangeInfo=ProgressBarRangeInfo(target,0f..1f)}
    .clip(RoundedCornerShape(6.dp)).background(c.input)){
  Box(Modifier.fillMaxWidth(animated).fillMaxHeight()
     .background(Brush.horizontalGradient(listOf(c.accent,c.secondary))))
 }
}
@Composable fun HNotice(text:String){HPanel(decor=false,padding=15){HText(text,size=16,color=LocalHunter.current.muted)}}
@Composable fun Chips(options:List<Pair<String,String>>,value:String,onChange:(String)->Unit){Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){for((key,label)in options)HChip(label,value==key){onChange(key)}}}
private val adaptiveBitmapCache=object:android.util.LruCache<String,ImageBitmap>(48*1024){
 override fun sizeOf(key:String,value:ImageBitmap)=value.asAndroidBitmap().allocationByteCount/1024
}
@Composable fun AssetImage(path:String,modifier:Modifier=Modifier,scale:ContentScale=ContentScale.Fit,description:String?=null,alignment:Alignment=Alignment.Center){
 val ctx=LocalContext.current;val tier=LocalVisualTier.current;val key=path+"|"+tier.id
 val bitmap by produceState<ImageBitmap?>(adaptiveBitmapCache.get(key),key) {
  value=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){
   adaptiveBitmapCache.get(key)?:runCatching{
    val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
    ctx.assets.open(path).use{BitmapFactory.decodeStream(it,null,bounds)}
    var sample=1
    while(maxOf(bounds.outWidth,bounds.outHeight)/sample>tier.imageEdge)sample*=2
    ctx.assets.open(path).use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=sample})}
     ?.asImageBitmap()?.also{adaptiveBitmapCache.put(key,it)}
   }.getOrNull()
  }
 }
 if(bitmap!=null)Image(bitmap!!,description,modifier,contentScale=scale,alignment=alignment)
}
@Composable fun Avatar(data:String,name:String,modifier:Modifier=Modifier){val image=remember(data){if(data.isBlank())null else runCatching{val b=Base64.decode(data.substringAfter(','),Base64.DEFAULT);BitmapFactory.decodeByteArray(b,0,b.size)?.asImageBitmap()}.getOrNull()};Box(modifier.clip(CircleShape).background(LocalHunter.current.selected),contentAlignment=Alignment.Center){if(image!=null)Image(image,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop) else HText(name.take(1).uppercase().ifBlank{"C"},size=23,bold=true)}}
fun companionKey(p:JSONObject?)=mapOf("knight" to "igris","warrior" to "iron","bear" to "tank","insect" to "beru","scientist" to "dokja")[p?.s("companionCharacter")]?:"igris"
fun companionName(p:JSONObject?)=mapOf("igris" to "IGRIS","iron" to "IRON","tank" to "TANK","beru" to "BERU","dokja" to "DOKJA KIM")[companionKey(p)]?:"IGRIS"
