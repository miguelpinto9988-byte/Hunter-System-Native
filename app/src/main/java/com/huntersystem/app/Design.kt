package com.huntersystem.app

import android.graphics.BitmapFactory
import android.util.Base64
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import org.json.JSONObject
import kotlin.math.*

val Barlow=FontFamily(Font(R.font.barlow),Font(R.font.barlow_semibold,FontWeight.SemiBold))
val Pirata=FontFamily(Font(R.font.pirata))
fun parseColor(value:String):Color{if(value.startsWith("#"))return Color(android.graphics.Color.parseColor(value));val n=Regex("[-+]?[0-9]*\\.?[0-9]+").findAll(value).map{it.value.toFloat()}.toList();if(n.size<3)return Color.White;val h=((n[0]%360)+360)%360/60;val s=n[1]/100;val l=n[2]/100;val c=(1-abs(2*l-1))*s;val x=c*(1-abs(h%2-1));val m=l-c/2;val rgb=when(h.toInt()){0->floatArrayOf(c,x,0f);1->floatArrayOf(x,c,0f);2->floatArrayOf(0f,c,x);3->floatArrayOf(0f,x,c);4->floatArrayOf(x,0f,c);else->floatArrayOf(c,0f,x)};return Color(rgb[0]+m,rgb[1]+m,rgb[2]+m,if(n.size>3)n[3] else 1f)}
data class HunterColors(val data:JSONObject){fun color(key:String)=parseColor(data.s("--$key","#ffffff"));val bg=color("bg");val panel=color("panel");val surface=color("surface");val input=color("input");val line=color("line");val text=color("text");val muted=color("muted");val accent=color("accent");val secondary=color("secondary33");val selected=color("selected33");fun hex(key:String)="#%06x".format(color(key).toArgb()and 0xffffff)}
val LocalHunter=staticCompositionLocalOf{HunterColors(JSONObject())}
@Composable fun HunterTheme(store:NativeStore,content: @Composable () -> Unit){store.revision.intValue;val p=store.profile;val theme=p?.s("theme","auto").let{if(it==null||it=="auto")if(p?.s("gender")=="female")"pink" else "blue" else it};val themeData=store.catalog.design.o("colors").o(theme.takeIf{store.catalog.design.o("colors").has(it)}?:"blue");val palette=store.state.o("appearance33").s("palette","bruma").takeIf{themeData.has(it)}?:"bruma";val colors=HunterColors(themeData.o(palette));CompositionLocalProvider(LocalHunter provides colors){MaterialTheme(colorScheme=darkColorScheme(primary=colors.accent,onPrimary=colors.bg,surface=colors.panel,onSurface=colors.text,background=colors.bg,onBackground=colors.text,outline=colors.line),typography=Typography(bodyLarge=TextStyle(fontFamily=Barlow,fontSize=18.sp),labelLarge=TextStyle(fontFamily=Barlow,fontSize=17.sp))){content()}}}
@Composable fun HText(text:String,modifier:Modifier=Modifier,size:Int=18,color:Color=LocalHunter.current.text,bold:Boolean=false,maxLines:Int=Int.MAX_VALUE){Text(text,modifier,color=color,fontFamily=Barlow,fontSize=size.sp,lineHeight=(size*1.4).sp,fontWeight=if(bold)FontWeight.SemiBold else FontWeight.Normal,maxLines=maxLines)}
@Composable fun Heading(text:String,modifier:Modifier=Modifier,size:Int=36){Text(text,modifier,fontFamily=Pirata,fontSize=size.sp,lineHeight=(size*1.12).sp,color=LocalHunter.current.text)}
@Composable fun Eyebrow(text:String,modifier:Modifier=Modifier){Text(text,modifier,fontFamily=Barlow,fontSize=12.sp,lineHeight=16.sp,letterSpacing=1.6.sp,fontWeight=FontWeight.SemiBold,color=LocalHunter.current.accent)}
@Composable fun HPanel(modifier:Modifier=Modifier,onClick:(()->Unit)?=null,decor:Boolean=true,padding:Int=24,content:@Composable ColumnScope.()->Unit){val c=LocalHunter.current;val shape=RoundedCornerShape(9.dp);Box(modifier.fillMaxWidth().clip(shape).background(Brush.linearGradient(listOf(c.surface,c.panel))).border(1.dp,c.line,shape).then(if(onClick!=null)Modifier.clickable(onClick=onClick) else Modifier)){
 if(decor){for((alignment,rotation)in listOf(Alignment.TopStart to 0f,Alignment.TopEnd to 90f,Alignment.BottomEnd to 180f,Alignment.BottomStart to 270f))OriginalSvg("decor/corner.svg",Modifier.align(alignment).size(50.dp).graphicsLayer{rotationZ=rotation},accent=c.hex("accent"),metal=c.hex("metal26"),ornament=c.hex("ornament26"));}
 Column(Modifier.fillMaxWidth().padding(padding.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
}}
@Composable fun HButton(text:String,modifier:Modifier=Modifier,primary:Boolean=false,enabled:Boolean=true,onClick:()->Unit){val c=LocalHunter.current;val shape=RoundedCornerShape(6.dp);Box(modifier.defaultMinSize(minHeight=46.dp).clip(shape).background(if(primary)c.accent else c.input).border(1.dp,if(primary)c.accent else c.line,shape).clickable(enabled=enabled,onClick=onClick).padding(horizontal=15.dp,vertical=10.dp),contentAlignment=Alignment.Center){HText(text,color=if(primary)c.bg else if(enabled)c.text else c.muted,size=17,bold=true)}}
@Composable fun HChip(text:String,selected:Boolean=false,modifier:Modifier=Modifier,onClick:()->Unit){val c=LocalHunter.current;Box(modifier.clip(RoundedCornerShape(7.dp)).background(if(selected)c.selected else c.input).border(1.dp,if(selected)c.accent else c.line,RoundedCornerShape(7.dp)).clickable(onClick=onClick).padding(horizontal=13.dp,vertical=9.dp)){HText(text,size=16,color=if(selected)c.accent else c.muted)}}
@Composable fun HInput(value:String,onChange:(String)->Unit,label:String="",modifier:Modifier=Modifier,number:Boolean=false,multiline:Boolean=false){val c=LocalHunter.current;Column(modifier,verticalArrangement=Arrangement.spacedBy(5.dp)){if(label.isNotEmpty())HText(label,size=16,color=c.muted);BasicTextField(value,onChange,Modifier.fillMaxWidth().defaultMinSize(minHeight=46.dp).clip(RoundedCornerShape(6.dp)).background(c.input).border(1.dp,c.line,RoundedCornerShape(6.dp)).padding(12.dp),textStyle=TextStyle(fontFamily=Barlow,fontSize=18.sp,color=c.text,lineHeight=25.sp),singleLine=!multiline,keyboardOptions=KeyboardOptions(keyboardType=if(number)KeyboardType.Decimal else KeyboardType.Text),cursorBrush=SolidColor(c.accent))}}
@Composable fun HSelect(label:String,value:String,options:List<Pair<String,String>>,modifier:Modifier=Modifier,onChange:(String)->Unit){var expanded by remember{mutableStateOf(false)};Column(modifier,verticalArrangement=Arrangement.spacedBy(5.dp)){if(label.isNotEmpty())HText(label,size=16,color=LocalHunter.current.muted);Box{HButton((options.firstOrNull{it.first==value}?.second?:value)+"  ▾",Modifier.fillMaxWidth()){expanded=true};DropdownMenu(expanded,{expanded=false}){for((id,name)in options)DropdownMenuItem(text={HText(name,size=17)},onClick={onChange(id);expanded=false})}}}}
@Composable fun HCheck(label:String,checked:Boolean,modifier:Modifier=Modifier,enabled:Boolean=true,onChange:()->Unit){val c=LocalHunter.current;Row(modifier.fillMaxWidth().clickable(enabled=enabled,onClick=onChange).padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Box(Modifier.size(23.dp).clip(RoundedCornerShape(4.dp)).background(if(checked)c.accent else c.input).border(1.dp,c.line,RoundedCornerShape(4.dp)),contentAlignment=Alignment.Center){if(checked)HText("✓",size=18,color=c.bg,bold=true)};HText(label,Modifier.weight(1f),size=17,color=if(enabled)c.text else c.muted)}}
@Composable fun HProgress(fraction:Float,modifier:Modifier=Modifier){val c=LocalHunter.current;Box(modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(5.dp)).background(c.input)){Box(Modifier.fillMaxWidth(fraction.coerceIn(0f,1f)).fillMaxHeight().background(c.accent))}}
@Composable fun HNotice(text:String){HPanel(decor=false,padding=15){HText(text,size=16,color=LocalHunter.current.muted)}}
@Composable fun Chips(options:List<Pair<String,String>>,value:String,onChange:(String)->Unit){Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){for((key,label)in options)HChip(label,value==key){onChange(key)}}}
@Composable fun AssetImage(path:String,modifier:Modifier=Modifier,scale:ContentScale=ContentScale.Fit,description:String?=null,alignment:Alignment=Alignment.Center){val context=LocalContext.current;val bitmap=remember(path){runCatching{context.assets.open(path).use{BitmapFactory.decodeStream(it)}?.asImageBitmap()}.getOrNull()};if(bitmap!=null)Image(bitmap,description,modifier,contentScale=scale,alignment=alignment)}
@Composable fun Avatar(data:String,name:String,modifier:Modifier=Modifier){val image=remember(data){if(data.isBlank())null else runCatching{val b=Base64.decode(data.substringAfter(','),Base64.DEFAULT);BitmapFactory.decodeByteArray(b,0,b.size)?.asImageBitmap()}.getOrNull()};Box(modifier.clip(CircleShape).background(LocalHunter.current.selected),contentAlignment=Alignment.Center){if(image!=null)Image(image,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop) else HText(name.take(1).uppercase().ifBlank{"C"},size=23,bold=true)}}
fun companionKey(p:JSONObject?)=mapOf("knight" to "igris","warrior" to "iron","bear" to "tank","insect" to "beru","scientist" to "dokja")[p?.s("companionCharacter")]?:"igris"
fun companionName(p:JSONObject?)=mapOf("igris" to "IGRIS","iron" to "IRON","tank" to "TANK","beru" to "BERU","dokja" to "DOKJA KIM")[companionKey(p)]?:"IGRIS"
