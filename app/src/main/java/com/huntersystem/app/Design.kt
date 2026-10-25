package com.huntersystem.app

import android.graphics.BitmapFactory
import android.util.Base64
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.*

val Barlow=FontFamily(Font(R.font.barlow),Font(R.font.barlow_semibold,FontWeight.SemiBold))
fun parseColor(value:String):Color{if(value.startsWith("#"))return Color(android.graphics.Color.parseColor(value));val n=Regex("[-+]?[0-9]*\\.?[0-9]+").findAll(value).map{it.value.toFloat()}.toList();if(n.size<3)return Color.White;val h=((n[0]%360)+360)%360/60;val s=n[1]/100;val l=n[2]/100;val c=(1-abs(2*l-1))*s;val x=c*(1-abs(h%2-1));val m=l-c/2;val rgb=when(h.toInt()){0->floatArrayOf(c,x,0f);1->floatArrayOf(x,c,0f);2->floatArrayOf(0f,c,x);3->floatArrayOf(0f,x,c);4->floatArrayOf(x,0f,c);else->floatArrayOf(c,0f,x)};return Color(rgb[0]+m,rgb[1]+m,rgb[2]+m,if(n.size>3)n[3] else 1f)}
data class HunterColors(val data:JSONObject){fun color(key:String)=parseColor(data.s("--$key","#ffffff"));val bg=color("bg");val panel=color("panel");val surface=color("surface");val input=color("input");val line=color("line");val text=color("text");val muted=color("muted");val accent=color("accent");val secondary=color("secondary33");val selected=color("selected33");fun hex(key:String)="#%06x".format(color(key).toArgb()and 0xffffff)}
val LocalHunter=staticCompositionLocalOf{HunterColors(JSONObject())}
/** Legacy theme identifiers retain their nearest palette; custom state is preserved. */
fun resolveV39Theme(value:String?,gender:String=""):String=when(value?.lowercase()){
 "eclipse","aurora","ceniza","amanecer","ice","constellation"->value.lowercase()
 "auto"->if(gender=="female")"aurora" else "eclipse"
 "pink","coral","red"->"aurora"
 "amber","gold"->"amanecer"
 "emerald","teal"->"ceniza"
 "violet","purple","reader"->"constellation"
 "hielo","glacier"->"ice"
 else->"eclipse"
}
@Composable fun HunterTheme(store:NativeStore,content:@Composable ()->Unit){
 store.revision.intValue
 val p=store.profile;val prefs=store.state.o("eclipseAppearance")
 val colors=eclipseColors(resolveV39Theme(p?.s("theme"),p?.s("gender")?:""))
 val context=LocalContext.current
 val life=LocalLifecycleOwner.current.lifecycle.currentStateAsState().value
 val systemMotion=runCatching{Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f}.getOrDefault(true)
 val motion=p?.b("animate",true)!=false&&!prefs.b("reduceMotion")&&systemMotion&&life.isAtLeast(Lifecycle.State.RESUMED)
 CompositionLocalProvider(LocalHunter provides colors,LocalMotionEnabled provides motion,LocalVisualPreferences provides prefs){
  MaterialTheme(colorScheme=darkColorScheme(primary=colors.accent,onPrimary=colors.bg,secondary=colors.secondary,onSecondary=colors.bg,
   surface=colors.panel,onSurface=colors.text,surfaceVariant=colors.surface,onSurfaceVariant=colors.muted,
   background=colors.bg,onBackground=colors.text,outline=colors.line,error=Color(0xffffb4b4)),
   shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(20.dp),large=RoundedCornerShape(28.dp)),
   typography=Typography(bodyLarge=TextStyle(fontFamily=Barlow,fontSize=17.sp,lineHeight=25.sp),
    bodyMedium=TextStyle(fontFamily=Barlow,fontSize=15.sp,lineHeight=22.sp),titleLarge=TextStyle(fontFamily=Barlow,fontWeight=FontWeight.SemiBold,fontSize=24.sp),
    labelLarge=TextStyle(fontFamily=Barlow,fontWeight=FontWeight.SemiBold,fontSize=16.sp))) {content()}
 }
}
@Composable fun HText(text:String,modifier:Modifier=Modifier,size:Int=18,color:Color=LocalHunter.current.text,bold:Boolean=false,maxLines:Int=Int.MAX_VALUE){
 Text(text,modifier,color=color,fontFamily=Barlow,fontSize=size.sp,lineHeight=(size*1.35).sp,fontWeight=if(bold)FontWeight.SemiBold else FontWeight.Normal,maxLines=maxLines,overflow=TextOverflow.Ellipsis)
}
@Composable fun Heading(text:String,modifier:Modifier=Modifier,size:Int=36){
 Text(text,modifier,fontFamily=Barlow,fontWeight=FontWeight.SemiBold,fontSize=size.sp,lineHeight=(size*1.12).sp,letterSpacing=(-.45).sp,color=LocalHunter.current.text)
}
@Composable fun Eyebrow(text:String,modifier:Modifier=Modifier){
 Text(text,modifier,fontFamily=Barlow,fontSize=11.sp,lineHeight=16.sp,letterSpacing=1.8.sp,fontWeight=FontWeight.SemiBold,color=LocalHunter.current.accent)
}
@Composable fun HPanel(modifier:Modifier=Modifier,onClick:(()->Unit)?=null,decor:Boolean=true,padding:Int=24,content:@Composable ColumnScope.()->Unit){
 val c=LocalHunter.current;val shape=RoundedCornerShape(24.dp)
 Box(modifier.fillMaxWidth().clip(shape).background(Brush.linearGradient(listOf(c.surface,c.panel)))
  .border(1.dp,Brush.linearGradient(listOf(if(decor)c.accent.copy(alpha=.28f) else c.line.copy(alpha=.65f),c.line.copy(alpha=.35f))),shape)
  .then(if(onClick!=null)Modifier.clickable(role=Role.Button,onClick=onClick) else Modifier)){
  Column(Modifier.fillMaxWidth().padding(padding.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
 }
}
fun buttonIcon(text:String):Sigil?=when {
 text.startsWith("‹")||text.startsWith("Volver")->Sigil.Back
 text=="×"->Sigil.Close
 text.startsWith("Ⅱ")->Sigil.Pause
 text.startsWith("▶")->Sigil.Play
 text.startsWith("⇄")->Sigil.Mirror
 text.startsWith("+")->Sigil.Plus
 text.startsWith("✧")||text.startsWith("✦")->Sigil.Spark
 text.startsWith("✎")->Sigil.Settings
 text.startsWith("Reiniciar")->Sigil.Reset
 text.startsWith("Guardar")||text.startsWith("Confirmar")->Sigil.Check
 text.startsWith("Ver progreso")->Sigil.Chart
 text.startsWith("Exportar")->Sigil.Download
 text.startsWith("Siguiente")->Sigil.Arrow
 else->null
}
fun buttonLabel(text:String)=text.trim().trim('‹','›','→','↗','⟶','✧','✦','✎','Ⅱ','▶','⇄','+',' ').ifBlank{if(text=="×")"Cerrar" else text}
@Composable fun HButton(text:String,modifier:Modifier=Modifier,primary:Boolean=false,enabled:Boolean=true,icon:Sigil?=buttonIcon(text),onClick:()->Unit){
 val c=LocalHunter.current;val shape=RoundedCornerShape(16.dp)
 Row(modifier.defaultMinSize(minHeight=50.dp).clip(shape).alpha(if(enabled)1f else .48f)
  .background(if(primary)Brush.horizontalGradient(listOf(c.accent,c.secondary)) else Brush.verticalGradient(listOf(c.surface,c.input)))
  .border(1.dp,if(primary)c.accent.copy(alpha=.7f) else c.line.copy(alpha=.7f),shape)
  .clickable(enabled=enabled,role=Role.Button,onClick=onClick).padding(horizontal=14.dp,vertical=12.dp),
  verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp,Alignment.CenterHorizontally)){
  if(icon!=null)HunterIcon(icon,Modifier.size(19.dp),if(primary)c.bg else c.accent)
  HText(buttonLabel(text),size=16,color=if(primary)c.bg else c.text,bold=true)
 }
}
@Composable fun HIconButton(icon:Sigil,label:String,modifier:Modifier=Modifier,onClick:()->Unit){
 val c=LocalHunter.current
 Box(modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(c.input).border(1.dp,c.line.copy(alpha=.6f),RoundedCornerShape(16.dp))
  .clickable(role=Role.Button,onClick=onClick).semantics{contentDescription=label},contentAlignment=Alignment.Center){HunterIcon(icon)}
}
@Composable fun HChip(text:String,selected:Boolean=false,modifier:Modifier=Modifier,onClick:()->Unit){
 val c=LocalHunter.current
 Box(modifier.defaultMinSize(minHeight=48.dp).clip(RoundedCornerShape(14.dp)).background(if(selected)c.selected else c.input)
  .border(1.dp,if(selected)c.accent.copy(alpha=.8f) else c.line.copy(alpha=.6f),RoundedCornerShape(14.dp))
  .selectable(selected,role=Role.Tab,onClick=onClick).padding(horizontal=13.dp,vertical=10.dp),contentAlignment=Alignment.Center){
  HText(text,size=15,color=if(selected)c.accent else c.muted,bold=selected)
 }
}
@Composable fun HInput(value:String,onChange:(String)->Unit,label:String="",modifier:Modifier=Modifier,number:Boolean=false,multiline:Boolean=false){
 val c=LocalHunter.current
 OutlinedTextField(value=value,onValueChange=onChange,modifier=modifier.fillMaxWidth(),label=if(label.isNotBlank()){{HText(label,size=14,color=c.muted,maxLines=1)}}else null,
  singleLine=!multiline,minLines=if(multiline)3 else 1,shape=RoundedCornerShape(16.dp),
  textStyle=TextStyle(fontFamily=Barlow,fontSize=17.sp,color=c.text,lineHeight=25.sp),
  keyboardOptions=KeyboardOptions(keyboardType=if(number)KeyboardType.Decimal else KeyboardType.Text),
  colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=c.accent,unfocusedBorderColor=c.line,focusedContainerColor=c.input,unfocusedContainerColor=c.input,cursorColor=c.accent))
}
@Composable fun HSelect(label:String,value:String,options:List<Pair<String,String>>,modifier:Modifier=Modifier,onChange:(String)->Unit){
 var expanded by remember{mutableStateOf(false)};Column(modifier,verticalArrangement=Arrangement.spacedBy(6.dp)){
  if(label.isNotEmpty())HText(label,size=14,color=LocalHunter.current.muted)
  Box{HButton(options.firstOrNull{it.first==value}?.second?:value,Modifier.fillMaxWidth(),icon=Sigil.Layers){expanded=true}
   DropdownMenu(expanded,{expanded=false}){for((id,name)in options)DropdownMenuItem(text={HText(name,size=16)},onClick={onChange(id);expanded=false})}
  }
 }
}
@Composable fun HCheck(label:String,checked:Boolean,modifier:Modifier=Modifier,enabled:Boolean=true,onChange:()->Unit){
 val c=LocalHunter.current
 Row(modifier.fillMaxWidth().defaultMinSize(minHeight=48.dp).clip(RoundedCornerShape(12.dp))
  .toggleable(value=checked,enabled=enabled,role=Role.Checkbox,onValueChange={onChange()}).padding(vertical=5.dp),
  verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
  Checkbox(checked=checked,onCheckedChange=null,enabled=enabled,colors=CheckboxDefaults.colors(checkedColor=c.accent,checkmarkColor=c.bg,uncheckedColor=c.muted))
  HText(label,Modifier.weight(1f),size=16,color=if(enabled)c.text else c.muted)
 }
}
@Composable fun HProgress(fraction:Float,modifier:Modifier=Modifier){
 val c=LocalHunter.current;val value by animateFloatAsState(fraction.coerceIn(0f,1f),tween(if(LocalMotionEnabled.current)350 else 0),label="progress")
 Box(modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(c.input).semantics{progressBarRangeInfo=ProgressBarRangeInfo(fraction.coerceIn(0f,1f),0f..1f)}){
  Box(Modifier.fillMaxWidth(value).fillMaxHeight().background(Brush.horizontalGradient(listOf(c.accent,c.secondary))))
 }
}
@Composable fun HNotice(text:String){
 val c=LocalHunter.current
 HPanel(decor=false,padding=16){Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){HunterIcon(Sigil.Shield,Modifier.size(20.dp),c.secondary);HText(text,Modifier.weight(1f),size=15,color=c.muted)}}
}
@Composable fun Chips(options:List<Pair<String,String>>,value:String,onChange:(String)->Unit){
 Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){for((key,label)in options)HChip(label,value==key){onChange(key)}}
}
private val assetBitmaps=object:android.util.LruCache<String,ImageBitmap>(24*1024){override fun sizeOf(key:String,value:ImageBitmap)=value.asAndroidBitmap().allocationByteCount/1024}
@Composable fun AssetImage(path:String,modifier:Modifier=Modifier,scale:ContentScale=ContentScale.Fit,description:String?=null,alignment:Alignment=Alignment.Center){
 val context=LocalContext.current
 val bitmap by produceState(assetBitmaps.get(path),path){value=withContext(Dispatchers.IO){assetBitmaps.get(path)?:runCatching{
  val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};context.assets.open(path).use{BitmapFactory.decodeStream(it,null,bounds)}
  var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>1600)sample*=2
  val options=BitmapFactory.Options().apply{inSampleSize=sample}
  context.assets.open(path).use{BitmapFactory.decodeStream(it,null,options)}?.asImageBitmap()?.also{assetBitmaps.put(path,it)}
 }.getOrNull()}}
 if(bitmap!=null)Image(bitmap!!,description,modifier,contentScale=scale,alignment=alignment) else Spacer(modifier)
}
@Composable fun Avatar(data:String,name:String,modifier:Modifier=Modifier){
 val image=remember(data){if(data.isBlank())null else runCatching{val b=Base64.decode(data.substringAfter(','),Base64.DEFAULT);BitmapFactory.decodeByteArray(b,0,b.size)?.asImageBitmap()}.getOrNull()}
 Box(modifier.clip(CircleShape).background(LocalHunter.current.selected).border(1.dp,LocalHunter.current.accent.copy(alpha=.5f),CircleShape),contentAlignment=Alignment.Center){
  if(image!=null)Image(image,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop) else HText(name.take(1).uppercase().ifBlank{"C"},size=22,bold=true)
 }
}
fun companionKey(p:JSONObject?)=mapOf("knight" to "igris","warrior" to "iron","bear" to "tank","insect" to "beru","scientist" to "dokja")[p?.s("companionCharacter")]?:"igris"
fun companionName(p:JSONObject?)=mapOf("igris" to "IGRIS","iron" to "IRON","tank" to "TANK","beru" to "BERU","dokja" to "DOKJA KIM")[companionKey(p)]?:"IGRIS"
